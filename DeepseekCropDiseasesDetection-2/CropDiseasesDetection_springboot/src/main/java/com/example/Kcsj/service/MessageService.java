package com.example.Kcsj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.entity.Message;
import com.example.Kcsj.entity.MessageReadStatus;
import com.example.Kcsj.entity.User;
import com.example.Kcsj.entity.UserMessageSetting;
import com.example.Kcsj.handler.MessageWebSocketHandler;
import com.example.Kcsj.mapper.MessageMapper;
import com.example.Kcsj.mapper.MessageReadStatusMapper;
import com.example.Kcsj.mapper.UserMapper;
import com.example.Kcsj.mapper.UserMessageSettingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.util.*;

@Slf4j
@Service
public class MessageService {
    @Resource
    MessageMapper messageMapper;
    @Resource
    MessageReadStatusMapper readStatusMapper;
    @Resource
    UserMessageSettingMapper settingMapper;
    @Resource
    UserMapper userMapper;
    @Resource
    MessageWebSocketHandler webSocketHandler;

    /**
     * 查询用户的消息列表
     * 管理员：能看到所有消息
     * 普通用户：广播消息 + 发给自己的消息
     */
    public Page<Message> findPage(Integer pageNum, Integer pageSize, Integer userId, String type, Boolean isRead) {
        String role = getUserRole(userId);
        boolean isAdmin = "admin".equals(role);

        LambdaQueryWrapper<Message> wrapper = Wrappers.<Message>lambdaQuery();

        if (isAdmin) {
            // 管理员能看到所有消息
            // 不添加user_id过滤条件
        } else {
            // 普通用户：广播消息 + 发给自己的消息
            wrapper.and(w -> w.eq(Message::getUserId, 0).or().eq(Message::getUserId, userId));
        }

        if (type != null && !type.isEmpty()) {
            wrapper.eq(Message::getType, type);
        }

        wrapper.orderByDesc(Message::getCreateTime);
        Page<Message> page = messageMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 为每条消息设置已读状态和接收者信息，同时过滤已隐藏的广播消息
        // 管理员删除广播消息时是硬删除，不会出现在查询结果中；普通用户删除时是隐藏
        List<Message> visibleMessages = new ArrayList<>();
        for (Message msg : page.getRecords()) {
            if (!isAdmin && msg.getUserId() == 0 && isMessageHidden(msg.getId(), userId)) {
                continue;
            }
            msg.setIsRead(isMessageRead(msg, userId, isAdmin));
            // 设置接收者显示
            if (msg.getUserId() == 0) {
                msg.setReceiverName("全体用户");
            } else {
                User receiver = userMapper.selectById(msg.getUserId());
                msg.setReceiverName(receiver != null ? receiver.getUsername() : "未知用户");
            }
            visibleMessages.add(msg);
        }

        // 如果有已读状态筛选，需要在内存中过滤（因为已读状态涉及两个表）
        if (isRead != null) {
            List<Message> filtered = new ArrayList<>();
            for (Message msg : visibleMessages) {
                if (isRead.equals(msg.getIsRead())) {
                    filtered.add(msg);
                }
            }
            page.setRecords(filtered);
            page.setTotal(filtered.size());
        } else {
            page.setRecords(visibleMessages);
            page.setTotal(visibleMessages.size());
        }

        return page;
    }

    /**
     * 获取用户角色
     */
    private String getUserRole(Integer userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getRole() : "common";
    }

    /**
     * 判断广播消息是否对用户隐藏
     */
    private boolean isMessageHidden(Integer messageId, Integer userId) {
        LambdaQueryWrapper<MessageReadStatus> wrapper = Wrappers.<MessageReadStatus>lambdaQuery();
        wrapper.eq(MessageReadStatus::getMessageId, messageId)
                .eq(MessageReadStatus::getUserId, userId)
                .eq(MessageReadStatus::getHidden, true);
        return readStatusMapper.selectCount(wrapper) > 0;
    }

