package com.librarian.config;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusClientV2;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
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
        MilvusClientV2 client = new MilvusClientV2(ConnectParam.newBuilder()
                .withHost(milvusProperties.getHost())
                .withPort(milvusProperties.getPort())
                .build());
        log.info("Connected to Milvus at {}:{}", milvusProperties.getHost(), milvusProperties.getPort());
        return client;
    }
}
