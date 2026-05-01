package com.example.Kcsj.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.ImgRecords;
import com.example.Kcsj.mapper.ImgRecordsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import javax.annotation.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/flask")
public class PredictionController {
    @Resource
    ImgRecordsMapper imgRecordsMapper;

    @Autowired
    FileController fileController;
    private final RestTemplate restTemplate = new RestTemplate();

    // 定义接收的参数类
    public static class PredictRequest {
        private String startTime;
        private String weight;
        private String username;
        private String inputImg;
        private String conf;
        private String ai;
        private String suggestion;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getWeight() {
            return weight;
        }

        public void setWeight(String weight) {
            this.weight = weight;
        }

        public String getInputImg() {
            return inputImg;
        }

        public void setInputImg(String inputImg) {
            this.inputImg = inputImg;
        }

        public String getConf() {
            return conf;
        }

        public void setConf(String conf) {
            this.conf = conf;
        }

        public String getAi() {
            return ai;
        }

        public void setAi(String ai) {
            this.ai = ai;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }

    @PostMapping("/predict")
    public Result<?> predict(@RequestBody PredictRequest request) {

        log.info("当前在/predict");
        if (request == null || request.getInputImg() == null || request.getInputImg().isEmpty()) {
            return Result.error("-1", "未提供图片链接");
        } else if (request.getWeight() == null || request.getWeight().isEmpty()) {
            return Result.error("-1", "未提供权重");
        }
        String imgUrl = request.getInputImg().toLowerCase();

        if (!(imgUrl.endsWith(".jpg") || imgUrl.endsWith(".jpeg") || imgUrl.endsWith(".png"))) {
            return Result.error("-1", "仅支持图片格式（jpg/png/jpeg），不支持该文件！");
        }

        try {
            // 创建请求体
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PredictRequest> requestEntity = new HttpEntity<>(request, headers);

            // 调用 Flask API
            String response = restTemplate.postForObject("http://localhost:5000/predictImg", requestEntity, String.class);
//            System.out.println("Received response: " + response);
            log.info(response);
            JSONObject responses = JSONObject.parseObject(response);
            if(responses.get("status").equals(400)){
                return Result.error("-1", "Error: " + responses.get("message"));
            }else {
                ImgRecords imgRecords = new ImgRecords();
                imgRecords.setWeight(request.getWeight());
                imgRecords.setConf(request.getConf());
                imgRecords.setInputImg(request.getInputImg());
                imgRecords.setUsername(request.getUsername());
                imgRecords.setStartTime(request.getStartTime());
                imgRecords.setAi(request.getAi());
                imgRecords.setLable(String.valueOf(responses.get("label")));
                imgRecords.setConfidence(String.valueOf(responses.get("confidence")));
                imgRecords.setAllTime(String.valueOf(responses.get("allTime")));
                imgRecords.setOutImg(String.valueOf(responses.get("outImg")));
                imgRecords.setSuggestion(String.valueOf(responses.get("suggestion")));
                imgRecordsMapper.insert(imgRecords); // 插入到数据库
                return Result.success(response);
            }
        } catch (Exception e) {
            return Result.error("-1", "Error: " + e.getMessage());
        }
    }

    @GetMapping("/file_names")
    public Result<?> getFileNames() {
        try {
            // 调用 Flask API
            String response = restTemplate.getForObject("http://127.0.0.1:5000/file_names", String.class);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("-1", "Error: " + e.getMessage());
        }
    }

