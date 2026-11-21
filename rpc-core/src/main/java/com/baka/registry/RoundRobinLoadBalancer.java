package com.baka.registry;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import com.baka.dto.ServiceKey;

/**
 * RoundRobinLoadBalancer
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

	private final ConcurrentMap<ServiceKey, AtomicLong> counters = new ConcurrentHashMap<>();

	@Override
	public ServiceInstance balance(List<ServiceInstance> instances) {
		if (instances.isEmpty()) {
			throw new IllegalStateException("no available service instances");
		}

		ServiceKey serviceKey = instances.get(0).getServiceKey();
		AtomicLong counter = counters.computeIfAbsent(serviceKey, key -> new AtomicLong());
		int index = (int) Math.floorMod(counter.getAndIncrement(), instances.size());
		return instances.get(index);
	}
}
