package com.wu.demo.dao;

import com.wu.demo.model.User;
import com.wu.mybatis.annotation.Insert;
import com.wu.mybatis.annotation.Mapper;
import com.wu.mybatis.annotation.Param;
import com.wu.mybatis.annotation.Select;

@Mapper
public interface UserMapper {

	@Select("select * from user where id=#{id}")
	public User selectUser(@Param("id") Integer id);
	
	@Insert("insert into user(username,password,timestamp) values(#{username},#{password},#{timestamp})")
	public int insertUser(@Param("username") String username,@Param("password") String password,@Param("timestamp") Long timestamp);
	
	@Select("select * from user where username=#{username}")
	public User selectUser(@Param("username") String username);
}
