package com.raghav.ragchatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
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

        ChromaApi.CreateCollectionRequest request = new ChromaApi.CreateCollectionRequest("indian_recipes", Map.of(
                "tenant", "ragchatbot",
                "database", "recipes_db"
        ));
        String tenantName = "ragchatbot"; // Check your configuration/Chroma setup for correct tenant/db names
        String databaseName = "recipes_db";
        ChromaApi.Tenant tenant = chromaApi.getTenant(tenantName);
        if(tenant==null){
            chromaApi.createTenant(tenantName);
        }

        if(chromaApi.getDatabase(tenantName,databaseName)==null){
            chromaApi.createDatabase(tenantName,databaseName);
        }

        if(chromaApi.getCollection(tenantName,databaseName,"indian_recipes")==null){
            chromaApi.createCollection(tenantName, databaseName, request);
        }

        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .tenantName("ragchatbot") // default: SpringAiTenant
                .databaseName("recipes_db") // default: SpringAiDatabase
                .collectionName("indian_recipes")
                .initializeSchema(true)
                .build();
    }

}
