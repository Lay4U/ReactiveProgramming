package com.lay.reactiveprogramming.ch3_blocking_nonblocking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.awt.print.Book;
import java.net.URI;
import java.time.LocalTime;

@Slf4j
public class NonBlockingClient implements CommandLineRunner {

    private URI baseUri = UriComponentsBuilder.newInstance().scheme("http")
            .host("localhost")
            .port(6060)
            .build()
            .encode()
            .toUri();
    @Override
    public void run(String... args) throws Exception {
        System.setProperty("reactor.netty.ioWorkerCount", "1");
        log.info("# 요청 시작 시간: {}", LocalTime.now());

        for (int i = 0; i < 5; i++) {
            int a = i;
            this.getBook(i)
                    .subscribe(book -> {
                        log.info("{}: book: {}", LocalTime.now(), book);
                    });
        }
    }

    private Mono<Book> getBook(long bookId){
        URI getBooksUrl = UriComponentsBuilder.fromUri(baseUri)
                .path("/{book-id}")
                .build()
                .expand(bookId)
                .encode()
                .toUri();

        return WebClient.create()
                .get()
                .uri(getBooksUrl)
                .retrieve()
                .bodyToMono(Book.class);
    }
}
