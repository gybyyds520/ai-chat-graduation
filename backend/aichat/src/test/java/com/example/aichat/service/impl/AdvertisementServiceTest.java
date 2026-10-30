package com.example.aichat.service.impl;

import com.example.aichat.entity.Advertisement;
import com.example.aichat.mapper.AdvertisementMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AdvertisementService 单元测试（Mockito 隔离测试）。
 *
 * 覆盖点：
 *   1. createAd - 正常创建广告，设置默认值
 *   2. updateAd - 正常更新
 *   3. deleteAd - 正常删除
 *   4. getAdById - 查询存在/不存在
 *   5. getAllAds - 查询全部
 *   6. getAdsByPosition - 按位置查询
 *   7. getActiveAds - 查询生效中
 *   8. enableAd - 启用广告
 *   9. disableAd - 禁用广告
 *  10. trackClick - 记录点击
 */
@ExtendWith(MockitoExtension.class)
class AdvertisementServiceTest {

    @Mock
    private AdvertisementMapper advertisementMapper;

    @InjectMocks
    private AdvertisementServiceImpl advertisementService;

    private Advertisement sampleAd;

    @BeforeEach
    void setUp() {
        sampleAd = new Advertisement();
        sampleAd.setId(1L);
        sampleAd.setTitle("测试广告");
        sampleAd.setContent("广告内容");
        sampleAd.setPosition("homepage_banner");
        sampleAd.setStatus("ENABLED");
    }

    @Test
    void createAd_正常创建应设置默认值() {
        Advertisement ad = new Advertisement();
        ad.setTitle("新广告");
        ad.setPosition("sidebar");

        Advertisement result = advertisementService.createAd(ad);

        assertNotNull(result);
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
        assertEquals(0, result.getClickCount());
        assertEquals(100, result.getPriority(), "默认优先级应为100");
        verify(advertisementMapper).insert(ad);
    }

    @Test
    void createAd_已有优先级应保留原值() {
        Advertisement ad = new Advertisement();
        ad.setTitle("高优先级广告");
        ad.setPosition("banner");
        ad.setPriority(10);

        advertisementService.createAd(ad);

        ArgumentCaptor<Advertisement> captor = ArgumentCaptor.forClass(Advertisement.class);
        verify(advertisementMapper).insert(captor.capture());
        assertEquals(10, captor.getValue().getPriority());
    }

    @Test
    void updateAd_正常更新应返回更新后对象() {
        when(advertisementMapper.selectById(1L)).thenReturn(sampleAd);

        sampleAd.setTitle("更新后的广告");
        Advertisement result = advertisementService.updateAd(sampleAd);

        assertNotNull(result);
        verify(advertisementMapper).updateById(any(Advertisement.class));
    }

    @Test
    void deleteAd_正常删除应调用mapper() {
        advertisementService.deleteAd(1L);

        verify(advertisementMapper).deleteById(1L);
    }

    @Test
    void getAdById_存在应返回对象() {
        when(advertisementMapper.selectById(1L)).thenReturn(sampleAd);

        Advertisement result = advertisementService.getAdById(1L);

        assertNotNull(result);
        assertEquals("测试广告", result.getTitle());
    }

    @Test
    void getAdById_不存在应返回null() {
        when(advertisementMapper.selectById(999L)).thenReturn(null);

        Advertisement result = advertisementService.getAdById(999L);

        assertNull(result);
    }

    @Test
    void getAllAds_应返回列表() {
        List<Advertisement> ads = List.of(sampleAd);
        when(advertisementMapper.selectList(null)).thenReturn(ads);

        List<Advertisement> result = advertisementService.getAllAds();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getAdsByPosition_应返回对应位置广告() {
        when(advertisementMapper.getAllAdsByPosition("homepage_banner")).thenReturn(List.of(sampleAd));

        List<Advertisement> result = advertisementService.getAdsByPosition("homepage_banner");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getActiveAds_应返回生效中广告() {
        when(advertisementMapper.getActiveAdsByPosition(eq("homepage_banner"), any(LocalDateTime.class)))
                .thenReturn(List.of(sampleAd));

        List<Advertisement> result = advertisementService.getActiveAds("homepage_banner");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void enableAd_应将状态设为ENABLED() {
        sampleAd.setStatus("DISABLED");
        when(advertisementMapper.selectById(1L)).thenReturn(sampleAd);

        advertisementService.enableAd(1L);

        ArgumentCaptor<Advertisement> captor = ArgumentCaptor.forClass(Advertisement.class);
        verify(advertisementMapper).updateById(captor.capture());
        assertEquals("ENABLED", captor.getValue().getStatus());
    }

    @Test
    void enableAd_广告不存在不应报错() {
        when(advertisementMapper.selectById(999L)).thenReturn(null);

        assertDoesNotThrow(() -> advertisementService.enableAd(999L));
        verify(advertisementMapper, never()).updateById(any(Advertisement.class));
    }

    @Test
    void disableAd_应将状态设为DISABLED() {
        when(advertisementMapper.selectById(1L)).thenReturn(sampleAd);

        advertisementService.disableAd(1L);

        ArgumentCaptor<Advertisement> captor = ArgumentCaptor.forClass(Advertisement.class);
        verify(advertisementMapper).updateById(captor.capture());
        assertEquals("DISABLED", captor.getValue().getStatus());
    }

    @Test
    void trackClick_应调用mapper增加点击数() {
        advertisementService.trackClick(1L);

        verify(advertisementMapper).incrementClickCount(1L);
    }
}
