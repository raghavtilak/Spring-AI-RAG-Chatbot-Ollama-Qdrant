package com.raghav.ragchatbot;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatBotService {

    private String template;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    @Value("classpath:/docs/indian_recepies.pdf")
    private Resource indianRecepiesPDF;

    public ChatBotService(@Lazy VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.clone().build();
        this.retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .build())
                .build();
    }
    public ResponseEntity<String> chat(String query) {

        try{
            String content = chatClient.prompt()
                    .advisors(retrievalAugmentationAdvisor)
                    .user(query)
                    .call()
                    .content();

            log.info("Chat Client Response:"+content);
            return ResponseEntity.ok(content);

        } catch (Exception e) {

            e.printStackTrace();
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<String> chatSystemMessageTemplate(String query) {

        try{
            String content = chatClient.prompt()
                    .advisors(retrievalAugmentationAdvisor)
                    .system(systemSpec -> systemSpec
                            .text(template)
                            .param("greeting", randomGreeting())
                    )
                    .user(query)
                    .call()
                    .content();
            log.info("Chat Client Response:"+content);
            return ResponseEntity.ok(content);

        }catch(Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @PostConstruct
    public void initVectorDatabase(){
        try{
            vectorStore.accept(
                    getDocsFromPDF()
            );
            log.info("Data Loaded in Vector");

            template = """
                You are an expert Indian food assistant.
                
                You MUST answer ONLY using the information provided in the context.
                If the answer is not present in the context, reply exactly:
                "I don't know based on the provided recipe."

                Always first greet with {greeting}               
            """;
        } catch (Exception e) {

            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    private List<Document> getDocsFromPDF(){
        var pdfReader = new PagePdfDocumentReader(indianRecepiesPDF);
        TextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(500)
                .build();
        List<Document> documents = textSplitter.apply(pdfReader.get());
        log.info("Total chunks created: {}", documents.size());
        log.info("Doc generated:");
        documents.forEach(doc -> {
            log.info("Chunk ID: {}", doc.getId());
            log.info("Chunk size (chars): {}", doc.getText().length());
        });

        return documents;
    }

    private String randomGreeting() {
        var names = List.of("Namaste","Hello","Kese hain aap,", "Namashkar");
        return names.get(new Random().nextInt(names.size()));
    }

    public Flux<String> streamChat(String query) {
        Flux<String> content = chatClient.prompt()
                .advisors(retrievalAugmentationAdvisor)
                .user(query)
                .stream()
                .content()
                .transform(flux->toChunk(flux,20));
        return content;
    }

    private Flux<String> toChunk(Flux<String> tokenFlux, int chunkSize) {
        return Flux.create(sink -> {
            StringBuilder buffer = new StringBuilder();
            tokenFlux.subscribe(
                    token -> {
                        buffer.append(token);
                        if (buffer.length() >= chunkSize) {
                            sink.next(buffer.toString());
                            buffer.setLength(0);
                        }
                    },
                    sink::error,
                    () -> {
                        if (buffer.length() > 0) {
                            sink.next(buffer.toString());
                        }
                        sink.complete();
                    }
            );
        });
    }
}
