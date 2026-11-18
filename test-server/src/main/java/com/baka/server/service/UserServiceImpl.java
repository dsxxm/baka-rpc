package com.baka.server.service;

import com.baka.api.dto.User;
import com.baka.api.service.UserService;

/**
 * UserServiceImpl
 */
public class UserServiceImpl implements UserService {

	@Override
	public User getUser(Long id) {
		return new User(1L, "bakaxxm");
	}

}
