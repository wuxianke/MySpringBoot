package com.wu.demo.service;

import com.wu.demo.dao.UserMapper;
import com.wu.spring.annotation.ioc.Autowired;
import com.wu.spring.annotation.ioc.Service;


@Service
public class UserService2 {
	@Autowired
	public UserService userService;
	
	@Autowired
	UserMapper userMapper;
}
