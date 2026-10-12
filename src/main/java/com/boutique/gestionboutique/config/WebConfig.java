package com.boutique.gestionboutique.config;

import com.boutique.gestionboutique.security.MotDePasseObligatoireInterceptor;
import com.boutique.gestionboutique.service.StockageImageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final StockageImageService stockageImageService;

    public WebConfig(StockageImageService stockageImageService) {
        this.stockageImageService = stockageImageService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MotDePasseObligatoireInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/profil/mot-de-passe",          // la page où il doit justement agir
                        "/login", "/error",
                        "/webjars/**", "/css/**", "/js/**", "/images/**");
    }

    // Les images envoyées sont accessibles sous /uploads/produits/...
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String emplacement = stockageImageService.getDossier().toUri().toString();
        if (!emplacement.endsWith("/")) {
            emplacement += "/";
        }
        registry.addResourceHandler("/uploads/produits/**")
                .addResourceLocations(emplacement)
                // les noms de fichiers sont uniques (UUID) : le cache navigateur est sans risque
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)));
    }
}