    /**
     * 判断消息是否已读
     */
    private boolean isMessageRead(Message message, Integer userId, boolean isAdmin) {
        if (message.getUserId() == 0) {
            // 广播消息，检查message_read_status表
            LambdaQueryWrapper<MessageReadStatus> wrapper = Wrappers.<MessageReadStatus>lambdaQuery();
            wrapper.eq(MessageReadStatus::getMessageId, message.getId())
                    .eq(MessageReadStatus::getUserId, userId);
            return readStatusMapper.selectCount(wrapper) > 0;
        } else {
            // 个人消息
            if (isAdmin) {
                // 管理员看所有消息，用is_read字段
                return Boolean.TRUE.equals(message.getIsRead());
            } else {
                // 普通用户看自己的消息
                return message.getUserId().equals(userId) && Boolean.TRUE.equals(message.getIsRead());
            }
        }
    }

    /**
     * 统计用户未读消息数
     */
    public long countUnread(Integer userId) {
        String role = getUserRole(userId);
        boolean isAdmin = "admin".equals(role);

        long count = 0;

        if (isAdmin) {
            // 管理员：统计所有未读的广播消息 + 所有用户的未读个人消息
            // 1. 广播消息中未读的
            LambdaQueryWrapper<Message> broadcastWrapper = Wrappers.<Message>lambdaQuery();
            broadcastWrapper.eq(Message::getUserId, 0);
            List<Message> broadcastMessages = messageMapper.selectList(broadcastWrapper);
            for (Message msg : broadcastMessages) {
                if (!isMessageRead(msg, userId, true)) {
                    count++;
                }
            }
            // 2. 所有用户的未读个人消息
            LambdaQueryWrapper<Message> personalWrapper = Wrappers.<Message>lambdaQuery();
            personalWrapper.ne(Message::getUserId, 0).eq(Message::getIsRead, false);
            count += messageMapper.selectCount(personalWrapper);
        } else {
            // 普通用户：统计广播消息中未读的 + 发给自己的未读消息
            // 1. 广播消息中未读的（排除已隐藏的）
            LambdaQueryWrapper<Message> broadcastWrapper = Wrappers.<Message>lambdaQuery();
            broadcastWrapper.eq(Message::getUserId, 0);
            List<Message> broadcastMessages = messageMapper.selectList(broadcastWrapper);
            for (Message msg : broadcastMessages) {
                if (!isMessageHidden(msg.getId(), userId) && !isMessageRead(msg, userId, false)) {
                    count++;
                }
            }
            // 2. 发给自己的未读消息
            LambdaQueryWrapper<Message> personalWrapper = Wrappers.<Message>lambdaQuery();
            personalWrapper.eq(Message::getUserId, userId).eq(Message::getIsRead, false);
            count += messageMapper.selectCount(personalWrapper);
        }

        return count;
    }

    /**
     * 标记单条消息已读
     */
    public void markAsRead(Integer id, Integer userId) {
        Message msg = messageMapper.selectById(id);
        if (msg == null) return;

        if (msg.getUserId() == 0) {
            // 广播消息，插入或更新已读状态记录
            LambdaQueryWrapper<MessageReadStatus> wrapper = Wrappers.<MessageReadStatus>lambdaQuery();
            wrapper.eq(MessageReadStatus::getMessageId, id)
                    .eq(MessageReadStatus::getUserId, userId);
            MessageReadStatus existing = readStatusMapper.selectOne(wrapper);
            if (existing == null) {
                MessageReadStatus status = MessageReadStatus.builder()
                        .messageId(id)
                        .userId(userId)
                        .readTime(new Date())
                        .hidden(false)
                        .build();
                readStatusMapper.insert(status);
            } else {
                existing.setReadTime(new Date());
                existing.setHidden(false);
                readStatusMapper.updateById(existing);
            }
        } else {
            // 个人消息，更新is_read字段
            msg.setIsRead(true);
            messageMapper.updateById(msg);
        }
    }

