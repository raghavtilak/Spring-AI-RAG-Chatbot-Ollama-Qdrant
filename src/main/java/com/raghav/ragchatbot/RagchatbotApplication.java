package com.raghav.ragchatbot;

import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
		ChromaVectorStoreAutoConfiguration.class
})
public class RagchatbotApplication {

	public static void main(String[] args) {
		SpringApplication.run(RagchatbotApplication.class, args);
	}

}
