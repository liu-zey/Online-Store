package com.example.onlinestore.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import static org.assertj.core.api.Assertions.assertThat;

class NacosConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NacosConfig.class));

    @Test
    void nacosConfig_isEnabled_whenPropertyIsTrue() {
        this.contextRunner
                .withPropertyValues("spring.cloud.nacos.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(NacosConfig.class);
                    // Check if @EnableDiscoveryClient related beans are present (can be tricky)
                    // A simpler check is that NacosConfig itself is loaded.
                    // Verifying @EnableDiscoveryClient often means checking for a DiscoveryClient bean,
                    // but this might require more infrastructure (like a mock Nacos server or more Spring Cloud context).
                    // For now, let's ensure NacosConfig bean is present.
                });
    }

    @Test
    void nacosConfig_isEnabled_whenPropertyIsMissing() {
        this.contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(NacosConfig.class);
                });
    }

    @Test
    void nacosConfig_isDisabled_whenPropertyIsFalse() {
        this.contextRunner
                .withPropertyValues("spring.cloud.nacos.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(NacosConfig.class);
                });
    }

    // It might be useful to also test if DiscoveryClient related beans are loaded
    // when NacosConfig is enabled. This can be more involved as it might
    // require mocking parts of Spring Cloud Nacos discovery.
    // For a start, the above tests verify the conditional loading of NacosConfig itself.
}
