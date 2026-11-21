package com.baka.trans;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.List;

import com.baka.dto.RpcReq;
import com.baka.dto.RpcResp;
import com.baka.dto.ServiceKey;
import com.baka.registry.LoadBalancer;
import com.baka.registry.ServiceInstance;
import com.baka.registry.ServiceRegistry;

import lombok.extern.slf4j.Slf4j;

/**
 * RpcClient 将请求发送给RpcServer以供其调用
 */
@Slf4j
public class RpcClient {
	private final ServiceRegistry serviceRegistry;
	private final LoadBalancer loadBalancer;

	public RpcClient(ServiceRegistry serviceRegistry, LoadBalancer loadBalancer) {
		this.serviceRegistry = serviceRegistry;
		this.loadBalancer = loadBalancer;
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
		// 先发现服务
		List<ServiceInstance> instances = serviceRegistry.discover(req.getServiceKey());
		ServiceInstance instance = loadBalancer.balance(instances);

		// 然后再发送请求接受响应
		try (Socket socket = new Socket(instance.getHost(), instance.getPort()); ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()); ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
			// 发送请求
			out.writeObject(req);
			out.flush();
			log.info("sent {} to server {}:{}", req, instance.getHost(), instance.getPort());

			// 接受服务端响应
			RpcResp<?> resp = (RpcResp<?>) in.readObject();
			log.info("received {} from server", resp);

			return resp;
		} catch (Exception e) {
			log.error("fail to connect the RpcServer");
		}

		return null;
	}

}
