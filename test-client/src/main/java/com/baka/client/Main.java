package com.baka.client;

import com.baka.api.dto.User;
import com.baka.api.service.UserService;
import com.baka.trans.RpcClient;

import lombok.extern.slf4j.Slf4j;

/**
 * Main
 */
@Slf4j
public class Main {

	public static void main(String[] args) {
		log.info("test client starting......");

		UserService userService = new RpcClient("localhost", 9090).getProxy(UserService.class);
		User user = userService.getUser(1L);
		log.info("client get user {}", user);
	}
}
