package com.example.Kcsj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.User;
import com.example.Kcsj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    UserService userService;

    @GetMapping
    public Result<?> findPage(@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
                              @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                              @RequestParam(name = "search", defaultValue = "") String search) {
        Page<User> page = userService.findPage(pageNum, pageSize, search);
        return Result.success(page);
    }

    @GetMapping("/{username}")
    public Result<?> getByUsername(@PathVariable("username") String username) {
        return Result.success(userService.findByUsername(username));
    }

    @GetMapping("/all")
    public Result<?> findAll() {
        return Result.success(userService.findAll());
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody User userParam) {
        User user = userService.login(userParam);
        if (user == null) {
            return Result.error("-1", "用户名不存在或密码错误");
        }
        return Result.success(user);
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        String error = userService.register(user);
        if (error != null) {
            return Result.error("-1", error);
        }
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody User user) {
        String error = userService.updateUser(user);
        if (error != null) {
            return Result.error("-1", error);
        }
        return Result.success();
    }

    @PutMapping("/password")
    public Result<?> resetPassword(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String error = userService.resetPassword(username, password);
        if (error != null) {
            return Result.error("-1", error);
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable("id") int id) {
        userService.deleteById(id);
        return Result.success();
    }

    @PostMapping
    public Result<?> save(@RequestBody User user) {
        String error = userService.saveUser(user);
        if (error != null) {
            return Result.error("-1", error);
        }
        return Result.success();
    }
}
