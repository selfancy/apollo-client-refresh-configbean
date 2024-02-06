package com.github.selfancy.sample;

import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import com.github.selfancy.apollo.DynamicProperties;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.IOException;

/**
 * SampleApplication
 * <p>
 * Created by mike on 2024/02/05
 */
@SpringBootApplication
@EnableConfigurationProperties(SampleApplication.SampleProperties.class)
public class SampleApplication {

    public static void main(String[] args) throws IOException {
        System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "true");
        System.setProperty(PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES, "application");
        System.setProperty("env", "DEV");
        System.setProperty("app.id", "SampleApp");
        System.setProperty("apollo.meta", "http://127.0.0.1:8080");

        System.setProperty("logging.level.com.ctrip", "debug");
        SpringApplication.run(SampleApplication.class, args);
        System.in.read();
    }

    @Data
    @DynamicProperties
    @ConfigurationProperties("sample")
    public static class SampleProperties {
        private int timeout;
        private Nested nested;

        @Data
        public static class Nested {
            private String url;
        }
    }
}
