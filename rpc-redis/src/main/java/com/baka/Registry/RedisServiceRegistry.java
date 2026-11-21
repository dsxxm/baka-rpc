package com.baka.Registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.baka.dto.ServiceKey;
import com.baka.registry.ServiceInstance;
import com.baka.registry.ServiceRegistry;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.extern.slf4j.Slf4j;

/**
 * RedisServiceRegistry
 */
@Slf4j
public final class RedisServiceRegistry implements ServiceRegistry, AutoCloseable {
	// 从注册/更新起 存活时间大于这个时长的服务被视为已停机
	private static final long LEASE_MILLIS = 30_000L;

	private final RedisClient client;
	private final StatefulRedisConnection<String, String> connection;
	private final RedisCommands<String, String> commands;

	public RedisServiceRegistry(String host, int port) {
		RedisURI uri = RedisURI.Builder.redis(host, port).build();
		client = RedisClient.create(uri);
		try {
			connection = client.connect();
			commands = connection.sync();
		} catch (Exception e) {
			client.shutdown();
			log.error("cannot connect redis", e);
			throw new IllegalStateException("cannot connect redis", e);
		}
	}

	@Override
	public void register(ServiceInstance instance) {
		String key = toRedisKey(instance.getServiceKey());
		String value = toRedisMember(instance);

		commands.zadd(key, System.currentTimeMillis() + LEASE_MILLIS, value);
		commands.pexpire(key, LEASE_MILLIS * 2);

		log.info("registered {} ", instance);
	}

	@Override
	public void heartbeat(ServiceInstance instance) {
		String key = toRedisKey(instance.getServiceKey());
		String value = toRedisMember(instance);

		commands.zadd(key, System.currentTimeMillis() + LEASE_MILLIS, value);
		commands.pexpire(key, LEASE_MILLIS * 2);

		log.info("sending heartbeat to {}", instance);
	}

	@Override
	public void unregister(ServiceInstance instance) {
		String key = toRedisKey(instance.getServiceKey());
		String value = toRedisMember(instance);

		commands.zrem(key, value);

		log.info("removed {} from redis", instance);
	}

	@Override
	public List<ServiceInstance> discover(ServiceKey serviceKey) {
		String key = toRedisKey(serviceKey);

		List<ScoredValue<String>> values = commands.zrangeWithScores(key, 0l, -1l);

		long now = System.currentTimeMillis();
		List<ServiceInstance> instances = new ArrayList<ServiceInstance>();
		for (ScoredValue<String> value : values) {
			if (value.getScore() <= now) {
				commands.zrem(key, value.getValue());
				continue;
			}

			String[] parts = value.getValue().split("/", 3);
			ServiceInstance instance = new ServiceInstance(serviceKey, parts[0], parts[1], Integer.parseInt(parts[2]));
			instances.add(instance);
		}

		instances.sort(Comparator.comparing(ServiceInstance::getInstanceId));
		return Collections.unmodifiableList(instances);
	}

	private String toRedisKey(ServiceKey serviceKey) {
		return serviceKey.getServiceName() + ':' + serviceKey.getGroupName() + ':' + serviceKey.getVersion();
	}

	private String toRedisMember(ServiceInstance instance) {
		return instance.getInstanceId() + '/' + instance.getHost() + '/' + instance.getPort();
	}

	@Override
	public void close() throws Exception {
		connection.close();
		client.shutdown();
	}

}
