package com.example.Kcsj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.JwtUtils;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.Favorite;
import com.example.Kcsj.entity.Message;
import com.example.Kcsj.entity.UserMessageSetting;
import com.example.Kcsj.handler.MessageWebSocketHandler;
import com.example.Kcsj.service.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/message")
public class MessageController {
    @Resource
    MessageService messageService;
    @Resource
    MessageWebSocketHandler webSocketHandler;

    private Integer getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof Integer) return (Integer) userId;
        if (userId != null) return Integer.parseInt(userId.toString());
        return null;
    }

    private String getUsername(HttpServletRequest request) {
        Object username = request.getAttribute("username");
        return username != null ? username.toString() : null;
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                          @RequestParam(name = "type", required = false) String type,
                          @RequestParam(name = "isRead", required = false) Boolean isRead,
                          HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.error("-1", "未登录");
        }
        Page<Message> page = messageService.findPage(pageNum, pageSize, userId, type, isRead);
        return Result.success(page);
    }

    @GetMapping("/unreadCount")
    public Result<?> unreadCount(HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.error("-1", "未登录");
        }
        long count = messageService.countUnread(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    @PostMapping("/read/{id}")
    public Result<?> markAsRead(@PathVariable("id") Integer id, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.error("-1", "未登录");
        }
        messageService.markAsRead(id, userId);
        return Result.success();
    }

    @PostMapping("/readAll")
    public Result<?> markAllAsRead(HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.error("-1", "未登录");
        }
        messageService.markAllAsRead(userId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteMessage(@PathVariable("id") Integer id, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.error("-1", "未登录");
        }
        boolean success = messageService.deleteMessage(id, userId);
        if (success) {
            // 同步更新未读数
            return Result.success();
        } else {
            return Result.error("-1", "删除失败，消息不存在或无权限");
        }
    }

    @DeleteMapping("/batch")
    public Result<?> deleteMessages(@RequestBody List<Integer> ids, HttpServletRequest request) {
        Integer userId = getUserId(request);
        if (userId == null) {
            return Result.error("-1", "未登录");
        }
        int count = messageService.deleteMessages(ids, userId);
        return Result.success("成功删除 " + count + " 条消息");
    }

    @PostMapping("/publish")
    public Result<?> publish(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String role = getRole(request);
        if (!"admin".equals(role)) {
            return Result.error("-1", "只有管理员可以发布公告");
        }
        String title = (String) params.get("title");
        String content = (String) params.get("content");
        String type = (String) params.get("type");
        String priority = (String) params.get("priority");
        String scope = (String) params.get("scope"); // all 或 specific
        List<String> usernames = (List<String>) params.get("usernames"); // 指定的用户名

        if (title == null || content == null) {
            return Result.error("-1", "标题和内容不能为空");
        }
        // 默认使用ANNOUNCE类型
        if (type == null || type.isEmpty()) {
            type = "ANNOUNCE";
        }
        // 验证类型是否合法
        if (!"ANNOUNCE".equals(type) && !"SECURITY".equals(type) &&
            !"DETECT".equals(type) && !"BATCH_DETECT".equals(type)) {
            return Result.error("-1", "无效的消息类型");
        }

        // 根据发送范围处理
        if ("specific".equals(scope) && usernames != null && !usernames.isEmpty()) {
            // 发送给指定用户
            messageService.sendToUsers(usernames, title, content, type, priority);
            return Result.success("已发送给 " + usernames.size() + " 个用户");
        } else {
            // 发送给所有用户
            messageService.sendToAll(title, content, type, priority);
            return Result.success();
        }
    }

    @GetMapping("/settings")
    public Result<?> getSettings(HttpServletRequest request) {
        String username = getUsername(request);
        if (username == null) {
            return Result.error("-1", "未登录");
        }
        return Result.success(messageService.getSetting(username));
    }

    @PutMapping("/settings")
    public Result<?> updateSettings(@RequestBody UserMessageSetting setting) {
        messageService.updateSetting(setting);
        return Result.success();
    }

    @GetMapping("/stats")
    public Result<?> getStats(HttpServletRequest request) {
        String role = getRole(request);
        if (!"admin".equals(role)) {
            return Result.error("-1", "只有管理员可以查看消息统计");
        }
        return Result.success(messageService.getMessageStats());
    }

    @GetMapping("/users")
    public Result<?> getUserList(HttpServletRequest request) {
        String role = getRole(request);
        if (!"admin".equals(role)) {
            return Result.error("-1", "只有管理员可以获取用户列表");
        }
        return Result.success(messageService.getUserList());
    }

    private String getRole(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null ? role.toString() : null;
    }
}
