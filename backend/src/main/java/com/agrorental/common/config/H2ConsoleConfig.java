package com.agrorental.common.config;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Registers the H2 web console servlet directly. Spring Boot's built-in H2
 * console auto-configuration isn't present on this project's dependency set
 * (it moved/split across the newer, more granular Boot 4.x modules), so
 * "spring.h2.console.enabled" alone no longer wires it up — this bean does
 * the same job explicitly. Dev/local tooling only; never registered in prod.
 */
@Configuration
@Profile("!prod")
public class H2ConsoleConfig {

    @Value("${spring.h2.console.path:/h2-console}")
    private String path;

    @Bean
    public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServlet() {
        ServletRegistrationBean<JakartaWebServlet> registration =
                new ServletRegistrationBean<>(new JakartaWebServlet(), path + "/*");
        registration.addInitParameter("webAllowOthers", "true");
        return registration;
    }
}
