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

@TableName("favorite_folder")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteFolder {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String name;
    private String description;
    private Boolean isPublic;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
