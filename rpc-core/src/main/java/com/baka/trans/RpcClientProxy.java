package com.baka.trans;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

import com.baka.dto.RpcReq;
import com.baka.dto.RpcResp;
import com.baka.dto.ServiceKey;

import lombok.extern.slf4j.Slf4j;

/**
 * RpcClientProxy 通过动态代理的方式 对接口原方法进行包装
 */
@Slf4j
public class RpcClientProxy implements InvocationHandler {

	private final RpcClient rpcClient;
	private final ServiceKey serviceKey;

	public RpcClientProxy(RpcClient rpcClient, ServiceKey serviceKey) {
		this.rpcClient = rpcClient;
		this.serviceKey = serviceKey;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// 拼装请求
		RpcReq req = new RpcReq(UUID.randomUUID().toString(), serviceKey, method.getName(), args, method.getParameterTypes());
		// 发送请求
		RpcResp<?> resp = rpcClient.invoke(req);

		if (RpcResp.isSuccess(resp)) {
			return resp.getData();
		} else {
			log.error("get failed response from server");
			throw new Exception("get failed response from server");
		}
	}

}
