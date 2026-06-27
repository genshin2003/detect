package com.example.Kcsj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.Kcsj.common.JwtUtils;
import com.example.Kcsj.common.Result;
import com.example.Kcsj.entity.Favorite;
import com.example.Kcsj.entity.FavoriteFolder;
import com.example.Kcsj.service.FavoriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/favorite")
public class FavoriteController {
    @Resource
    FavoriteService favoriteService;

    private String getUsername(HttpServletRequest request) {
        Object username = request.getAttribute("username");
        return username != null ? username.toString() : null;
    }

    // ========== 收藏夹接口 ==========

    @PostMapping("/folder")
    public Result<?> createFolder(@RequestBody FavoriteFolder folder, HttpServletRequest request) {
        String username = getUsername(request);
        if (username == null) return Result.error("-1", "未登录");
        folder.setUsername(username);
        favoriteService.createFolder(folder);
        return Result.success();
    }

    @PutMapping("/folder")
    public Result<?> updateFolder(@RequestBody FavoriteFolder folder) {
        favoriteService.updateFolder(folder);
        return Result.success();
    }

    @DeleteMapping("/folder/{id}")
    public Result<?> deleteFolder(@PathVariable("id") Integer id, HttpServletRequest request) {
        String username = getUsername(request);
        if (username == null) return Result.error("-1", "未登录");
        favoriteService.deleteFolder(id, username);
        return Result.success();
    }

    @GetMapping("/folders")
    public Result<?> getMyFolders(HttpServletRequest request) {
        String username = getUsername(request);
        if (username == null) return Result.error("-1", "未登录");
        return Result.success(favoriteService.getMyFolders(username));
    }

    // ========== 收藏接口 ==========

    @PostMapping
    public Result<?> addFavorite(@RequestBody Favorite favorite, HttpServletRequest request) {
        String username = getUsername(request);
        if (username == null) return Result.error("-1", "未登录");
        favorite.setUsername(username);
        favoriteService.addFavorite(favorite);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> removeFavorite(@PathVariable("id") Integer id, HttpServletRequest request) {
        String username = getUsername(request);
        if (username == null) return Result.error("-1", "未登录");
        favoriteService.removeFavorite(id, username);
        return Result.success();
    }

    @PutMapping
    public Result<?> updateFavorite(@RequestBody Favorite favorite) {
        favoriteService.updateFavorite(favorite);
        return Result.success();
    }

    @GetMapping("/check")
    public Result<?> checkFavorite(@RequestParam("recordId") Integer recordId,
                                    @RequestParam("recordType") String recordType,
                                    HttpServletRequest request) {
        String username = getUsername(request);
        if (username == null) return Result.error("-1", "未登录");
        Favorite fav = favoriteService.checkFavorite(username, recordId, recordType);
        Map<String, Object> result = new HashMap<>();
        result.put("favorited", fav != null);
        result.put("favorite", fav);
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<?> list(@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
                          @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                          @RequestParam(name = "folderId", required = false) Integer folderId,
                          @RequestParam(name = "priority", required = false) String priority,
                          HttpServletRequest request) {
        String username = getUsername(request);
        if (username == null) return Result.error("-1", "未登录");
        Page<Map<String, Object>> page = favoriteService.findPage(pageNum, pageSize, username, folderId, priority);
        return Result.success(page);
    }

    @PostMapping("/batchRemove")
    public Result<?> batchRemove(@RequestBody List<Integer> ids, HttpServletRequest request) {
        String username = getUsername(request);
        if (username == null) return Result.error("-1", "未登录");
        favoriteService.batchRemove(ids, username);
        return Result.success();
    }

    @PostMapping("/batchMove")
    public Result<?> batchMove(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String username = getUsername(request);
        if (username == null) return Result.error("-1", "未登录");
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) params.get("ids");
        Integer targetFolderId = (Integer) params.get("targetFolderId");
        favoriteService.batchMove(ids, targetFolderId, username);
        return Result.success();
    }

    @GetMapping("/batchCheck")
    public Result<?> batchCheck(@RequestParam("recordIds") List<Integer> recordIds,
                                 @RequestParam("recordType") String recordType,
                                 HttpServletRequest request) {
        String username = getUsername(request);
        if (username == null) return Result.error("-1", "未登录");
        return Result.success(favoriteService.batchCheckFavorite(username, recordIds, recordType));
    }

    // ========== 公开收藏夹接口 ==========

    @GetMapping("/public/folders")
    public Result<?> getPublicFolders(@RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
                                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                       HttpServletRequest request) {
        String username = getUsername(request);
        return Result.success(favoriteService.getPublicFolders(pageNum, pageSize, username));
    }

    @GetMapping("/public/folder/{id}")
    public Result<?> getPublicFolderDetail(@PathVariable("id") Integer id,
                                            @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
                                            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(favoriteService.getPublicFolderDetail(id, pageNum, pageSize));
    }
}
