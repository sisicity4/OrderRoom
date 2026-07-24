package com.github.karuhito.orderroombackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.github.karuhito.orderroombackend.interceptor.HostKeyInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final HostKeyInterceptor hostKeyInterceptor;

    public WebMvcConfig(HostKeyInterceptor hostKeyInterceptor) {
        this.hostKeyInterceptor = hostKeyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(hostKeyInterceptor)
        .addPathPatterns("/api/rooms/*/items/*/*");
    }
    
}