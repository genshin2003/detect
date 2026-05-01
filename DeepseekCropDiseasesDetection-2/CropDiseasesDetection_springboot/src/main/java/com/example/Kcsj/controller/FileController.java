package com.example.Kcsj.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.example.Kcsj.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${server.port}")
    private String port;

    @Value("${file.ip}")
    private String ip;

    // ==================== 工具方法：根据文件名返回正确的 Content-Type ====================
    private String determineContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String name = filename.toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".bmp")) return "image/bmp";
        if (name.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    // ==================== 单文件上传（保持不变）===================
    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file) {
        log.info("接收到上传请求: {}", file != null ? file.getOriginalFilename() : "null");
        if (file == null || file.isEmpty()) {
            return Result.error("-1", "上传文件不能为空");
        }
        try {
            String originalFilename = file.getOriginalFilename();
            // ========== 关键修复：去除路径，只保留纯文件名 ==========
            if (originalFilename != null) {
                // 处理 Unix 风格路径分隔符 '/'
                if (originalFilename.contains("/")) {
                    originalFilename = originalFilename.substring(originalFilename.lastIndexOf("/") + 1);
                }
                // 处理 Windows 风格路径分隔符 '\'
                if (originalFilename.contains("\\")) {
                    originalFilename = originalFilename.substring(originalFilename.lastIndexOf("\\") + 1);
                }
            }
            // ====================================================
            if (originalFilename == null || originalFilename.isEmpty()) {
                originalFilename = "temp.jpg";
            }

            String flag = IdUtil.fastSimpleUUID();
            String rootFilePath = System.getProperty("user.dir") + "/files/" + flag + "_" + originalFilename;
            File saveFile = new File(rootFilePath);
            if (!saveFile.getParentFile().exists()) {
                saveFile.getParentFile().mkdirs();
            }
            file.transferTo(saveFile);

            // 调试日志（可选，问题解决后可移除）
            log.info("文件保存路径: {}, 大小: {} bytes", saveFile.getAbsolutePath(), saveFile.length());

            String safeIp = (ip == null) ? "localhost" : ip;
            String safePort = (port == null) ? "9999" : port;
            String fileUrl = "http://" + safeIp + ":" + safePort + "/files/" + flag;
            log.info("✅ 文件上传成功，URL: {}", fileUrl);
            return Result.success(fileUrl);
        } catch (Exception e) {
            log.error("文件上传系统异常", e);
            return Result.error("-1", "上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/uploadFolder")
    public Result<?> uploadFolder(MultipartFile folder) throws IOException {
        log.info("当前在/uploadFolder");
        if (folder == null || !folder.getOriginalFilename().endsWith(".zip")) {
            return Result.error("-1", "请上传有效的zip文件");
        }

        String flag = IdUtil.fastSimpleUUID();
        String basePath = System.getProperty("user.dir") + "/files/" + flag + "/";
        File baseDir = new File(basePath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        try (ZipInputStream zis = new ZipInputStream(folder.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    Path filePath = Paths.get(basePath, entry.getName());
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath);
                }
                zis.closeEntry();
            }
        }

        String folderUrl = "http://" + ip + ":" + port + "/files/" + flag;
        return Result.success(folderUrl);
    }


    // ==================== 【修复重点】动态获取文件（单文件 + 子文件夹）===================
    @GetMapping("/{flag}/{filename:.+}")
    public ResponseEntity<Resource> getFileFromFolder(
            @PathVariable String flag,
            @PathVariable String filename) {

        String basePath = System.getProperty("user.dir") + "/files/";
        String filePath = basePath + flag + "/" + filename;
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            log.warn("文件不存在: {}", filePath);
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String contentType = determineContentType(filename);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    // 主服务接口：同时支持单文件（动态查找）和文件夹打包下载
    @GetMapping("/{flag}")
    public void getFiles(@PathVariable String flag, HttpServletResponse response) {
        log.info("当前在 /files/{}", flag);
        String basePath = System.getProperty("user.dir") + "/files/";
        String folderPath = basePath + flag + "/";
        File folder = new File(folderPath);

        try {
            // 1. 是文件夹 → 打包下载
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles();
                if (files != null && files.length > 0) {
                    response.setContentType("application/zip");
                    response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(flag + ".zip", "UTF-8"));

                    try (OutputStream os = response.getOutputStream();
                         ZipOutputStream zos = new ZipOutputStream(os)) {
                        for (File file : files) {
                            if (file.isFile()) {
                                ZipEntry zipEntry = new ZipEntry(file.getName());
                                zos.putNextEntry(zipEntry);
                                byte[] bytes = FileUtil.readBytes(file);
                                zos.write(bytes);
                                zos.closeEntry();
                            }
                        }
                        zos.finish();
                    }
                    return;
                }
            }

            // 2. 单文件处理（精确匹配 + 动态前缀查找）
            String filePath = basePath + flag;
            File file = new File(filePath);

            if (file.exists() && file.isFile()) {
                // 精确匹配成功
                serveImage(file, response);
                return;
            }

            // 3. 动态查找：flag 可能是纯 UUID，找 flag_ 开头的文件（你原来的动态逻辑）
            List<String> fileNames = FileUtil.listFileNames(basePath);
            String matchedFile = fileNames.stream()
                    .filter(name -> name.startsWith(flag + "_"))
                    .findFirst()
                    .orElse("");

            if (StrUtil.isNotEmpty(matchedFile)) {
                file = new File(basePath + matchedFile);
                serveImage(file, response);
                return;
            }

            // 都没找到
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("File or folder not found: " + flag);
        } catch (IOException e) {
            log.error("文件服务异常", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // 抽取的公共方法：正确返回图片（inline + 正确 Content-Type）
    private void serveImage(File file, HttpServletResponse response) throws IOException {
        String contentType = determineContentType(file.getName());
        response.setContentType(contentType);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + URLEncoder.encode(file.getName(), "UTF-8") + "\"");

        byte[] bytes = FileUtil.readBytes(file);
        try (OutputStream os = response.getOutputStream()) {
            os.write(bytes);
            os.flush();
        }
        log.info("成功返回图片: {}  Content-Type: {}", file.getName(), contentType);
    }
}