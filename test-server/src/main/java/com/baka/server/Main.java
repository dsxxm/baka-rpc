package com.baka.server;

import com.baka.server.service.UserServiceImpl;
import com.baka.api.service.UserService;

import com.baka.trans.RpcServer;

import lombok.extern.slf4j.Slf4j;

/**
 * Main
 */
@Slf4j
public class Main {

	public static void main(String[] args) {
		log.info("test server starting......");

		RpcServer server = new RpcServer(9090);
		UserServiceImpl service = new UserServiceImpl();
		server.register(UserService.class, service);
		server.start();
	}
}
