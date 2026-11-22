package com.baka.api.service;

import com.baka.api.protobuf.UserProto.GetUserRequest;
import com.baka.api.protobuf.UserProto.User;

/**
 * UserService
 */
public interface UserService {

	User getUser(GetUserRequest request);
}
