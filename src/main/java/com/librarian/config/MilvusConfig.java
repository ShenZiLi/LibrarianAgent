package com.librarian.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MilvusConfig {

    private final MilvusProperties milvusProperties;

    @Bean
    public MilvusClientV2 milvusClient() {
        MilvusClientV2 client = new MilvusClientV2(ConnectConfig.builder()
                .uri(milvusProperties.getHost() + ":" + milvusProperties.getPort())
                .build());
        log.info("Connected to Milvus at {}:{}", milvusProperties.getHost(), milvusProperties.getPort());
        return client;
    }
}
