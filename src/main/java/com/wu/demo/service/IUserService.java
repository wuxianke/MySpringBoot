package com.wu.demo.service;

import com.wu.demo.model.User;
import com.wu.demo.model.UserRequest;

public interface IUserService {
	boolean registerUser(UserRequest request);
	
	User loginUser(UserRequest request);
	
	boolean isUserLogin(String username);
	
	boolean logout(String username);
	
	User findUser(Integer id) throws Exception;
	
	User findUser1(Integer id1) throws Exception;
	
	User findUser2(Integer id2);
}
