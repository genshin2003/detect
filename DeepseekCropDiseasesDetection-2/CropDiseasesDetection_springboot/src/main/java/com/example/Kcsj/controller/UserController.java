package com.example.Kcsj.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.User;
import com.example.Kcsj.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.Time;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    UserMapper userMapper;

    /**
     * 用户分页列表查询，包含表的一对多查询
     *
     * @param pageNum
     * @param pageSize
     * @param search
     * @return
     */
    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search) {
        LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery();
        wrapper.orderByDesc(User::getId);
        if (StrUtil.isNotBlank(search)) {
            wrapper.like(User::getUsername, search);
        }
        Page<User> UserPage = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(UserPage);
    }

    @GetMapping("/{username}")
    public Result<?> getByUsername(@PathVariable String username) {
        log.info("当前在"+"username:"+username);
//        System.out.println(username);

        return Result.success(userMapper.selectByUsername(username));
    }

    @GetMapping("/all")
    public Result<?> GetAll() {
        return Result.success(userMapper.selectList(null));
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody User userParam) {
        if(userParam.getUsername() == null){
            return Result.error("-1","用户名不能为空");
        }
        User userPwd = userMapper.selectByName(userParam.getUsername());
        if (userPwd == null) {
            return Result.error("-1", "用户不存在");
        }
        log.info("当前在/login");
        String md5password = DigestUtils.md5DigestAsHex(userParam.getPassword().getBytes());
        log.info(md5password);
        log.info(userPwd.getPassword());

        if (!userPwd.getPassword().equals(md5password)){
            return Result.error("-1", "1密码错误");
            }
        userPwd.setPassword(null);
        return Result.success(userPwd);
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        log.info("当前在/register");

        if (user.getUsername() == null || user.getPassword() == null) {
            return Result.error("-1", "用户名或密码不能为空");
        }
        User res = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, user.getUsername()));
        if (res != null) {
            return Result.error("-1", "用户名重复");
        }
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        user.setTime(new Date());
        user.setRole("common");


        userMapper.insert(user);
        return Result.success();
    }
    @PostMapping("/update")
    public Result<?> updates(@RequestBody User user) {
        if (user.getId() == null) {
            return Result.error("-1", "缺少用户ID");
        }

        // 1. 查询数据库中原有的用户信息
        User dbUser = userMapper.selectById(user.getId());
        if (dbUser == null) {
            return Result.error("-1", "用户不存在");
        }

        // 2. 校验用户名是否被其他人占用
        if (user.getUsername() != null && !user.getUsername().equals(dbUser.getUsername())) {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", user.getUsername());
            User existUser = userMapper.selectOne(queryWrapper);

            if (existUser != null) {
                return Result.error("-1", "该账号名已被占用，请更换一个！");
            }
        }

        // 3. 处理密码逻辑：判断是否需要加密
        String inputPassword = user.getPassword();
        if (inputPassword != null && !inputPassword.trim().isEmpty()) {

            // 正则判断：是否为 32 位十六进制字符串（MD5 特征）
            boolean isMd5 = inputPassword.matches("^[a-fA-F0-9]{32}$");

            if (!isMd5) {
                // 如果不是 MD5，说明用户输入了新明文密码，进行加密
                user.setPassword(DigestUtils.md5DigestAsHex(inputPassword.getBytes()));
            } else {
                // 如果已经是 MD5，说明是前端传回的旧密文，保持原样即可
                // 或者用户手动输入了一个长得像 MD5 的密码（概率极低）
                log.info("检测到修改密码为 MD5 格式，跳过重复加密");
            }
        } else {
            // 如果前端没传密码，设为 null，MyBatis-Plus 的 updateById 不会更新该字段
            user.setPassword(null);
        }

        // 4. 执行更新操作
        userMapper.updateById(user);
        return Result.success();
    }

    @PostMapping("/resetPassword")
    public Result<?> resetPassword(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            return Result.error("-1", "用户名或密码不能为空");
        }
        User dbUser = userMapper.selectByUsername(username);
        if (dbUser == null) {
            return Result.error("-1", "用户名不存在");
        }
        User updateUser = new User();
        updateUser.setId(dbUser.getId());
        updateUser.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        userMapper.updateById(updateUser);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable int id) {
        userMapper.deleteById(id);
        return Result.success();
    }
    @PostMapping
    public Result<?> save(@RequestBody User user) {
        log.info("新增用户接口触发");

        // 1. 基础校验
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return Result.error("-1", "用户名不能为空");
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return Result.error("-1", "密码不能为空");
        }

        // 2. 判断并处理密码
        String rawPassword = user.getPassword();

        // 正则解释：^ 开始，$ 结束，[a-fA-F0-9] 十六进制字符，{32} 长度正好32位
        boolean isMd5 = rawPassword.matches("^[a-fA-F0-9]{32}$");

        if (!isMd5) {
            // 如果不是 MD5 格式，则进行加密
            String encryptedPassword = DigestUtils.md5DigestAsHex(rawPassword.getBytes());
            user.setPassword(encryptedPassword);
        } else {
            // 如果已经是 MD5 格式，则保持原样，无需操作
            log.info("检测到密码已是 MD5 格式，跳过加密步骤");
        }

        // 3. 设置默认属性并保存

        user.setRole("common");
        user.setTime(new Date());


        userMapper.insert(user);
        return Result.success();
    }
}
