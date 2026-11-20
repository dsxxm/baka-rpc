package com.baka.trans;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.baka.dto.ServiceKey;

import lombok.extern.slf4j.Slf4j;

/**
 * ServiceProviderRegistry 维护服务标识到本地实现实例的映射。
 */
@Slf4j
class ServiceProviderRegistry {

	private final Map<ServiceKey, Object> providers;

	ServiceProviderRegistry() {
		providers = new ConcurrentHashMap<>();
	}

	void registerProvider(ServiceKey serviceKey, Object serviceProvider) {
		providers.put(serviceKey, serviceProvider);
		log.info("registered provider {} for {}", serviceProvider, serviceKey);
	}

	Object getProvider(ServiceKey serviceKey) {
		return providers.get(serviceKey);
	}
}
