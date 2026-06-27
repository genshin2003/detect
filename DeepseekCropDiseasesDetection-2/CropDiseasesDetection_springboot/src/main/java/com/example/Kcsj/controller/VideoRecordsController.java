package com.example.Kcsj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.VideoRecords;
import com.example.Kcsj.service.VideoRecordsService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/videoRecords")
public class VideoRecordsController {
    @Resource
    VideoRecordsService videoRecordsService;

    @GetMapping("/all")
    public Result<?> findAll() {
        return Result.success(videoRecordsService.findAll());
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable("id") int id) {
        return Result.success(videoRecordsService.findById(id));
    }

    @GetMapping
    public Result<?> findPage(@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
                              @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                              @RequestParam(name = "username", defaultValue = "") String username,
                              @RequestParam(name = "startTime", defaultValue = "") String startTime,
                              @RequestParam(name = "endTime", defaultValue = "") String endTime,
                              @RequestParam(name = "conf", defaultValue = "") String conf) {
        Page<VideoRecords> page = videoRecordsService.findPage(pageNum, pageSize, username, startTime, endTime, conf);
        return Result.success(page);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable("id") int id) {
        videoRecordsService.deleteById(id);
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody VideoRecords videoRecords) {
        videoRecordsService.update(videoRecords);
        return Result.success();
    }

    @PostMapping
    public Result<?> save(@RequestBody VideoRecords videoRecords) {
        videoRecordsService.save(videoRecords);
        return Result.success();
    }
}
