package com.baka.trans;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.baka.dto.ServiceKey;

import lombok.extern.slf4j.Slf4j;

/**
 * ServiceRegistry 将服务注册到内存中
 */
@Slf4j
class ServiceRegistry {

	private Map<ServiceKey, Object> map;

	ServiceRegistry() {
		map = new ConcurrentHashMap<>();
	}

	// 注册服务
	void register(ServiceKey serviceKey, Object service) {
		this.map.put(serviceKey, service);
		log.info("registered {}, instance {}", serviceKey, service);
	}

	// 找不到会返回null
	Object getService(ServiceKey key) {
		return this.map.getOrDefault(key, null);
	}
}
