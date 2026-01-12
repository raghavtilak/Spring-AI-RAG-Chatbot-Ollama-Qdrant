package com.raghav.ragchatbot;

import jakarta.websocket.server.PathParam;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController("/chat")
public class ChatBotController {

    @Autowired
    private ChatBotService chatBotService;

    @GetMapping("/{query}")
    public ResponseEntity<String> chat(@PathVariable("query") String query){
        return chatBotService.chat(query);
    }

    @GetMapping("/template/{query}")
    public ResponseEntity<String> chatSystemMessageTemplate(@PathVariable("query") String query) {
        return chatBotService.chatSystemMessageTemplate(query);
    }

    //streaming
    @GetMapping(value = "/stream/{query}",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@PathVariable("query") String query){
        return chatBotService.streamChat(query);
    }

}
