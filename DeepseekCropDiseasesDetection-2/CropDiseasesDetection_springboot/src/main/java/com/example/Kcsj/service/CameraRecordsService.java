package com.example.Kcsj.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.entity.CameraRecords;
import com.example.Kcsj.mapper.CameraRecordsMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class CameraRecordsService {
    @Resource
    CameraRecordsMapper cameraRecordsMapper;

    public Page<CameraRecords> findPage(Integer pageNum, Integer pageSize, String username,
                                        String startTime, String endTime, String conf) {
        LambdaQueryWrapper<CameraRecords> wrapper = Wrappers.<CameraRecords>lambdaQuery();
        wrapper.orderByDesc(CameraRecords::getStartTime);
        if (StrUtil.isNotBlank(username)) {
            wrapper.like(CameraRecords::getUsername, username);
        }
        if (StrUtil.isNotBlank(startTime)) {
            wrapper.ge(CameraRecords::getStartTime, startTime);
        }
        if (StrUtil.isNotBlank(endTime)) {
            wrapper.le(CameraRecords::getStartTime, endTime);
        }
        if (StrUtil.isNotBlank(conf)) {
            wrapper.ge(CameraRecords::getConf, conf);
        }
        return cameraRecordsMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public java.util.List<CameraRecords> findAll() {
        return cameraRecordsMapper.selectList(null);
    }

    public CameraRecords findById(int id) {
        return cameraRecordsMapper.selectById(id);
    }

    public void deleteById(int id) {
        cameraRecordsMapper.deleteById(id);
    }

    public void update(CameraRecords cameraRecords) {
        cameraRecordsMapper.updateById(cameraRecords);
    }

    public void save(CameraRecords cameraRecords) {
        cameraRecordsMapper.insert(cameraRecords);
    }
}
