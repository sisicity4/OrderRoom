package com.github.karuhito.orderroombackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.github.karuhito.orderroombackend.converter.StringToItemStatusConverter;
import com.github.karuhito.orderroombackend.interceptor.HostKeyInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final HostKeyInterceptor hostKeyInterceptor;
    private final StringToItemStatusConverter stringToItemStatusConverter;

    public WebMvcConfig(HostKeyInterceptor hostKeyInterceptor, StringToItemStatusConverter stringToItemStatusConverter) {
        this.hostKeyInterceptor = hostKeyInterceptor;
        this.stringToItemStatusConverter = stringToItemStatusConverter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(hostKeyInterceptor)
        .addPathPatterns("/api/rooms/*/items/*/*");
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToItemStatusConverter);
    }
}