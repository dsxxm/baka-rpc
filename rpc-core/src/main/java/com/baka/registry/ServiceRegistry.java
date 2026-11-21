package com.baka.registry;

import java.util.List;

import com.baka.dto.ServiceKey;

/**
 * ServiceRegistry 负责服务注册发现
 */
public interface ServiceRegistry {

	// 注册服务
	void register(ServiceInstance instance);

	// 心跳
	void heartbeat(ServiceInstance instance);

	// 注销服务
	void unregister(ServiceInstance instance);

	// 发现服务
	List<ServiceInstance> discover(ServiceKey serviceKey);
}
