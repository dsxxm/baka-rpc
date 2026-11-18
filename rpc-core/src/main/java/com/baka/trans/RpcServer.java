package com.baka.trans;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

import com.baka.dto.RpcReq;
import com.baka.dto.RpcResp;
import com.baka.dto.ServiceKey;

import lombok.extern.slf4j.Slf4j;

/**
 * RpcServer
 */
@Slf4j
public class RpcServer {
	private final int port;
	private final ServiceRegistry serviceRegistry;

	public RpcServer(int port) { this.port = port; serviceRegistry = new ServiceRegistry(); }

	public <T> void register(Class<T> serviceInterface, T service, String group, String version) {
		if (!serviceInterface.isInterface()) {
			throw new IllegalArgumentException("RpcServer.register serviceInterface must be an interface");
		}
		if (!serviceInterface.isInstance(service)) {
			throw new IllegalArgumentException("service must implement serviceInterface");
		}
		serviceRegistry.register(new ServiceKey(serviceInterface.getName(), group, version), service);
	}

	public <T> void register(Class<T> serviceInterface, T service) {
		register(serviceInterface, service, ServiceKey.DEFAULT_GROUP, ServiceKey.DEFAULT_VERSION);
	}

	public void start() {
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			log.info("server start on {}:{}", serverSocket.getInetAddress(), port);
			Socket clientSocket;
			while ((clientSocket = serverSocket.accept()) != null) {
				// 读取请求
				ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
				RpcReq req = (RpcReq) in.readObject();
				log.info("received {} from client", req);

				// 调用服务
				Object service = serviceRegistry.getService(req.getServiceKey());
				if (service == null) {
					log.error("cannot find service,try register first");
					continue;
				}
				Method method = service.getClass().getMethod(req.getMethodName(), req.getParamTypes());
				Object data = method.invoke(service, req.getParams());

				// 返回响应
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
				RpcResp<?> resp = RpcResp.success(req.getReqId(), data);
				out.writeObject(resp);
				out.flush();
				log.info("send {} to client", resp);
			}
		} catch (Exception e) {
			log.error("fail to create RpcServer socket");
		}
	}
}
