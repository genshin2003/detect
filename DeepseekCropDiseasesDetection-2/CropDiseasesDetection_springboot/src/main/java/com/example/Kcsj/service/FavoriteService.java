package com.example.Kcsj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.entity.Favorite;
import com.example.Kcsj.entity.FavoriteFolder;
import com.example.Kcsj.entity.ImgRecords;
import com.example.Kcsj.entity.VideoRecords;
import com.example.Kcsj.entity.CameraRecords;
import com.example.Kcsj.mapper.FavoriteFolderMapper;
import com.example.Kcsj.mapper.FavoriteMapper;
import com.example.Kcsj.mapper.ImgRecordsMapper;
import com.example.Kcsj.mapper.VideoRecordsMapper;
import com.example.Kcsj.mapper.CameraRecordsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FavoriteService {
    @Resource
    FavoriteMapper favoriteMapper;
    @Resource
    FavoriteFolderMapper folderMapper;
    @Resource
    ImgRecordsMapper imgRecordsMapper;
    @Resource
    VideoRecordsMapper videoRecordsMapper;
    @Resource
    CameraRecordsMapper cameraRecordsMapper;

    // ========== 收藏夹操作 ==========

    public FavoriteFolder createFolder(FavoriteFolder folder) {
        folder.setCreateTime(new Date());
        if (folder.getIsPublic() == null) folder.setIsPublic(false);
        folderMapper.insert(folder);
        return folder;
    }

    public void updateFolder(FavoriteFolder folder) {
        folderMapper.updateById(folder);
    }

    public void deleteFolder(Integer folderId, String username) {
        // 找到默认收藏夹
        FavoriteFolder defaultFolder = getDefaultFolder(username);
        // 将该收藏夹下的收藏移到默认收藏夹
        LambdaQueryWrapper<Favorite> wrapper = Wrappers.<Favorite>lambdaQuery();
        wrapper.eq(Favorite::getFolderId, folderId);
        List<Favorite> favorites = favoriteMapper.selectList(wrapper);
        for (Favorite fav : favorites) {
            fav.setFolderId(defaultFolder.getId());
            favoriteMapper.updateById(fav);
        }
        folderMapper.deleteById(folderId);
    }

    public List<Map<String, Object>> getMyFolders(String username) {
        LambdaQueryWrapper<FavoriteFolder> wrapper = Wrappers.<FavoriteFolder>lambdaQuery();
        wrapper.eq(FavoriteFolder::getUsername, username).orderByDesc(FavoriteFolder::getCreateTime);
        List<FavoriteFolder> folders = folderMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (FavoriteFolder folder : folders) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", folder.getId());
            map.put("name", folder.getName());
            map.put("description", folder.getDescription());
            map.put("isPublic", folder.getIsPublic());
            map.put("createTime", folder.getCreateTime());
            // 统计收藏数量
            long count = favoriteMapper.selectCount(
                    Wrappers.<Favorite>lambdaQuery().eq(Favorite::getFolderId, folder.getId()));
            map.put("count", count);
            result.add(map);
        }
        return result;
    }

    public FavoriteFolder getDefaultFolder(String username) {
        LambdaQueryWrapper<FavoriteFolder> wrapper = Wrappers.<FavoriteFolder>lambdaQuery();
        wrapper.eq(FavoriteFolder::getUsername, username).eq(FavoriteFolder::getName, "默认收藏夹");
        FavoriteFolder folder = folderMapper.selectOne(wrapper);
        if (folder == null) {
            folder = FavoriteFolder.builder()
                    .username(username)
                    .name("默认收藏夹")
                    .description("系统自动创建的默认收藏夹")
                    .isPublic(false)
                    .createTime(new Date())
                    .build();
            folderMapper.insert(folder);
        }
        return folder;
    }

    // ========== 收藏操作 ==========

    public void addFavorite(Favorite favorite) {
        // 如果没有指定收藏夹，使用默认收藏夹
        if (favorite.getFolderId() == null) {
            FavoriteFolder defaultFolder = getDefaultFolder(favorite.getUsername());
            favorite.setFolderId(defaultFolder.getId());
        }
        if (favorite.getPriority() == null) favorite.setPriority("NORMAL");
        favorite.setCreateTime(new Date());
        favoriteMapper.insert(favorite);
    }

    public void removeFavorite(Integer id, String username) {
        LambdaQueryWrapper<Favorite> wrapper = Wrappers.<Favorite>lambdaQuery();
        wrapper.eq(Favorite::getId, id).eq(Favorite::getUsername, username);
        favoriteMapper.delete(wrapper);
    }

    public void updateFavorite(Favorite favorite) {
        favoriteMapper.updateById(favorite);
    }

    public Favorite checkFavorite(String username, Integer recordId, String recordType) {
        LambdaQueryWrapper<Favorite> wrapper = Wrappers.<Favorite>lambdaQuery();
        wrapper.eq(Favorite::getUsername, username)
                .eq(Favorite::getRecordId, recordId)
                .eq(Favorite::getRecordType, recordType);
        return favoriteMapper.selectOne(wrapper);
    }

    public Page<Map<String, Object>> findPage(Integer pageNum, Integer pageSize, String username,
                                    Integer folderId, String priority) {
        LambdaQueryWrapper<Favorite> wrapper = Wrappers.<Favorite>lambdaQuery();
        wrapper.eq(Favorite::getUsername, username);
        if (folderId != null) {
            wrapper.eq(Favorite::getFolderId, folderId);
        }
        if (priority != null && !priority.isEmpty()) {
            wrapper.eq(Favorite::getPriority, priority);
        }
        wrapper.orderByDesc(Favorite::getCreateTime);
        Page<Favorite> page = favoriteMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 收集各类recordId
        List<Integer> imgIds = new ArrayList<>();
        List<Integer> videoIds = new ArrayList<>();
        List<Integer> cameraIds = new ArrayList<>();
        for (Favorite fav : page.getRecords()) {
            switch (fav.getRecordType()) {
                case "IMG" -> imgIds.add(fav.getRecordId());
                case "VIDEO" -> videoIds.add(fav.getRecordId());
                case "CAMERA" -> cameraIds.add(fav.getRecordId());
            }
        }

        // 批量查询记录详情
        Map<Integer, ImgRecords> imgMap = new HashMap<>();
        Map<Integer, VideoRecords> videoMap = new HashMap<>();
        Map<Integer, CameraRecords> cameraMap = new HashMap<>();
        if (!imgIds.isEmpty()) {
            List<ImgRecords> list = imgRecordsMapper.selectBatchIds(imgIds);
            list.forEach(r -> imgMap.put(r.getId(), r));
        }
        if (!videoIds.isEmpty()) {
            List<VideoRecords> list = videoRecordsMapper.selectBatchIds(videoIds);
            list.forEach(r -> videoMap.put(r.getId(), r));
        }
        if (!cameraIds.isEmpty()) {
            List<CameraRecords> list = cameraRecordsMapper.selectBatchIds(cameraIds);
            list.forEach(r -> cameraMap.put(r.getId(), r));
        }

        // 组装返回数据
        List<Map<String, Object>> enrichedRecords = new ArrayList<>();
        for (Favorite fav : page.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", fav.getId());
            item.put("username", fav.getUsername());
            item.put("folderId", fav.getFolderId());
            item.put("recordId", fav.getRecordId());
            item.put("recordType", fav.getRecordType());
            item.put("remark", fav.getRemark());
            item.put("priority", fav.getPriority());
            item.put("createTime", fav.getCreateTime());

            // 根据类型填充记录详情
            switch (fav.getRecordType()) {
                case "IMG" -> {
                    ImgRecords r = imgMap.get(fav.getRecordId());
                    if (r != null) {
                        item.put("inputImg", r.getInputImg());
                        item.put("outImg", r.getOutImg());
                        item.put("label", parseFirstLabel(r.getLabel()));
                        item.put("confidence", parseFirstConfidence(r.getConfidence()));
                    }
                }
                case "VIDEO" -> {
                    VideoRecords r = videoMap.get(fav.getRecordId());
                    if (r != null) {
                        item.put("inputImg", r.getInputVideo());
                        item.put("outImg", r.getOutVideo());
                        item.put("label", null);
                        item.put("confidence", null);
                    }
                }
                case "CAMERA" -> {
                    CameraRecords r = cameraMap.get(fav.getRecordId());
                    if (r != null) {
                        item.put("inputImg", null);
                        item.put("outImg", r.getOutVideo());
                        item.put("label", null);
                        item.put("confidence", null);
                    }
                }
            }
            enrichedRecords.add(item);
        }

        Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(enrichedRecords);
        return result;
    }

    private String parseFirstLabel(String labelJson) {
        if (labelJson == null || labelJson.isEmpty()) return null;
        try {
            if (labelJson.startsWith("[")) {
                String cleaned = labelJson.replaceAll("[\\[\\]\"]", "");
                String[] parts = cleaned.split(",");
                return parts.length > 0 ? parts[0].trim() : null;
            }
            return labelJson;
        } catch (Exception e) {
            return labelJson;
        }
    }

    private String parseFirstConfidence(String confidenceJson) {
        if (confidenceJson == null || confidenceJson.isEmpty()) return null;
        try {
            if (confidenceJson.startsWith("[")) {
                String cleaned = confidenceJson.replaceAll("[\\[\\]\"]", "");
                String[] parts = cleaned.split(",");
                return parts.length > 0 ? parts[0].trim() : null;
            }
            return confidenceJson;
        } catch (Exception e) {
            return confidenceJson;
        }
    }

    public void batchRemove(List<Integer> ids, String username) {
        LambdaQueryWrapper<Favorite> wrapper = Wrappers.<Favorite>lambdaQuery();
        wrapper.in(Favorite::getId, ids).eq(Favorite::getUsername, username);
        favoriteMapper.delete(wrapper);
    }

    public void batchMove(List<Integer> ids, Integer targetFolderId, String username) {
        LambdaQueryWrapper<Favorite> wrapper = Wrappers.<Favorite>lambdaQuery();
        wrapper.in(Favorite::getId, ids).eq(Favorite::getUsername, username);
        List<Favorite> favorites = favoriteMapper.selectList(wrapper);
        for (Favorite fav : favorites) {
            fav.setFolderId(targetFolderId);
            favoriteMapper.updateById(fav);
        }
    }

    public Page<Map<String, Object>> getPublicFolders(Integer pageNum, Integer pageSize, String excludeUsername) {
        LambdaQueryWrapper<FavoriteFolder> wrapper = Wrappers.<FavoriteFolder>lambdaQuery();
        wrapper.eq(FavoriteFolder::getIsPublic, true);
        if (excludeUsername != null) {
            wrapper.ne(FavoriteFolder::getUsername, excludeUsername);
        }
        wrapper.orderByDesc(FavoriteFolder::getCreateTime);
        Page<FavoriteFolder> page = folderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (FavoriteFolder folder : page.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", folder.getId());
            map.put("name", folder.getName());
            map.put("description", folder.getDescription());
            map.put("username", folder.getUsername());
            map.put("createTime", folder.getCreateTime());
            long count = favoriteMapper.selectCount(
                    Wrappers.<Favorite>lambdaQuery().eq(Favorite::getFolderId, folder.getId()));
            map.put("count", count);
            result.add(map);
        }
        Page<Map<String, Object>> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(result);
        return resultPage;
    }

    public Page<Map<String, Object>> getPublicFolderDetail(Integer folderId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Favorite> wrapper = Wrappers.<Favorite>lambdaQuery();
        wrapper.eq(Favorite::getFolderId, folderId);
        wrapper.orderByDesc(Favorite::getCreateTime);
        Page<Favorite> page = favoriteMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        // 收集各类recordId
        List<Integer> imgIds = new ArrayList<>();
        List<Integer> videoIds = new ArrayList<>();
        List<Integer> cameraIds = new ArrayList<>();
        for (Favorite fav : page.getRecords()) {
            switch (fav.getRecordType()) {
                case "IMG" -> imgIds.add(fav.getRecordId());
                case "VIDEO" -> videoIds.add(fav.getRecordId());
                case "CAMERA" -> cameraIds.add(fav.getRecordId());
            }
        }

        // 批量查询记录详情
        Map<Integer, ImgRecords> imgMap = new HashMap<>();
        Map<Integer, VideoRecords> videoMap = new HashMap<>();
        Map<Integer, CameraRecords> cameraMap = new HashMap<>();
        if (!imgIds.isEmpty()) {
            List<ImgRecords> list = imgRecordsMapper.selectBatchIds(imgIds);
            list.forEach(r -> imgMap.put(r.getId(), r));
        }
        if (!videoIds.isEmpty()) {
            List<VideoRecords> list = videoRecordsMapper.selectBatchIds(videoIds);
            list.forEach(r -> videoMap.put(r.getId(), r));
        }
        if (!cameraIds.isEmpty()) {
            List<CameraRecords> list = cameraRecordsMapper.selectBatchIds(cameraIds);
            list.forEach(r -> cameraMap.put(r.getId(), r));
        }

        List<Map<String, Object>> enrichedRecords = new ArrayList<>();
        for (Favorite fav : page.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", fav.getId());
            item.put("recordType", fav.getRecordType());
            item.put("remark", fav.getRemark());
            item.put("priority", fav.getPriority());
            item.put("createTime", fav.getCreateTime());

            // 根据类型填充记录详情
            switch (fav.getRecordType()) {
                case "IMG" -> {
                    ImgRecords r = imgMap.get(fav.getRecordId());
                    if (r != null) {
                        item.put("inputImg", r.getInputImg());
                        item.put("outImg", r.getOutImg());
                        item.put("label", parseFirstLabel(r.getLabel()));
                        item.put("confidence", parseFirstConfidence(r.getConfidence()));
                    }
                }
                case "VIDEO" -> {
                    VideoRecords r = videoMap.get(fav.getRecordId());
                    if (r != null) {
                        item.put("inputImg", r.getInputVideo());
                        item.put("outImg", r.getOutVideo());
                        item.put("label", null);
                        item.put("confidence", null);
                    }
                }
                case "CAMERA" -> {
                    CameraRecords r = cameraMap.get(fav.getRecordId());
                    if (r != null) {
                        item.put("inputImg", null);
                        item.put("outImg", r.getOutVideo());
                        item.put("label", null);
                        item.put("confidence", null);
                    }
                }
            }
            enrichedRecords.add(item);
        }

        Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(enrichedRecords);
        return result;
    }

    // 批量查询收藏状态（用于列表页）
    public Map<Integer, Favorite> batchCheckFavorite(String username, List<Integer> recordIds, String recordType) {
        LambdaQueryWrapper<Favorite> wrapper = Wrappers.<Favorite>lambdaQuery();
        wrapper.eq(Favorite::getUsername, username)
                .eq(Favorite::getRecordType, recordType)
                .in(Favorite::getRecordId, recordIds);
        List<Favorite> list = favoriteMapper.selectList(wrapper);
        Map<Integer, Favorite> map = new HashMap<>();
        for (Favorite fav : list) {
            map.put(fav.getRecordId(), fav);
        }
        return map;
    }
}
