package com.example.Kcsj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.CameraRecords;
import com.example.Kcsj.service.CameraRecordsService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/cameraRecords")
public class CameraRecordsController {
    @Resource
    CameraRecordsService cameraRecordsService;

    @GetMapping("/all")
    public Result<?> findAll() {
        return Result.success(cameraRecordsService.findAll());
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable("id") int id) {
        return Result.success(cameraRecordsService.findById(id));
    }

    @GetMapping
    public Result<?> findPage(@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
                              @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                              @RequestParam(name = "username", defaultValue = "") String username,
                              @RequestParam(name = "startTime", defaultValue = "") String startTime,
                              @RequestParam(name = "endTime", defaultValue = "") String endTime,
                              @RequestParam(name = "conf", defaultValue = "") String conf) {
        Page<CameraRecords> page = cameraRecordsService.findPage(pageNum, pageSize, username, startTime, endTime, conf);
        return Result.success(page);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable("id") int id) {
        cameraRecordsService.deleteById(id);
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody CameraRecords cameraRecords) {
        cameraRecordsService.update(cameraRecords);
        return Result.success();
    }

    @PostMapping
    public Result<?> save(@RequestBody CameraRecords cameraRecords) {
        cameraRecordsService.save(cameraRecords);
        return Result.success();
    }
}
