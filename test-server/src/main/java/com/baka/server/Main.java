package com.baka.server;

import com.baka.server.service.UserServiceImpl;
import com.baka.Registry.RedisServiceRegistry;
import com.baka.api.service.UserService;

import com.baka.trans.RpcServer;

import lombok.extern.slf4j.Slf4j;

/**
 * Main
 */
@Slf4j
public class Main {

	public static void main(String[] args) {
		if (args.length != 1) {
			throw new IllegalArgumentException("usage: Main <port>");
		}

		int port = Integer.parseInt(args[0]);
		if (port < 1 || port > 65535) {
			throw new IllegalArgumentException("port must be between 1 and 65535");
		}

		log.info("test server starting on port {}......", port);
		RpcServer server = new RpcServer("localhost", port, new RedisServiceRegistry("localhost", 6379));

		UserServiceImpl service = new UserServiceImpl();
		server.register(UserService.class, service);
		server.start();
	}
}
