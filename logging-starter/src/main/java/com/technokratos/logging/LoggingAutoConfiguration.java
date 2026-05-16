package com.technokratos.logging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@AutoConfiguration
public class LoggingAutoConfiguration {

    /**
     * MVC (servlet) — регистрируется только когда есть jakarta.servlet.Filter в classpath.
     * Не регистрируется в Gateway (WebFlux-only).
     */
    @Configuration
    @ConditionalOnClass(name = "jakarta.servlet.Filter")
    static class ServletLoggingConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnMissingClass("org.springframework.cloud.gateway.filter.GatewayFilterChain")
        public FilterRegistrationBean<MdcFilter> mdcFilter() {
            FilterRegistrationBean<MdcFilter> registration = new FilterRegistrationBean<>(new MdcFilter());
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
            registration.addUrlPatterns("/*");
            return registration;
        }
    }
}
