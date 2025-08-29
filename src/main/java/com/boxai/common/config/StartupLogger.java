package com.boxai.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupLogger implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    private final Environment env;

    public StartupLogger(Environment env) {
        this.env = env;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String app = env.getProperty("spring.application.name", "application");
        String port = env.getProperty("server.port", "8080");
        String ctx = env.getProperty("server.servlet.context-path", "");
        if (ctx == null) ctx = "";
        String base = "http://localhost:" + port + ctx;

        log.info("\n----------------------------------------------------------\n" +
                "Application '{}' started at {}\n" +
                "Swagger UI        : {}/swagger-ui.html\n" +
                "OpenAPI JSON      : {}/v3/api-docs\n" +
                "OpenAPI YAML      : {}/v3/api-docs.yaml\n" +
                "Health Check      : {}/api/health\n" +
                "----------------------------------------------------------",
                app, base, base, base, base, base);
    }
}


