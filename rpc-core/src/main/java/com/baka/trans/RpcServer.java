package com.baka.trans;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.baka.dto.RpcReq;
import com.baka.dto.RpcResp;
import com.baka.dto.ServiceKey;
import com.baka.registry.ServiceInstance;
import com.baka.registry.ServiceRegistry;

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
	// 为socket准备的线程池
	private final ThreadPoolExecutor pool;
	// 为定时心跳准备的线程池
	private final ScheduledExecutorService scheduler;

	private final long HEARTBEAT_INTERVAL = 10_000L;

	/**
	 * @param host
	 * @param port
	 * @param serviceRegistry
	 */
	public RpcServer(String host, int port, ServiceRegistry serviceRegistry) {
		this.host = host;
		this.port = port;
		this.instanceId = UUID.randomUUID().toString();
		this.serviceProviderRegistry = new ServiceProviderRegistry();
		this.serviceRegistry = serviceRegistry;
		int cpuNum = Runtime.getRuntime().availableProcessors();
		this.pool = new ThreadPoolExecutor(cpuNum, 2 * cpuNum, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(128), new ThreadPoolExecutor.CallerRunsPolicy());
		this.scheduler = Executors.newScheduledThreadPool(1);
	}

	public String getInstanceId() {
		return instanceId;
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
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			log.info("server start on {}:{}", host, port);

			// 将服务注册到redis中
			Set<ServiceKey> serviceKeys = serviceProviderRegistry.getServiceKeys();
			Set<ServiceInstance> serviceInstances = new HashSet<ServiceInstance>();
			for (ServiceKey serviceKey : serviceKeys) {
				ServiceInstance instance = new ServiceInstance(serviceKey, instanceId, host, port);
				serviceInstances.add(instance);
				serviceRegistry.register(instance);
			}

			// 定期向所有服务发送心跳请求
			scheduler.scheduleAtFixedRate(() -> {
				for (ServiceInstance instance : serviceInstances) {
					try {
						serviceRegistry.heartbeat(instance);
					} catch (Exception e) {
						log.error("heartbeat failed for {}", instance, e);
					}
				}
			}, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);

			// 接受并处理RpcClient的请求 返回响应
			while (true) {
				Socket clientSocket = serverSocket.accept();
				pool.execute(() -> handleClient(clientSocket));
			}
		} catch (IOException e) {
			log.error("RPC server stopped", e);
		}
	}

	private void handleClient(Socket socket) {
		try (Socket clientSocket = socket; ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream()); ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {
			RpcReq req = (RpcReq) in.readObject();
			log.info("received {} from client", req);

			RpcResp<?> resp = invokeService(req);
			out.writeObject(resp);
			out.flush();
			log.info("send {} to client", resp);
		} catch (IOException | ClassNotFoundException e) {
			log.error("RPC request processing failed", e);
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
