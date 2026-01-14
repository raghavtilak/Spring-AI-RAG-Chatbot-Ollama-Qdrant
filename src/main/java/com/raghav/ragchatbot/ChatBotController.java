package com.raghav.ragchatbot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController("/chat")
@CrossOrigin
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
        return chatBotService.streamChat(query).concatWith(Flux.just("[DONE]"));
    }

}