    /**
     * 删除单条消息（用户维度）
     * 广播消息：标记为对该用户隐藏
     * 个人消息：直接删除
     */
    public boolean deleteMessage(Integer messageId, Integer userId) {
        Message msg = messageMapper.selectById(messageId);
        if (msg == null) return false;

        String role = getUserRole(userId);
        boolean isAdmin = "admin".equals(role);

        if (msg.getUserId() == 0) {
            // 广播消息
            if (isAdmin) {
                // 管理员：硬删除，同时清理已读状态记录
                readStatusMapper.delete(
                        Wrappers.<MessageReadStatus>lambdaQuery()
                                .eq(MessageReadStatus::getMessageId, messageId));
                messageMapper.deleteById(messageId);
            } else {
                // 普通用户：标记为对该用户隐藏
                LambdaQueryWrapper<MessageReadStatus> wrapper = Wrappers.<MessageReadStatus>lambdaQuery();
                wrapper.eq(MessageReadStatus::getMessageId, messageId)
                        .eq(MessageReadStatus::getUserId, userId);
                MessageReadStatus existing = readStatusMapper.selectOne(wrapper);
                if (existing != null) {
                    existing.setHidden(true);
                    readStatusMapper.updateById(existing);
                } else {
                    MessageReadStatus status = MessageReadStatus.builder()
                            .messageId(messageId)
                            .userId(userId)
                            .readTime(new Date())
                            .hidden(true)
                            .build();
                    readStatusMapper.insert(status);
                }
            }
        } else {
            // 个人消息，管理员或消息所有者可以删除
            if (!isAdmin && !msg.getUserId().equals(userId)) {
                return false;
            }
            messageMapper.deleteById(messageId);
        }
        return true;
    }

    /**
     * 批量删除消息
     */
    public int deleteMessages(List<Integer> messageIds, Integer userId) {
        int count = 0;
        for (Integer id : messageIds) {
            if (deleteMessage(id, userId)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 全部标记已读
     */
    public void markAllAsRead(Integer userId) {
        String role = getUserRole(userId);
        boolean isAdmin = "admin".equals(role);

        if (isAdmin) {
            // 管理员：标记所有广播消息为已读 + 标记所有个人消息为已读
            // 1. 标记广播消息
            LambdaQueryWrapper<Message> broadcastWrapper = Wrappers.<Message>lambdaQuery();
            broadcastWrapper.eq(Message::getUserId, 0);
            List<Message> broadcastMessages = messageMapper.selectList(broadcastWrapper);
            for (Message msg : broadcastMessages) {
                markAsRead(msg.getId(), userId);
            }
            // 2. 标记所有个人消息
            LambdaQueryWrapper<Message> personalWrapper = Wrappers.<Message>lambdaQuery();
            personalWrapper.ne(Message::getUserId, 0).eq(Message::getIsRead, false);
            List<Message> personalMessages = messageMapper.selectList(personalWrapper);
            for (Message msg : personalMessages) {
                msg.setIsRead(true);
                messageMapper.updateById(msg);
            }
        } else {
            // 普通用户：标记广播消息 + 发给自己的消息
            // 1. 标记广播消息
            LambdaQueryWrapper<Message> broadcastWrapper = Wrappers.<Message>lambdaQuery();
            broadcastWrapper.eq(Message::getUserId, 0);
            List<Message> broadcastMessages = messageMapper.selectList(broadcastWrapper);
            for (Message msg : broadcastMessages) {
                markAsRead(msg.getId(), userId);
            }
            // 2. 标记发给自己的消息
            LambdaQueryWrapper<Message> personalWrapper = Wrappers.<Message>lambdaQuery();
            personalWrapper.eq(Message::getUserId, userId).eq(Message::getIsRead, false);
            List<Message> personalMessages = messageMapper.selectList(personalWrapper);
            for (Message msg : personalMessages) {
                msg.setIsRead(true);
                messageMapper.updateById(msg);
            }
        }
    }

    /**
     * 发送消息给指定用户
     */
    public void sendToUser(Integer userId, String title, String content, String type, String priority) {
        // 检查用户消息偏好
        if (!shouldNotify(userId, type)) {
            return;
        }
        Message message = Message.builder()
                .userId(userId)
                .title(title)
                .content(content)
                .type(type)
                .priority(priority != null ? priority : "NORMAL")
                .isRead(false)
                .createTime(new Date())
                .build();
        messageMapper.insert(message);

        // 通过WebSocket推送实时通知
        sendWebSocketNotification(userId, message);
    }

    /**
     * 通过用户名发送消息
     */
    public void sendToUserByUsername(String username, String title, String content, String type, String priority) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user != null) {
            sendToUser(user.getId(), title, content, type, priority);
        }
    }

    /**
     * 广播消息给所有用户（只存一条记录）
     */
    public void sendToAll(String title, String content, String type, String priority) {
        // 只存储一条广播消息
        Message message = Message.builder()
                .userId(0)  // 0表示广播消息
                .title(title)
                .content(content)
                .type(type)
                .priority(priority != null ? priority : "NORMAL")
                .isRead(false)
                .createTime(new Date())
                .build();
        messageMapper.insert(message);

        // 给每个用户推送WebSocket通知
        List<User> users = userMapper.selectList(null);
        for (User user : users) {
            if (shouldNotify(user.getId(), type)) {
                sendWebSocketNotification(user.getId(), message);
            }
        }
    }

    /**
     * 发送给多个指定用户
     */
    public void sendToUsers(List<String> usernames, String title, String content, String type, String priority) {
        for (String username : usernames) {
            sendToUserByUsername(username, title, content, type, priority);
        }
    }

    /**
     * 发送消息给用户和管理员（用于检测通知等）
     */
    public void sendToUserAndAdmin(Integer userId, String title, String content, String type, String priority) {
        // 发送给用户
        sendToUser(userId, title, content, type, priority);

        // 发送给管理员（如果用户不是管理员）
        String userRole = getUserRole(userId);
        if (!"admin".equals(userRole)) {
            // 查找所有管理员
            List<User> admins = userMapper.selectList(
                    Wrappers.<User>lambdaQuery().eq(User::getRole, "admin"));
            for (User admin : admins) {
                if (!admin.getId().equals(userId)) {
                    sendToUser(admin.getId(), title, content, type, priority);
                }
            }
        }
    }

    /**
     * 通过用户名发送消息给用户和管理员
     */
    public void sendToUserByUsernameAndAdmin(String username, String title, String content, String type, String priority) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (user != null) {
            sendToUserAndAdmin(user.getId(), title, content, type, priority);
        }
    }

