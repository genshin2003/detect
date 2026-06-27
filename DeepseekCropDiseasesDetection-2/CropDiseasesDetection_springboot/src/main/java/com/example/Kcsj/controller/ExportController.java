package com.example.Kcsj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.CameraRecords;
import com.example.Kcsj.entity.ImgRecords;
import com.example.Kcsj.entity.VideoRecords;
import com.example.Kcsj.service.CameraRecordsService;
import com.example.Kcsj.service.ExcelExportService;
import com.example.Kcsj.service.ImgRecordsService;
import com.example.Kcsj.service.PdfExportService;
import com.example.Kcsj.service.VideoRecordsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/export")
public class ExportController {
    @Resource
    PdfExportService pdfExportService;
    @Resource
    ExcelExportService excelExportService;
    @Resource
    ImgRecordsService imgRecordsService;
    @Resource
    VideoRecordsService videoRecordsService;
    @Resource
    CameraRecordsService cameraRecordsService;

    @GetMapping("/pdf/img/{id}")
    public void exportImgPdf(@PathVariable("id") int id, HttpServletResponse response) {
        ImgRecords record = imgRecordsService.findById(id);
        if (record == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        byte[] pdf = pdfExportService.generateImgRecordPdf(record);
        downloadFile(response, pdf, "图片检测报告_" + id + ".pdf");
    }

    @GetMapping("/pdf/video/{id}")
    public void exportVideoPdf(@PathVariable("id") int id, HttpServletResponse response) {
        VideoRecords record = videoRecordsService.findById(id);
        if (record == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        byte[] pdf = pdfExportService.generateVideoRecordPdf(record);
        downloadFile(response, pdf, "视频检测报告_" + id + ".pdf");
    }

    @GetMapping("/pdf/camera/{id}")
    public void exportCameraPdf(@PathVariable("id") int id, HttpServletResponse response) {
        CameraRecords record = cameraRecordsService.findById(id);
        if (record == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        byte[] pdf = pdfExportService.generateCameraRecordPdf(record);
        downloadFile(response, pdf, "摄像头检测报告_" + id + ".pdf");
    }

    @GetMapping("/excel/img")
    public void exportImgExcel(@RequestParam(name = "username", defaultValue = "") String username,
                               @RequestParam(name = "startTime", defaultValue = "") String startTime,
                               @RequestParam(name = "endTime", defaultValue = "") String endTime,
                               HttpServletResponse response) {
        Page<ImgRecords> page = imgRecordsService.findPage(1, 10000, username, startTime, endTime, "", "");
        byte[] excel = excelExportService.generateImgRecordsExcel(page.getRecords());
        downloadFile(response, excel, "图片检测记录.xlsx");
    }

    @GetMapping("/excel/video")
    public void exportVideoExcel(@RequestParam(name = "username", defaultValue = "") String username,
                                 @RequestParam(name = "startTime", defaultValue = "") String startTime,
                                 @RequestParam(name = "endTime", defaultValue = "") String endTime,
                                 HttpServletResponse response) {
        Page<VideoRecords> page = videoRecordsService.findPage(1, 10000, username, startTime, endTime, "");
        byte[] excel = excelExportService.generateVideoRecordsExcel(page.getRecords());
        downloadFile(response, excel, "视频检测记录.xlsx");
    }

    @GetMapping("/excel/camera")
    public void exportCameraExcel(@RequestParam(name = "username", defaultValue = "") String username,
                                  @RequestParam(name = "startTime", defaultValue = "") String startTime,
                                  @RequestParam(name = "endTime", defaultValue = "") String endTime,
                                  HttpServletResponse response) {
        Page<CameraRecords> page = cameraRecordsService.findPage(1, 10000, username, startTime, endTime, "");
        byte[] excel = excelExportService.generateCameraRecordsExcel(page.getRecords());
        downloadFile(response, excel, "摄像头检测记录.xlsx");
    }

    private void downloadFile(HttpServletResponse response, byte[] data, String filename) {
        try {
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"));
            response.setContentLength(data.length);
            response.getOutputStream().write(data);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("文件下载失败", e);
        }
    }
}
