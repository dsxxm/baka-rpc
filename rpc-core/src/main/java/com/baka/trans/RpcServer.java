package com.baka.trans;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.baka.dto.RpcReq;
import com.baka.dto.RpcResp;
import com.baka.dto.ServiceKey;
import com.baka.protocol.RpcMessage;
import com.baka.registry.ServiceInstance;
import com.baka.registry.ServiceRegistry;
import com.baka.web.RpcMessageDecoder;
import com.baka.web.RpcMessageEncoder;
import com.baka.web.RpcRequestHandler;
import com.baka.web.ServerHeartbeatHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * RpcServer
 */
@Slf4j
public class RpcServer {
	private final String host;
	private final int port;
	private final String instanceId;
	private final ServiceProviderRegistry serviceProviderRegistry;
	private final ServiceRegistry serviceRegistry;
	private final byte codec;
	// 为业务调用准备的线程池
	private final ThreadPoolExecutor pool;
	// 为定时心跳准备的线程池
	private final ScheduledExecutorService scheduler;

	private volatile Channel serverChannel;
	private final Set<ServiceInstance> serviceInstances = ConcurrentHashMap.newKeySet();
	private final AtomicBoolean registryClosed = new AtomicBoolean();
	private final long HEARTBEAT_INTERVAL = 10_000L;

	public RpcServer(String host, int port, ServiceRegistry serviceRegistry, byte codec) {
		this.host = host;
		this.port = port;
		this.instanceId = UUID.randomUUID().toString();
		this.serviceProviderRegistry = new ServiceProviderRegistry();
		this.serviceRegistry = serviceRegistry;
		this.codec = codec;
		int cpuNum = Runtime.getRuntime().availableProcessors();
		this.pool = new ThreadPoolExecutor(cpuNum, 2 * cpuNum, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(128), new ThreadPoolExecutor.CallerRunsPolicy());
		this.scheduler = Executors.newScheduledThreadPool(1);
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void close() {
		Channel channel = serverChannel;
		if (channel != null) {
			channel.close().syncUninterruptibly();
		}
		shutdownRegistry();
	}

	private void shutdownRegistry() {
		if (!registryClosed.compareAndSet(false, true)) {
			return;
		}

		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}

		for (ServiceInstance instance : serviceInstances) {
			try {
				serviceRegistry.unregister(instance);
			} catch (Exception e) {
				log.error("failed to unregister {}", instance, e);
			}
		}
	}

	public <T> void register(Class<T> serviceInterface, T service, String group, String version) {
		if (!serviceInterface.isInterface()) {
			log.error("RpcServer: RpcServer.register serviceInterface must be an interface");
			throw new IllegalArgumentException("RpcServer.register serviceInterface must be an interface");
		}
		if (!serviceInterface.isInstance(service)) {
			log.error("RpcServer: service must implement serviceInterface");
			throw new IllegalArgumentException("service must implement serviceInterface");
		}
		serviceProviderRegistry.registerProvider(new ServiceKey(serviceInterface.getName(), group, version), service);
	}

	public <T> void register(Class<T> serviceInterface, T service) {
		register(serviceInterface, service, ServiceKey.DEFAULT_GROUP, ServiceKey.DEFAULT_VERSION);
	}

	public void start() {
		EventLoopGroup parentGroup = new EpollEventLoopGroup(1);
		EventLoopGroup childrenGroup = new EpollEventLoopGroup(4);

		try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(parentGroup, childrenGroup)
					.channel(EpollServerSocketChannel.class)
					.childOption(ChannelOption.TCP_NODELAY, true)
					.childOption(ChannelOption.SO_KEEPALIVE, true)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel channel) {
							channel.pipeline()
									.addLast(new LengthFieldBasedFrameDecoder(
											RpcMessage.HEADER_LENGTH + RpcMessage.MAX_BODY_LENGTH,
											7,
											4,
											0,
											0
									))
									.addLast(new RpcMessageDecoder())
									.addLast(new RpcMessageEncoder(codec))
									.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS))
									.addLast(new ServerHeartbeatHandler(codec))
									.addLast(new RpcRequestHandler(RpcServer.this::invokeService, pool));
						}
					});

			serverChannel = bootstrap.bind(port).sync().channel();
			log.info("server start on {}:{}", host, port);

			for (ServiceKey serviceKey : serviceProviderRegistry.getServiceKeys()) {
				ServiceInstance instance = new ServiceInstance(serviceKey, instanceId, host, port);
				serviceInstances.add(instance);
				serviceRegistry.register(instance);
			}

			scheduler.scheduleAtFixedRate(() -> {
				if (!serverChannel.isActive()) {
					log.warn("server channel is closed; skipping heartbeat");
					return;
				}

				for (ServiceInstance instance : serviceInstances) {
					try {
						serviceRegistry.heartbeat(instance);
					} catch (Exception e) {
						log.error("heartbeat failed for {}", instance, e);
					}
				}
			}, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);

			serverChannel.closeFuture().sync();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("RPC server interrupted", e);
		} finally {
			shutdownRegistry();

			pool.shutdown();
			childrenGroup.shutdownGracefully().syncUninterruptibly();
			parentGroup.shutdownGracefully().syncUninterruptibly();
			serverChannel = null;
		}
	}

	private RpcResp<?> invokeService(RpcReq req) {
		try {
			Object service = serviceProviderRegistry.getProvider(req.getServiceKey());
			if (service == null) {
				throw new IllegalStateException("service not found: " + req.getServiceKey());
			}

			Method method = service.getClass().getMethod(req.getMethodName(), req.getParamTypes());
			Object data = method.invoke(service, req.getParams());
			return RpcResp.success(req.getReqId(), data);
		} catch (Exception e) {
			log.error("RPC invocation failed for {}", req, e);
			return RpcResp.fail(req.getReqId(), e.getMessage());
		}
	}
}
