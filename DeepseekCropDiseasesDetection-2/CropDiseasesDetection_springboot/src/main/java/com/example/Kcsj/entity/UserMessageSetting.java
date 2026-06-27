package com.example.Kcsj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@TableName("user_message_setting")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserMessageSetting {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private Boolean detectNotify;
    private Boolean batchNotify;
    private Boolean announceNotify;
    private Boolean securityNotify;
    private Boolean doNotDisturb;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date dndEndTime;
}
