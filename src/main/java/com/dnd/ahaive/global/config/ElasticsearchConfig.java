package com.dnd.ahaive.global.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris}")
    private String esUri;

    @Override
    public ClientConfiguration clientConfiguration() {
        URI uri = URI.create(esUri);
        String hostAndPort = uri.getHost() + ":" + uri.getPort();
        return ClientConfiguration.builder()
                .connectedTo(hostAndPort)
                .build();
    }
}
