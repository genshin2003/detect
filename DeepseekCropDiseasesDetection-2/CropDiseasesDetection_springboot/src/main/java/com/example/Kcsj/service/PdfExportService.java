package com.example.Kcsj.service;

import com.example.Kcsj.entity.ImgRecords;
import com.example.Kcsj.entity.VideoRecords;
import com.example.Kcsj.entity.CameraRecords;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class PdfExportService {

    /**
     * 从文件系统加载图片，避免通过HTTP请求（中文文件名编码问题）
     * 支持完整URL（http://host:port/files/xxx.jpg）和相对路径（/files/xxx.jpg）
     */
    private byte[] loadImageFromDisk(String imageUrl) {
        try {
            String filename;
            if (imageUrl.contains("/files/")) {
                // 提取 /files/ 后面的文件名部分
                filename = imageUrl.substring(imageUrl.indexOf("/files/") + 7);
            } else {
                filename = imageUrl;
            }
            // URL解码中文字符
            filename = URLDecoder.decode(filename, StandardCharsets.UTF_8.name());
            String filePath = System.getProperty("user.dir") + "/files/" + filename;
            java.io.File file = new java.io.File(filePath);
            if (file.exists()) {
                try (InputStream is = new FileInputStream(file)) {
                    return is.readAllBytes();
                }
            } else {
                log.warn("文件不存在: {}", filePath);
            }
        } catch (Exception e) {
            log.warn("从磁盘加载图片失败: {}", imageUrl, e);
        }
        return null;
    }

    private PdfFont getChineseFont() {
        try {
            // 尝试使用系统字体
            String[] fontPaths = {
                    "C:/Windows/Fonts/simsun.ttc",
                    "C:/Windows/Fonts/simhei.ttf",
                    "C:/Windows/Fonts/msyh.ttc",
                    "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc"
            };
            for (String path : fontPaths) {
                try {
                    return PdfFontFactory.createFont(path, PdfEncodings.IDENTITY_H);
                } catch (Exception ignored) {
                }
            }
            // 兜底：使用iText内置字体（不支持中文，但不会报错）
            return PdfFontFactory.createFont("Helvetica");
        } catch (IOException e) {
            log.error("加载中文字体失败", e);
            return null;
        }
    }

    public byte[] generateImgRecordPdf(ImgRecords record) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            PdfFont font = getChineseFont();

            // 标题
            Paragraph title = new Paragraph("农作物病害检测报告")
                    .setFont(font).setFontSize(20).setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(title);

            // 基本信息表格
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70})).useAllAvailableWidth();
            infoTable.addCell(new Cell().add(new Paragraph("检测用户").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph(record.getUsername() != null ? record.getUsername() : "-").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph("检测时间").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph(record.getStartTime() != null ? record.getStartTime() : "-").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph("使用模型").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph(record.getWeight() != null ? record.getWeight() : "-").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph("最小阈值").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph(record.getConf() != null ? record.getConf() : "-").setFont(font).setFontSize(11)));
            infoTable.setMarginBottom(15);
            document.add(infoTable);

            // 检测结果
            Paragraph resultTitle = new Paragraph("检测结果").setFont(font).setFontSize(14).setMarginBottom(10);
            document.add(resultTitle);

            Table resultTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
            resultTable.addCell(new Cell().add(new Paragraph("识别结果").setFont(font).setFontSize(11)));
            resultTable.addCell(new Cell().add(new Paragraph("置信度").setFont(font).setFontSize(11)));
            resultTable.addCell(new Cell().add(new Paragraph(record.getLabel() != null ? record.getLabel() : "-").setFont(font).setFontSize(11)));
            resultTable.addCell(new Cell().add(new Paragraph(record.getConfidence() != null ? record.getConfidence() : "-").setFont(font).setFontSize(11)));
            resultTable.addCell(new Cell().add(new Paragraph("总耗时").setFont(font).setFontSize(11)));
            resultTable.addCell(new Cell().add(new Paragraph(record.getAllTime() != null ? record.getAllTime() : "-").setFont(font).setFontSize(11)));
            resultTable.setMarginBottom(15);
            document.add(resultTable);

            // 检测图片 - 优先从文件系统加载，避免中文URL编码问题
            if (record.getInputImg() != null && !record.getInputImg().isEmpty()) {
                document.add(new Paragraph("原始图片").setFont(font).setFontSize(14).setMarginBottom(5));
                try {
                    byte[] imgBytes = loadImageFromDisk(record.getInputImg());
                    com.itextpdf.io.image.ImageData imgData;
                    if (imgBytes != null) {
                        imgData = com.itextpdf.io.image.ImageDataFactory.create(imgBytes);
                    } else {
                        imgData = com.itextpdf.io.image.ImageDataFactory.create(new URL(record.getInputImg()));
                    }
                    Image inputImg = new Image(imgData);
                    inputImg.scaleToFit(400, 300).setMarginBottom(10);
                    document.add(inputImg);
                } catch (Exception e) {
                    log.warn("原始图片加载失败: {}", record.getInputImg(), e);
                    document.add(new Paragraph("图片加载失败").setFont(font).setFontSize(10));
                }
            }

            if (record.getOutImg() != null && !record.getOutImg().isEmpty()) {
                document.add(new Paragraph("检测结果图片").setFont(font).setFontSize(14).setMarginBottom(5));
                try {
                    byte[] imgBytes = loadImageFromDisk(record.getOutImg());
                    com.itextpdf.io.image.ImageData imgData;
                    if (imgBytes != null) {
                        imgData = com.itextpdf.io.image.ImageDataFactory.create(imgBytes);
                    } else {
                        imgData = com.itextpdf.io.image.ImageDataFactory.create(new URL(record.getOutImg()));
                    }
                    Image outImg = new Image(imgData);
                    outImg.scaleToFit(400, 300).setMarginBottom(10);
                    document.add(outImg);
                } catch (Exception e) {
                    log.warn("检测结果图片加载失败: {}", record.getOutImg(), e);
                    document.add(new Paragraph("图片加载失败").setFont(font).setFontSize(10));
                }
            }

            // AI建议
            if (record.getSuggestion() != null && !record.getSuggestion().isEmpty()) {
                document.add(new Paragraph("AI建议").setFont(font).setFontSize(14).setMarginBottom(5));
                document.add(new Paragraph(record.getSuggestion()).setFont(font).setFontSize(11).setMarginBottom(10));
            }

            // 页脚
            document.add(new Paragraph("报告生成时间: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setFont(font).setFontSize(9).setTextAlignment(TextAlignment.RIGHT).setMarginTop(20));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("生成PDF失败", e);
            return null;
        }
    }

    public byte[] generateVideoRecordPdf(VideoRecords record) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            PdfFont font = getChineseFont();

            document.add(new Paragraph("视频检测报告").setFont(font).setFontSize(20).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70})).useAllAvailableWidth();
            infoTable.addCell(new Cell().add(new Paragraph("检测用户").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph(record.getUsername() != null ? record.getUsername() : "-").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph("检测时间").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph(record.getStartTime() != null ? record.getStartTime() : "-").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph("使用模型").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph(record.getWeight() != null ? record.getWeight() : "-").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph("最小阈值").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph(record.getConf() != null ? record.getConf() : "-").setFont(font).setFontSize(11)));
            infoTable.setMarginBottom(15);
            document.add(infoTable);

            if (record.getInputVideo() != null && !record.getInputVideo().isEmpty()) {
                document.add(new Paragraph("原始视频地址: " + record.getInputVideo()).setFont(font).setFontSize(11).setMarginBottom(10));
            }
            if (record.getOutVideo() != null && !record.getOutVideo().isEmpty()) {
                document.add(new Paragraph("处理结果视频地址: " + record.getOutVideo()).setFont(font).setFontSize(11).setMarginBottom(10));
            }

            document.add(new Paragraph("报告生成时间: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setFont(font).setFontSize(9).setTextAlignment(TextAlignment.RIGHT).setMarginTop(20));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("生成视频PDF失败", e);
            return null;
        }
    }

    public byte[] generateCameraRecordPdf(CameraRecords record) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            PdfFont font = getChineseFont();

            document.add(new Paragraph("摄像头检测报告").setFont(font).setFontSize(20).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70})).useAllAvailableWidth();
            infoTable.addCell(new Cell().add(new Paragraph("检测用户").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph(record.getUsername() != null ? record.getUsername() : "-").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph("检测时间").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph(record.getStartTime() != null ? record.getStartTime() : "-").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph("使用模型").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph(record.getWeight() != null ? record.getWeight() : "-").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph("最小阈值").setFont(font).setFontSize(11)));
            infoTable.addCell(new Cell().add(new Paragraph(record.getConf() != null ? record.getConf() : "-").setFont(font).setFontSize(11)));
            infoTable.setMarginBottom(15);
            document.add(infoTable);

            if (record.getOutVideo() != null && !record.getOutVideo().isEmpty()) {
                document.add(new Paragraph("处理结果视频地址: " + record.getOutVideo()).setFont(font).setFontSize(11).setMarginBottom(10));
            }

            document.add(new Paragraph("报告生成时间: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setFont(font).setFontSize(9).setTextAlignment(TextAlignment.RIGHT).setMarginTop(20));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("生成摄像头PDF失败", e);
            return null;
        }
    }
}
