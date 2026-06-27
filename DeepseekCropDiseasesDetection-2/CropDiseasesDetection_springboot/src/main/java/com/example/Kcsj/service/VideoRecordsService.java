package com.example.Kcsj.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.entity.VideoRecords;
import com.example.Kcsj.mapper.VideoRecordsMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class VideoRecordsService {
    @Resource
    VideoRecordsMapper videoRecordsMapper;

    public Page<VideoRecords> findPage(Integer pageNum, Integer pageSize, String username,
                                       String startTime, String endTime, String conf) {
        LambdaQueryWrapper<VideoRecords> wrapper = Wrappers.<VideoRecords>lambdaQuery();
        wrapper.orderByDesc(VideoRecords::getStartTime);
        if (StrUtil.isNotBlank(username)) {
            wrapper.like(VideoRecords::getUsername, username);
        }
        if (StrUtil.isNotBlank(startTime)) {
            wrapper.ge(VideoRecords::getStartTime, startTime);
        }
        if (StrUtil.isNotBlank(endTime)) {
            wrapper.le(VideoRecords::getStartTime, endTime);
        }
        if (StrUtil.isNotBlank(conf)) {
            wrapper.ge(VideoRecords::getConf, conf);
        }
        return videoRecordsMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<VideoRecords> findAll() {
        return videoRecordsMapper.selectList(null);
    }

    public VideoRecords findById(int id) {
        return videoRecordsMapper.selectById(id);
    }

    public void deleteById(int id) {
        videoRecordsMapper.deleteById(id);
    }

    public void update(VideoRecords videoRecords) {
        videoRecordsMapper.updateById(videoRecords);
    }

    public void save(VideoRecords videoRecords) {
        videoRecordsMapper.insert(videoRecords);
    }
}
