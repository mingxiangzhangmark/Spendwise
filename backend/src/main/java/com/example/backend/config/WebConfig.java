package com.example.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 打印路径，方便确认
        String uploadPath = System.getProperty("user.dir") + "/uploads/picture/";
//        System.out.println(">>> 图片映射目录: " + uploadPath);

        registry.addResourceHandler("/picture/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
