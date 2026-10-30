package com.example.aichat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic());

        // 统一使用 user.dir 作为上传根目录，与 FileUploadUtil 保持一致
        String uploadBase = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadBase)
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic());

        String userUploadPath = uploadBase + "user" + File.separator;
        registry.addResourceHandler("/uploads/user/**")
                .addResourceLocations("file:" + userUploadPath)
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic());

        String characterUploadPath = uploadBase + "character" + File.separator;
        registry.addResourceHandler("/uploads/character/**")
                .addResourceLocations("file:" + characterUploadPath)
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic());
    }
}