package com.github.karuhito.orderroombackend.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.github.karuhito.orderroombackend.converter.StringToItemStatusConverter;
import com.github.karuhito.orderroombackend.interceptor.HostKeyInterceptor;
import com.github.karuhito.orderroombackend.resolver.ParticipantArgumentResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final HostKeyInterceptor hostKeyInterceptor;
    private final StringToItemStatusConverter stringToItemStatusConverter;
    private final ParticipantArgumentResolver participantArgumentResolver;

    public WebMvcConfig(HostKeyInterceptor hostKeyInterceptor, StringToItemStatusConverter stringToItemStatusConverter, ParticipantArgumentResolver participantArgumentResolver) {
        this.hostKeyInterceptor = hostKeyInterceptor;
        this.stringToItemStatusConverter = stringToItemStatusConverter;
        this.participantArgumentResolver = participantArgumentResolver;
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

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(participantArgumentResolver);
    }
}
