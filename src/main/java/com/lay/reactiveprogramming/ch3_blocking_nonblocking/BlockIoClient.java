package com.lay.reactiveprogramming.ch3_blocking_nonblocking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.awt.print.Book;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
public class BlockIoClient implements CommandLineRunner {

    private URI baseUri = UriComponentsBuilder.newInstance().scheme("http")
            .host("localhost")
            .port(8080)
            .path("/v1/books")
            .build()
            .encode()
            .toUri();

    @Override
    public void run(String... args) throws Exception {
        log.info("# 요청 시작 시간: {}", LocalDateTime.now());

        for (int i = 0; i < 5; i++) {
            Book book = this.getBook(i);
            log.info("{}: book: {}", LocalDateTime.now(), book);
        }
    }

    private Book getBook(long bookId){
        RestTemplate restTemplate = new RestTemplate();

        URI getBooksUri = UriComponentsBuilder.fromUri(baseUri)
                .path("/{book-id}")
                .build()
                .expand(bookId)
                .encode()
                .toUri();

        ResponseEntity<Book> response = restTemplate.getForEntity(getBooksUri, Book.class);
        return response.getBody();

    }
}
