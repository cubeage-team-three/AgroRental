package com.agrorental.common.config;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Registers the H2 web console servlet directly. Spring Boot's built-in H2
 * console auto-configuration isn't present on this project's dependency set
 * (it moved/split across the newer, more granular Boot 4.x modules), so
 * "spring.h2.console.enabled" alone no longer wires it up — this bean does
 * the same job explicitly.
 *
 * <p>Local development tooling only: registered solely when the "dev" profile is explicitly
 * active and spring.h2.console.enabled=true (see {@link DeploymentProfiles#isH2ConsoleEnabled}).
 * Never registered for an implicit default profile, staging or prod. Remote (non-localhost)
 * console connections are not allowed.
 */
@Configuration
@Conditional(H2ConsoleConfig.H2ConsoleEnabledCondition.class)
public class H2ConsoleConfig {

    @Value("${spring.h2.console.path:/h2-console}")
    private String path;

    @Bean
    public ServletRegistrationBean<JakartaWebServlet> h2ConsoleServlet() {
        return new ServletRegistrationBean<>(new JakartaWebServlet(), path + "/*");
    }

    static class H2ConsoleEnabledCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return DeploymentProfiles.isH2ConsoleEnabled(context.getEnvironment());
        }
    }
}
