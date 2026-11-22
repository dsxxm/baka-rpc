package com.baka.trans;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.baka.dto.RpcReq;
import com.baka.dto.RpcResp;
import com.baka.dto.ServiceKey;
import com.baka.registry.LoadBalancer;
import com.baka.registry.ServiceInstance;
import com.baka.registry.ServiceRegistry;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import lombok.extern.slf4j.Slf4j;


/**
 * RpcClient 将请求发送给RpcServer以供其调用
 */
@Slf4j
public class RpcClient implements AutoCloseable {
	private final ServiceRegistry serviceRegistry;
	private final LoadBalancer loadBalancer;
	private final byte codec;
	private final EventLoopGroup eventLoopGroup;
	private final ChannelManager channelManager;

	public RpcClient(ServiceRegistry serviceRegistry, LoadBalancer loadBalancer, byte codec) {
		this.serviceRegistry = serviceRegistry;
		this.loadBalancer = loadBalancer;
		this.codec = codec;
		this.eventLoopGroup = new EpollEventLoopGroup();
		this.channelManager = new ChannelManager(eventLoopGroup, codec);
	}

	// 获取动态代理的接口实现类
	public <T> T getProxy(Class<T> service) {
		return getProxy(service, ServiceKey.DEFAULT_GROUP, ServiceKey.DEFAULT_VERSION);
	}

	// 获取动态代理的接口实现类
	public <T> T getProxy(Class<T> service, String group, String version) {
		if (!service.isInterface()) {
			log.error("getProxy error:RpcClient.getProxy service must be a interface");
			throw new IllegalArgumentException("RpcClient.getProxy service must be a interface");
		}

		ServiceKey serviceKey = new ServiceKey(service.getName(), group, version);
		Object object = Proxy.newProxyInstance(service.getClassLoader(), new Class[] { service }, new RpcClientProxy(this, serviceKey));

		return service.cast(object);
	}

	RpcResp<?> invoke(RpcReq req) {
		List<ServiceInstance> instances = serviceRegistry.discover(req.getServiceKey());
		ServiceInstance instance = loadBalancer.balance(instances);
		CompletableFuture<RpcResp<?>> responseFuture = null;

		try {
			Channel channel = channelManager.getOrConnect(instance).get();
			responseFuture = channelManager.registerRequest(req.getReqId(), channel);
			channel.writeAndFlush(req).sync();

			RpcResp<?> resp = responseFuture.get();
			log.info("received {} from server {}:{}", resp, instance.getHost(), instance.getPort());
			return resp;
		} catch (Exception e) {
			if (responseFuture != null) {
				channelManager.failRequest(req.getReqId(), responseFuture, e);
			}
			throw new IllegalStateException("failed to invoke RPC service " + instance, e);
		} finally {
			if (responseFuture != null) {
				channelManager.removeRequest(req.getReqId(), responseFuture);
			}
		}
	}

	@Override
	public void close() {
		channelManager.close();
		eventLoopGroup.shutdownGracefully().syncUninterruptibly();
	}
}
