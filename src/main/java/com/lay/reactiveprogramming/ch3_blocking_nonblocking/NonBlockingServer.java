package com.lay.reactiveprogramming.ch3_blocking_nonblocking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.awt.print.Book;
import java.util.Map;

@Slf4j
@RequestMapping("/v1/books")
@RequiredArgsConstructor
@RestController
public class NonBlockingServer {
    private Map<Long, Book> bookMap;

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{book-id}")
    public Mono<Book> getBook(@PathVariable("book-id") long bookId) throws InterruptedException {
        Thread.sleep(5000);
        Book book = bookMap.get(bookId);
        return Mono.just(book);
    }


}