    /**
     * 发送WebSocket通知
     */
    private void sendWebSocketNotification(Integer userId, Message message) {
        try {
            Map<String, Object> wsData = new HashMap<>();
            wsData.put("type", "notification");
            wsData.put("title", message.getTitle());
            wsData.put("content", message.getContent());
            wsData.put("messageType", message.getType());
            wsData.put("priority", message.getPriority());
            wsData.put("messageId", message.getId());
            wsData.put("createTime", message.getCreateTime());
            webSocketHandler.sendToUser(userId, wsData);
        } catch (Exception e) {
            log.error("WebSocket推送通知失败: userId={}", userId, e);
        }
    }

    /**
     * 检查是否应该发送通知
     */
    private boolean shouldNotify(Integer userId, String type) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;

        UserMessageSetting setting = settingMapper.selectOne(
                Wrappers.<UserMessageSetting>lambdaQuery()
                        .eq(UserMessageSetting::getUsername, user.getUsername()));
        if (setting == null) {
            return true; // 默认全部开启
        }

        // 检查免打扰
        if (Boolean.TRUE.equals(setting.getDoNotDisturb())) {
            if (setting.getDndEndTime() != null && new Date().after(setting.getDndEndTime())) {
                setting.setDoNotDisturb(false);
                settingMapper.updateById(setting);
            } else {
                return "SECURITY".equals(type); // 免打扰期间只推安全通知
            }
        }

