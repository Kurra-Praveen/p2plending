package com.llms.config;

import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class TestConfig {
    // ObjectMapper is already configured in JacksonConfig with @Primary
    // No need for a test-specific ObjectMapper since the configuration is identical
}

