package com.example.CampingHub.seguridad;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns(
                "/salud", "/fincas", "/fincas/**", "/reservas", "/reservas/**",
                "/usuarios", "/usuarios/**", "/clientes", "/clientes/**",
                "/administradores", "/administradores/**", "/notificaciones", "/notificaciones/**");
    }
}
