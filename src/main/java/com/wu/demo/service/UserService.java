package com.wu.demo.service;

import com.alibaba.fastjson.JSON;
import com.wu.demo.dao.UserMapper;
import com.wu.demo.model.User;
import com.wu.demo.model.UserRequest;
import com.wu.spring.annotation.aop.Transactional;
import com.wu.spring.annotation.ioc.Autowired;
import com.wu.spring.annotation.ioc.Service;
import redis.clients.jedis.Jedis;

@Service
public class UserService implements IUserService{

	@Autowired
	public com.wu.demo.service.UserService2 userservice2;

	@Autowired
	UserMapper userMapper;
	
	@Autowired
	Jedis jedis;
	
	@Override
	public boolean registerUser(UserRequest request) {
        try{
            userMapper.insertUser(request.getUserName(), request.getPassword(), System.currentTimeMillis());
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
		return false;
	}

	@Override
	public User loginUser(UserRequest request) {
		User user = userMapper.selectUser(request.getUserName());
		if(null==user || !user.getPassword().equals(request.getPassword())) {
			return null;
		}
		String key = "login:"+user.getUserName();
		jedis.set(key, JSON.toJSONString(user));
		System.out.println(user.getUserName()+" 登录成功");
		return user;
	}

	@Override
	public boolean isUserLogin(String username) {
		String key = "login:"+username;
		System.out.println(username + "仍然在登录");
		return jedis.exists(key);
	}

	@Override
	public boolean logout(String username) {
		// TODO Auto-generated method stub
		String key = "login:"+username;
		if(!jedis.exists(key)) {
			return false;
		}
		jedis.del(key);
		System.out.println(username + "退出成功");
		return true;
	}

	
	@Override
	@Transactional(rollbackFor = {Exception.class})
	public User findUser1(Integer id1) throws Exception {
		// TODO Auto-generated method stub
		return userMapper.selectUser(id1);
	}
	
	@Override
	@Transactional
	public User findUser2(Integer id2) {
		// TODO Auto-generated method stub
		return userMapper.selectUser(id2);
	}

	@Override
	//@Transactional
	public User findUser(Integer id) throws Exception {
		// TODO Auto-generated method stub
		User user1 = findUser1(id);
		
		User user2 = findUser2(id);
		return user1;
		
	}
}
