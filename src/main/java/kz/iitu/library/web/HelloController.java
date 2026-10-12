package kz.iitu.library.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** Lab 1: the first REST controller — @RestController, @GetMapping, @RequestParam, @Value. */
@RestController
@RequestMapping("/api")
public class HelloController {

    @Value("${app.owner:unknown}")
    private String owner;

    @GetMapping("/hello")
    public Greeting hello(@RequestParam(defaultValue = "world") String name) {
        return new Greeting("Hello, " + name + "!", owner, LocalDateTime.now());
    }

    @GetMapping("/info")
    public Info info() {
        return new Info(owner, System.getProperty("java.version"),
                Runtime.getRuntime().availableProcessors());
    }

    // Lab 1, variant 7: word count, character count and the longest word
    @GetMapping("/wordcount")
    public WordCount wordCount(@RequestParam(defaultValue = "") String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return new WordCount(0, 0, "");
        String[] words = trimmed.split("\\s+");
        String longestWord = words[0];
        for (String word : words) {
            if (word.length() > longestWord.length()) longestWord = word;
        }
        return new WordCount(words.length, text.length(), longestWord);
    }

    public record Greeting(String message, String owner, LocalDateTime timestamp) {}
    public record Info(String owner, String javaVersion, int cpuCores) {}
    public record WordCount(int wordCount, int characterCount, String longestWord) {}
}
