package com.raghav.ragchatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Configuration
@Slf4j
public class ChatBotConfig {

    @Bean
    public RestClient.Builder builder() {
        return RestClient.builder().requestFactory(new SimpleClientHttpRequestFactory());
    }

    @Bean
    public ChromaApi chromaApi(RestClient.Builder restClientBuilder) {
        String chromaUrl = "http://localhost:8000";
        return new ChromaApi(chromaUrl, restClientBuilder, new ObjectMapper());
    }

    @Bean
    public VectorStore chromaVectorStore(EmbeddingModel embeddingModel, ChromaApi chromaApi) {

        String tenantName = "ragchatbot";
        String databaseName = "recipes_db";
        String collectionName = "indian_recipes";

        try {
            chromaApi.getTenant(tenantName);
        } catch (Exception e) {
            chromaApi.createTenant(tenantName);
        }

        try {
            chromaApi.getDatabase(tenantName, databaseName);
        } catch (Exception e) {
            chromaApi.createDatabase(tenantName, databaseName);
        }

        try {
            chromaApi.getCollection(tenantName, databaseName, collectionName);
        } catch (Exception e) {
            chromaApi.createCollection(
                    tenantName,
                    databaseName,
                    new ChromaApi.CreateCollectionRequest(
                            collectionName,
                            Map.of("hnsw:space", "cosine")
                    )
            );
        }

        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .tenantName(tenantName)
                .databaseName(databaseName)
                .collectionName(collectionName)
                .initializeSchema(true)
                .build();
    }

}
