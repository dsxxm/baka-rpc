package com.baka.server.service;

import com.baka.api.protobuf.UserProto.GetUserRequest;
import com.baka.api.protobuf.UserProto.User;
import com.baka.api.service.UserService;

/**
 * UserServiceImpl
 */
public class UserServiceImpl implements UserService {

	@Override
	public User getUser(GetUserRequest request) {
		return User.newBuilder().setId(request.getId()).setName("bakaxxm").build();
	}

}