        switch (type) {
            case "DETECT":
                return setting.getDetectNotify() == null || setting.getDetectNotify();
            case "BATCH_DETECT":
                return setting.getBatchNotify() == null || setting.getBatchNotify();
            case "ANNOUNCE":
                return setting.getAnnounceNotify() == null || setting.getAnnounceNotify();
            case "SECURITY":
                return setting.getSecurityNotify() == null || setting.getSecurityNotify();
            default:
                return true;
        }
    }

    /**
     * 获取用户消息设置
     */
    public UserMessageSetting getSetting(String username) {
        UserMessageSetting setting = settingMapper.selectOne(
                Wrappers.<UserMessageSetting>lambdaQuery().eq(UserMessageSetting::getUsername, username));
        if (setting == null) {
            setting = UserMessageSetting.builder()
                    .username(username)
                    .detectNotify(true)
                    .batchNotify(true)
                    .announceNotify(true)
                    .securityNotify(true)
                    .doNotDisturb(false)
                    .build();
            settingMapper.insert(setting);
        }
        return setting;
    }

    /**
     * 更新用户消息设置
     */
    public void updateSetting(UserMessageSetting setting) {
        UserMessageSetting existing = settingMapper.selectOne(
                Wrappers.<UserMessageSetting>lambdaQuery().eq(UserMessageSetting::getUsername, setting.getUsername()));
        if (existing != null) {
            setting.setId(existing.getId());
            settingMapper.updateById(setting);
        } else {
            settingMapper.insert(setting);
        }
    }

    /**
     * 获取用户列表（管理员用）
     */
    public List<Map<String, Object>> getUserList() {
        List<User> users = userMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User user : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("name", user.getName());
            map.put("role", user.getRole());
            result.add(map);
        }
        return result;
    }

    /**
     * 获取消息统计（管理员用）
     */
    public Map<String, Object> getMessageStats() {
        Map<String, Object> stats = new HashMap<>();

        // 消息总数
        long totalCount = messageMapper.selectCount(null);
        stats.put("totalCount", totalCount);

        // 各类型消息数量
        Map<String, Long> typeCount = new HashMap<>();
        typeCount.put("ANNOUNCE", messageMapper.selectCount(
                Wrappers.<Message>lambdaQuery().eq(Message::getType, "ANNOUNCE")));
        typeCount.put("SECURITY", messageMapper.selectCount(
                Wrappers.<Message>lambdaQuery().eq(Message::getType, "SECURITY")));
        typeCount.put("DETECT", messageMapper.selectCount(
                Wrappers.<Message>lambdaQuery().eq(Message::getType, "DETECT")));
        typeCount.put("BATCH_DETECT", messageMapper.selectCount(
                Wrappers.<Message>lambdaQuery().eq(Message::getType, "BATCH_DETECT")));
        stats.put("typeCount", typeCount);

        // 各类型占比
        Map<String, Double> typePercentage = new HashMap<>();
        if (totalCount > 0) {
            typePercentage.put("ANNOUNCE", (double) typeCount.get("ANNOUNCE") / totalCount * 100);
            typePercentage.put("SECURITY", (double) typeCount.get("SECURITY") / totalCount * 100);
            typePercentage.put("DETECT", (double) typeCount.get("DETECT") / totalCount * 100);
            typePercentage.put("BATCH_DETECT", (double) typeCount.get("BATCH_DETECT") / totalCount * 100);
        }
        stats.put("typePercentage", typePercentage);

        // 广播消息阅读统计
        long userCount = userMapper.selectCount(null);
        Map<String, Object> broadcastStats = new HashMap<>();

        // 统计广播公告的已读情况
        LambdaQueryWrapper<Message> announceWrapper = Wrappers.<Message>lambdaQuery();
        announceWrapper.eq(Message::getType, "ANNOUNCE").eq(Message::getUserId, 0);
        long announceCount = messageMapper.selectCount(announceWrapper);

        if (announceCount > 0 && userCount > 0) {
            long announceReadRecords = readStatusMapper.selectCount(
                    Wrappers.<MessageReadStatus>lambdaQuery()
                            .inSql(MessageReadStatus::getMessageId,
                                    "SELECT id FROM message WHERE type='ANNOUNCE' AND user_id=0"));
            // 已读率 = 已读记录数 / (公告数 * 用户数)
            double readRate = (double) announceReadRecords / (announceCount * userCount) * 100;
            broadcastStats.put("announceCount", announceCount);
            broadcastStats.put("totalReadRecords", announceReadRecords);
            broadcastStats.put("readRate", Math.round(readRate * 100.0) / 100.0);
        } else {
            broadcastStats.put("announceCount", 0);
            broadcastStats.put("totalReadRecords", 0);
            broadcastStats.put("readRate", 0.0);
        }
        stats.put("broadcastStats", broadcastStats);

        // 管理员未读消息数
        User admin = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getRole, "admin"));
        if (admin != null) {
            stats.put("adminUnreadCount", countUnread(admin.getId()));
        }

        return stats;
    }
}
