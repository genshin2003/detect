package com.example.Kcsj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.ImgRecords;
import com.example.Kcsj.service.ImgRecordsService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/imgRecords")
public class ImgRecordsController {
    @Resource
    ImgRecordsService imgRecordsService;

    @GetMapping("/all")
    public Result<?> findAll() {
        return Result.success(imgRecordsService.findAll());
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable("id") int id) {
        return Result.success(imgRecordsService.findById(id));
    }

    @GetMapping
    public Result<?> findPage(@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
                              @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                              @RequestParam(name = "username", defaultValue = "") String username,
                              @RequestParam(name = "startTime", defaultValue = "") String startTime,
                              @RequestParam(name = "endTime", defaultValue = "") String endTime,
                              @RequestParam(name = "label", defaultValue = "") String label,
                              @RequestParam(name = "conf", defaultValue = "") String conf) {
        Page<ImgRecords> page = imgRecordsService.findPage(pageNum, pageSize, username, startTime, endTime, label, conf);
        return Result.success(page);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable("id") int id) {
        imgRecordsService.deleteById(id);
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody ImgRecords imgRecords) {
        imgRecordsService.update(imgRecords);
        return Result.success();
    }

    @PostMapping
    public Result<?> save(@RequestBody ImgRecords imgRecords) {
        imgRecordsService.save(imgRecords);
        return Result.success();
    }
}
