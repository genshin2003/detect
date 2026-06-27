package com.example.Kcsj.service;
import com.alibaba.excel.EasyExcel;
import com.example.Kcsj.entity.ImgRecords;
import com.example.Kcsj.entity.VideoRecords;
import com.example.Kcsj.entity.CameraRecords;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class ExcelExportService {

    public byte[] generateImgRecordsExcel(List<ImgRecords> records) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<List<String>> data = new ArrayList<>();
        for (ImgRecords r : records) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(r.getId()));
            row.add(r.getUsername() != null ? r.getUsername() : "");
            row.add(r.getStartTime() != null ? r.getStartTime() : "");
            row.add(r.getLabel() != null ? r.getLabel() : "");
            row.add(r.getConfidence() != null ? r.getConfidence() : "");
            row.add(r.getWeight() != null ? r.getWeight() : "");
            row.add(r.getConf() != null ? r.getConf() : "");
            row.add(r.getAi() != null ? r.getAi() : "");
            row.add(r.getSuggestion() != null ? r.getSuggestion() : "");
            row.add(r.getAllTime() != null ? r.getAllTime() : "");
            data.add(row);
        }

        List<List<String>> head = new ArrayList<>();
        head.add(Collections.singletonList("ID"));
        head.add(Collections.singletonList("用户名"));
        head.add(Collections.singletonList("检测时间"));
        head.add(Collections.singletonList("识别结果"));
        head.add(Collections.singletonList("置信度"));
        head.add(Collections.singletonList("使用模型"));
        head.add(Collections.singletonList("最小阈值"));
        head.add(Collections.singletonList("AI助手"));
        head.add(Collections.singletonList("AI建议"));
        head.add(Collections.singletonList("总耗时"));

        EasyExcel.write(baos).head(head).sheet("图片检测记录").doWrite(data);
        return baos.toByteArray();
    }

    public byte[] generateVideoRecordsExcel(List<VideoRecords> records) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<List<String>> data = new ArrayList<>();
        for (VideoRecords r : records) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(r.getId()));
            row.add(r.getUsername() != null ? r.getUsername() : "");
            row.add(r.getStartTime() != null ? r.getStartTime() : "");
            row.add(r.getWeight() != null ? r.getWeight() : "");
            row.add(r.getConf() != null ? r.getConf() : "");
            row.add(r.getInputVideo() != null ? r.getInputVideo() : "");
            row.add(r.getOutVideo() != null ? r.getOutVideo() : "");
            data.add(row);
        }

        List<List<String>> head = new ArrayList<>();
        head.add(Collections.singletonList("ID"));
        head.add(Collections.singletonList("用户名"));
        head.add(Collections.singletonList("检测时间"));
        head.add(Collections.singletonList("使用模型"));
        head.add(Collections.singletonList("最小阈值"));
        head.add(Collections.singletonList("原始视频"));
        head.add(Collections.singletonList("处理结果"));

        EasyExcel.write(baos).head(head).sheet("视频检测记录").doWrite(data);
        return baos.toByteArray();
    }

    public byte[] generateCameraRecordsExcel(List<CameraRecords> records) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<List<String>> data = new ArrayList<>();
        for (CameraRecords r : records) {
            List<String> row = new ArrayList<>();
            row.add(String.valueOf(r.getId()));
            row.add(r.getUsername() != null ? r.getUsername() : "");
            row.add(r.getStartTime() != null ? r.getStartTime() : "");
            row.add(r.getWeight() != null ? r.getWeight() : "");
            row.add(r.getConf() != null ? r.getConf() : "");
            row.add(r.getOutVideo() != null ? r.getOutVideo() : "");
            data.add(row);
        }

        List<List<String>> head = new ArrayList<>();
        head.add(Collections.singletonList("ID"));
        head.add(Collections.singletonList("用户名"));
        head.add(Collections.singletonList("检测时间"));
        head.add(Collections.singletonList("使用模型"));
        head.add(Collections.singletonList("最小阈值"));
        head.add(Collections.singletonList("处理结果"));

        EasyExcel.write(baos).head(head).sheet("摄像头检测记录").doWrite(data);
        return baos.toByteArray();
    }
}
