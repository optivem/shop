package com.mycompany.myshop.backend.component.legacy.smoke.system;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class MyShopSmokeTest extends BaseComponentTest {

    @Test
    void bootsInProcessAndServesHttp() {
        var response = restTemplate.getForEntity("/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }
}
