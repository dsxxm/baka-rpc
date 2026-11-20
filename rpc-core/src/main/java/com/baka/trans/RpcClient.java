package com.baka.trans;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Proxy;
import java.net.Socket;

import com.baka.dto.RpcReq;
import com.baka.dto.RpcResp;
import com.baka.dto.ServiceKey;

import lombok.extern.slf4j.Slf4j;

/**
 * RpcClient 将请求发送给RpcServer以供其调用
 */
@Slf4j
public class RpcClient {
	private final String host;
	private final int port;

	public RpcClient(String host, int port) {
		this.host = host;
		this.port = port;
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

	RpcResp<?> sendReq(RpcReq req) {
		try (Socket socket = new Socket(host, port);
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
			// 发送请求
			out.writeObject(req);
			out.flush();
			log.info("sent {} to server", req);

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
