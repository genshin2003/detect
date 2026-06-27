package com.example.Kcsj.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.JwtUtils;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.User;
import com.example.Kcsj.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import jakarta.annotation.Resource;
import java.util.Date;

@Slf4j
@Service
public class UserService {
    @Resource
    UserMapper userMapper;

    public Page<User> findPage(Integer pageNum, Integer pageSize, String search) {
        LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery();
        wrapper.orderByDesc(User::getId);
        if (StrUtil.isNotBlank(search)) {
            wrapper.like(User::getUsername, search);
        }
        return userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public User findByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    public java.util.List<User> findAll() {
        return userMapper.selectList(null);
    }

    public User login(User userParam) {
        if (userParam.getUsername() == null) {
            return null;
        }
        User userPwd = userMapper.selectByName(userParam.getUsername());
        if (userPwd == null) {
            return null;
        }
        String md5password = DigestUtils.md5DigestAsHex(userParam.getPassword().getBytes());
        if (!userPwd.getPassword().equals(md5password)) {
            return null;
        }
        String token = JwtUtils.createToken(userPwd.getId(), userPwd.getUsername(), userPwd.getRole());
        userPwd.setToken(token);
        userPwd.setPassword(null);
        return userPwd;
    }

    public String register(User user) {
        if (user.getUsername() == null || user.getPassword() == null) {
            return "用户名或密码不能为空";
        }
        User res = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, user.getUsername()));
        if (res != null) {
            return "用户名重复";
        }
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        user.setTime(new Date());
        user.setRole("common");
        userMapper.insert(user);
        return null;
    }

    public String updateUser(User user) {
        if (user.getId() == null) {
            return "缺少用户ID";
        }
        User dbUser = userMapper.selectById(user.getId());
        if (dbUser == null) {
            return "用户不存在";
        }
        if (user.getUsername() != null && !user.getUsername().equals(dbUser.getUsername())) {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", user.getUsername());
            User existUser = userMapper.selectOne(queryWrapper);
            if (existUser != null) {
                return "该账号名已被占用，请更换一个！";
            }
        }
        String inputPassword = user.getPassword();
        if (inputPassword != null && !inputPassword.trim().isEmpty()) {
            boolean isMd5 = inputPassword.matches("^[a-fA-F0-9]{32}$");
            if (!isMd5) {
                user.setPassword(DigestUtils.md5DigestAsHex(inputPassword.getBytes()));
            }
        } else {
            user.setPassword(null);
        }
        userMapper.updateById(user);
        return null;
    }

    public String resetPassword(String username, String password) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return "用户名或密码不能为空";
        }
        User dbUser = userMapper.selectByUsername(username);
        if (dbUser == null) {
            return "用户名不存在";
        }
        User updateUser = new User();
        updateUser.setId(dbUser.getId());
        updateUser.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        userMapper.updateById(updateUser);
        return null;
    }

    public void deleteById(int id) {
        userMapper.deleteById(id);
    }

    public String saveUser(User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return "用户名不能为空";
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return "密码不能为空";
        }
        String rawPassword = user.getPassword();
        boolean isMd5 = rawPassword.matches("^[a-fA-F0-9]{32}$");
        if (!isMd5) {
            String encryptedPassword = DigestUtils.md5DigestAsHex(rawPassword.getBytes());
            user.setPassword(encryptedPassword);
        }
        user.setRole("common");
        user.setTime(new Date());
        userMapper.insert(user);
        return null;
    }
}