    @PostMapping("/predictImgBatch")
    public Result<?> predictImgBatch(
            @RequestParam("username") String username,
            @RequestParam("weight") String weight,
            @RequestParam("conf") String conf,
            @RequestParam("images") MultipartFile[] images,
            @RequestParam("ai") String ai) {

        long startTime = System.currentTimeMillis();
        log.info("开始处理批量识别转发：用户={}, 权重={}, 图片数={}", username, weight, images.length);
        try {
            // 1. 缓存原图字节与原始信息
            List<byte[]> originalBytesList = new ArrayList<>();
            List<String> originalFilenames = new ArrayList<>();
            List<String> originalContentTypes = new ArrayList<>();

            for (MultipartFile file : images) {
                originalBytesList.add(file.getBytes());
                originalFilenames.add(file.getOriginalFilename());
                originalContentTypes.add(file.getContentType());
            }

            // 2. 转发给 Flask（使用 ByteArrayResource）
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("username", username);
            body.add("weight", weight);
            body.add("conf", conf);
            body.add("ai", ai);

            for (int i = 0; i < images.length; i++) {
                final int idx = i;
                ByteArrayResource resource = new ByteArrayResource(originalBytesList.get(i)) {
                    @Override
                    public String getFilename() {
                        return originalFilenames.get(idx);
                    }
                };
                body.add("images", resource);
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String flaskUrl = "http://localhost:5000/predictImgBatch";
            String response = restTemplate.postForObject(flaskUrl, requestEntity, String.class);
            JSONObject flaskJson = JSONObject.parseObject(response);

            if (flaskJson.getInteger("code") != 0) {
                return Result.error("-1", flaskJson.getString("message"));
            }
            JSONArray data = flaskJson.getJSONArray("data");
            log.info("Flask 返回的 data 数组长度: {}", data.size());

            // 3. 逐张处理
            for (int i = 0; i < data.size(); i++) {
                JSONObject item = data.getJSONObject(i);

                // 清理文件名中的路径
                String rawName = originalFilenames.get(i);
                String cleanName = rawName;
                if (cleanName != null) {
                    if (cleanName.contains("/")) {
                        cleanName = cleanName.substring(cleanName.lastIndexOf("/") + 1);
                    }
                    if (cleanName.contains("\\")) {
                        cleanName = cleanName.substring(cleanName.lastIndexOf("\\") + 1);
                    }
                }

                // 原图上传（使用清理后的文件名）
                MultipartFile originalMultipart = new MockMultipartFile(
                        "file",
                        cleanName,
                        originalContentTypes.get(i),
                        originalBytesList.get(i)
                );
                Result<?> uploadResult = fileController.upload(originalMultipart);
                String inputImgUrl = uploadResult.getData().toString();

                // 结果图上传
                String base64Image = item.getString("outImgBase64");
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                MultipartFile resultMultipart = new MockMultipartFile(
                        "batch_" + i + ".jpg",
                        "batch_" + i + ".jpg",
                        "image/jpeg",
                        imageBytes
                );
                Result<?> outUpload = fileController.upload(resultMultipart);
                String outImgUrl = outUpload.getData().toString();

                // 数据库记录
                ImgRecords record = new ImgRecords();
                record.setInputImg(inputImgUrl);
                record.setOutImg(outImgUrl);
                record.setAi(ai);
                String suggestion = item.getString("suggestion");
                if (suggestion == null || suggestion.isEmpty()) {
                    suggestion = "未选择AI，无AI建议！";
                }
                record.setSuggestion(suggestion);

                Object labelObj = item.get("label");
                String labelStr = (labelObj instanceof JSONArray) ? ((JSONArray) labelObj).toJSONString() : String.valueOf(labelObj);
                record.setLable(labelStr);

                Object confidenceObj = item.get("confidence");
                String confidenceStr = (confidenceObj instanceof JSONArray) ? ((JSONArray) confidenceObj).toJSONString() : String.valueOf(confidenceObj);
                record.setConfidence(confidenceStr);

                record.setAllTime(item.getString("allTime"));
                record.setStartTime(item.getString("startTime"));
                record.setUsername(username);
                record.setWeight(weight);
                record.setConf(conf);

                imgRecordsMapper.insert(record);

                item.put("inputImg", inputImgUrl);
                item.put("outImg", outImgUrl);
            }
            Long end_time=System.currentTimeMillis();
            log.info(end_time-startTime+"ms");
            return Result.success(flaskJson.get("data"));
        } catch (Exception e) {
            log.error("批量识别中转失败", e);
            return Result.error("-1", "中转异常: " + e.getMessage());
        }
    }
}
