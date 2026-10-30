package com.example.aichat.controller;

import com.example.aichat.dto.R;
import com.example.aichat.entity.Advertisement;
import com.example.aichat.service.AdvertisementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 广告管理控制器
 * 安全加固（P1）：移除 @CrossOrigin(origins = "*")，统一走 CorsConfig 白名单
 * 管理端点（创建/更新/删除/启用/禁用）要求 ADMIN 角色；公开读取端点允许已登录用户访问。
 */
@RestController
@RequestMapping("/api/advertisement")
public class AdvertisementController {

    private final AdvertisementService advertisementService;

    public AdvertisementController(AdvertisementService advertisementService) {
        this.advertisementService = advertisementService;
    }

    /**
     * 创建广告
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<Advertisement> create(@RequestBody Advertisement ad) {
        Advertisement created = advertisementService.createAd(ad);
        return R.success(created);
    }

    /**
     * 更新广告
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Advertisement> update(@PathVariable Long id, @RequestBody Advertisement ad) {
        ad.setId(id);
        Advertisement updated = advertisementService.updateAd(ad);
        return R.success(updated);
    }

    /**
     * 删除广告
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        advertisementService.deleteAd(id);
        return R.success();
    }

    /**
     * 获取广告详情
     */
    @GetMapping("/{id}")
    public R<Advertisement> getById(@PathVariable Long id) {
        Advertisement ad = advertisementService.getAdById(id);
        if (ad == null) {
            return R.error("广告不存在");
        }
        return R.success(ad);
    }

    /**
     * 获取所有广告
     */
    @GetMapping
    public R<List<Advertisement>> getAll() {
        List<Advertisement> ads = advertisementService.getAllAds();
        return R.success(ads);
    }

    /**
     * 根据位置获取广告
     */
    @GetMapping("/position/{position}")
    public R<List<Advertisement>> getByPosition(@PathVariable String position) {
        List<Advertisement> ads = advertisementService.getAdsByPosition(position);
        return R.success(ads);
    }

    /**
     * 获取当前生效的广告
     */
    @GetMapping("/active/{position}")
    public R<List<Advertisement>> getActive(@PathVariable String position) {
        List<Advertisement> ads = advertisementService.getActiveAds(position);
        return R.success(ads);
    }

    /**
     * 启用广告
     */
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> enable(@PathVariable Long id) {
        advertisementService.enableAd(id);
        return R.success();
    }

    /**
     * 禁用广告
     */
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> disable(@PathVariable Long id) {
        advertisementService.disableAd(id);
        return R.success();
    }

    /**
     * 记录点击
     */
    @PostMapping("/{id}/click")
    public R<Void> trackClick(@PathVariable Long id) {
        advertisementService.trackClick(id);
        return R.success();
    }
}