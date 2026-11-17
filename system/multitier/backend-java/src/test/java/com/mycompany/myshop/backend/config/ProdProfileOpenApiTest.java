package com.mycompany.myshop.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

/** B-PRG 4.4: the production profile serves no OpenAPI document and no Swagger UI. */
class ProdProfileOpenApiTest {

    @Test
    void prodProfileDisablesTheApiDocsAndSwaggerUi() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-prod.yml"));
        var properties = yaml.getObject();

        assertThat(properties).containsEntry("springdoc.api-docs.enabled", false);
        assertThat(properties).containsEntry("springdoc.swagger-ui.enabled", false);
    }
}
