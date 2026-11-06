package com.example.consumer;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import run.iknow.mustachereact.spring.ReactPage;

@RestController
public class HelloController {

    public record Hello(String message, List<String> frameworks) {
    }

    @ReactPage(value = "Hello", title = "Hello")
    @GetMapping("/")
    public Hello hello() {
        return new Hello("Hello from Spring", List.of("Spring Boot", "React", "Vite"));
    }
}
