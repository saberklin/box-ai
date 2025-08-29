package com.boxai.common.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi apiGroup() {
        return GroupedOpenApi.builder()
                .group("box-ai")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public io.swagger.v3.oas.models.OpenAPI springOpenAPI() {
        return new io.swagger.v3.oas.models.OpenAPI()
                .addServersItem(new Server().url("/").description("Default"))
                .info(new Info().title("Box AI Karaoke API")
                        .description("KTV 点歌/播放/场景/灯光 API")
                        .version("v1")
                        .contact(new Contact().name("BoxAI").email("support@example.com"))
                        .license(new License().name("Apache 2.0")))
                .externalDocs(new ExternalDocumentation().description("Docs"));
    }
}


