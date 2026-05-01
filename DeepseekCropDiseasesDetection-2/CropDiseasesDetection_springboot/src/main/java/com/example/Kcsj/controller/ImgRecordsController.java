package com.example.Kcsj.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.ImgRecords;
import com.example.Kcsj.mapper.ImgRecordsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/imgRecords")
public class ImgRecordsController {
    @Resource
    ImgRecordsMapper imgRecordsMapper;

    @GetMapping("/all")
    public Result<?> GetAll() {
        return Result.success(imgRecordsMapper.selectList(null));
    }
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable int id) {
        System.out.println(id);
        return Result.success(imgRecordsMapper.selectById(id));
    }

    @GetMapping
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search,   // 用户名
                              @RequestParam(defaultValue = "") String search1,  // 开始时间
                              @RequestParam(defaultValue = "") String endTime,  // 结束时间 (新增入参)
                              @RequestParam(defaultValue = "") String search2,  // 识别结果 Label
                              @RequestParam(defaultValue = "") String search3) {// 最低阈值 Conf

        LambdaQueryWrapper<ImgRecords> wrapper = Wrappers.<ImgRecords>lambdaQuery();

        // 按时间倒序
        wrapper.orderByDesc(ImgRecords::getStartTime);

        // 用户名模糊查询
        if (StrUtil.isNotBlank(search)) {
            wrapper.like(ImgRecords::getUsername, search);
        }

        // 识别结果模糊查询
        if (StrUtil.isNotBlank(search2)) {
            wrapper.like(ImgRecords::getLabel, search2);
        }

        // 最小阈值查询 (根据业务逻辑，这里通常用 >= )
        if (StrUtil.isNotBlank(search3)) {
            wrapper.ge(ImgRecords::getConf, search3);
        }

        // ⭐ 重点修正：时间范围查询
        if (StrUtil.isNotBlank(search1)) {
            wrapper.ge(ImgRecords::getStartTime, search1); // >= 开始时间
        }
        if (StrUtil.isNotBlank(endTime)) {
            wrapper.le(ImgRecords::getStartTime, endTime); // <= 结束时间
        }

        Page<ImgRecords> page = imgRecordsMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return Result.success(page);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable int id) {
        imgRecordsMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<?> updates(@RequestBody ImgRecords imgrecords) {
        imgRecordsMapper.updateById(imgrecords);
        return Result.success();
    }


    @PostMapping
    public Result<?> save(@RequestBody ImgRecords imgrecords) {
//        System.out.println(imgrecords);
        imgRecordsMapper.insert(imgrecords);
        return Result.success();
    }
}
