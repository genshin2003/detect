package com.example.Kcsj.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.entity.ImgRecords;
import com.example.Kcsj.mapper.ImgRecordsMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class ImgRecordsService {
    @Resource
    ImgRecordsMapper imgRecordsMapper;

    public Page<ImgRecords> findPage(Integer pageNum, Integer pageSize, String username,
                                     String startTime, String endTime, String label, String conf) {
        LambdaQueryWrapper<ImgRecords> wrapper = Wrappers.<ImgRecords>lambdaQuery();
        wrapper.orderByDesc(ImgRecords::getStartTime);
        if (StrUtil.isNotBlank(username)) {
            wrapper.like(ImgRecords::getUsername, username);
        }
        if (StrUtil.isNotBlank(label)) {
            wrapper.like(ImgRecords::getLabel, label);
        }
        if (StrUtil.isNotBlank(conf)) {
            wrapper.ge(ImgRecords::getConf, conf);
        }
        if (StrUtil.isNotBlank(startTime)) {
            wrapper.ge(ImgRecords::getStartTime, startTime);
        }
        if (StrUtil.isNotBlank(endTime)) {
            wrapper.le(ImgRecords::getStartTime, endTime);
        }
        return imgRecordsMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<ImgRecords> findAll() {
        return imgRecordsMapper.selectList(null);
    }

    public ImgRecords findById(int id) {
        return imgRecordsMapper.selectById(id);
    }

    public void deleteById(int id) {
        imgRecordsMapper.deleteById(id);
    }

    public void update(ImgRecords imgRecords) {
        imgRecordsMapper.updateById(imgRecords);
    }

    public void save(ImgRecords imgRecords) {
        imgRecordsMapper.insert(imgRecords);
    }
}
