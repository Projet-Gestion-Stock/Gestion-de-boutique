package com.boutique.gestionboutique.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MotDePasseObligatoireInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/profil/mot-de-passe",          // la page où il doit justement agir
                        "/login", "/error",
                        "/webjars/**", "/css/**", "/js/**", "/images/**");
    }
}