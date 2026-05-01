package com.example.Kcsj.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.VideoRecords;
import com.example.Kcsj.mapper.VideoRecordsMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/videoRecords")
public class VideoRecordsController {
    @Resource
    VideoRecordsMapper videoRecordsMapper;

    @GetMapping("/all")
    public Result<?> GetAll() {
        return Result.success(videoRecordsMapper.selectList(null));
    }
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable int id) {
        System.out.println(id);
        return Result.success(videoRecordsMapper.selectById(id));
    }

    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search,  // 用户名
                              @RequestParam(defaultValue = "") String search1, // 开始时间
                              @RequestParam(defaultValue = "") String search2, // 结束时间
                              @RequestParam(defaultValue = "") String search3) // 最低阈值
    {
        LambdaQueryWrapper<VideoRecords> wrapper = Wrappers.<VideoRecords>lambdaQuery();

        // 1. 按时间倒序排序
        wrapper.orderByDesc(VideoRecords::getStartTime);

        // 2. 搜索用户名 (search)
        if (StrUtil.isNotBlank(search)) {
            wrapper.like(VideoRecords::getUsername, search);
        }

        // 3. 处理时间范围 (关键修改：作用于同一个字段 StartTime)
        // 如果有开始时间，查询 StartTime >= search1
        if (StrUtil.isNotBlank(search1)) {
            wrapper.ge(VideoRecords::getStartTime, search1);
        }
        // 如果有结束时间，查询 StartTime <= search2
        if (StrUtil.isNotBlank(search2)) {
            wrapper.le(VideoRecords::getStartTime, search2);
        }

        // 4. 最低阈值查询 (search3 -> Conf)
        if (StrUtil.isNotBlank(search3)) {
            wrapper.ge(VideoRecords::getConf, search3); // 阈值通常是大于等于
        }

        Page<VideoRecords> page = videoRecordsMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(page);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable int id) {
        videoRecordsMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<?> updates(@RequestBody VideoRecords videoRecords) {
        videoRecordsMapper.updateById(videoRecords);
        return Result.success();
    }


    @PostMapping
    public Result<?> save(@RequestBody VideoRecords videoRecords) {
        System.out.println(videoRecords);
        videoRecordsMapper.insert(videoRecords);
        return Result.success();
    }
}
