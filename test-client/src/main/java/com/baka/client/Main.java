package com.baka.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.baka.api.protobuf.UserProto.GetUserRequest;
import com.baka.api.protobuf.UserProto.User;
import com.baka.api.service.UserService;
import com.baka.Registry.RedisServiceRegistry;
import com.baka.protocol.RpcMessage;

import com.baka.registry.RoundRobinLoadBalancer;
import com.baka.trans.RpcClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Main
 */
@Slf4j
public class Main {
	private static final int CONCURRENCY = 10;
	private static final int REQUEST_COUNT = 100;

	public static void main(String[] args) throws Exception {
		RedisServiceRegistry registry = new RedisServiceRegistry("localhost", 6379);
		RpcClient client = new RpcClient(registry, new RoundRobinLoadBalancer(), RpcMessage.Codec.PROTOBUF);
		UserService userService = client.getProxy(UserService.class);
		User user = userService.getUser(GetUserRequest.newBuilder().setId(1L).build());
		client.close();
		log.info("succeed to recevie {}", user);
	}

	private void testConcurrency() throws Exception {
		log.info("test client starting with {} concurrent workers and {} requests", CONCURRENCY, REQUEST_COUNT);

		try (RedisServiceRegistry registry = new RedisServiceRegistry("localhost", 6379); RpcClient client = new RpcClient(registry, new RoundRobinLoadBalancer(), RpcMessage.Codec.PROTOBUF)) {
			UserService userService = client.getProxy(UserService.class);
			ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
			CountDownLatch start = new CountDownLatch(1);
			List<Future<User>> futures = new ArrayList<>(REQUEST_COUNT);

			try {
				for (long id = 1; id <= REQUEST_COUNT; id++) {
					long userId = id;
					futures.add(executor.submit(() -> {
						start.await();
						return userService.getUser(GetUserRequest.newBuilder().setId(userId).build());
					}));
				}

				start.countDown();
				for (Future<User> future : futures) {
					User user = future.get();
					if (user == null) {
						throw new IllegalStateException("RPC returned a null user");
					}
				}
				log.info("concurrent RPC test completed: {} successful requests", REQUEST_COUNT);
			} finally {
				executor.shutdown();
				if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			}
		}
	}
}
