package com.lay.reactiveprogramming.ch6;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;

public class HotAndColdSequence {
    public void coldSequence() throws InterruptedException {
        Flux<String> coldFlux = Flux.fromIterable(Arrays.asList("KOREA", "JAPAN", "CHINESE"))
                .map(String::toLowerCase);

        coldFlux.subscribe(country -> System.out.println("country = " + country));
        System.out.println("==================================");
        Thread.sleep(2000L);
        coldFlux.subscribe(country -> System.out.println("country = " + country));
    }

    public void hotSequence() throws InterruptedException {
        String[] singers = {"Singer A", "Singer B", "Singer C", "Singer D", "Singer E"};

        System.out.println("# Begin concert:");
        Flux<String> concertFlux = Flux.fromArray(singers)
                .delayElements(Duration.ofSeconds(1))
                .share();

        concertFlux.subscribe(
                singer -> System.out.println("Singer: " + singer)
        );

        Thread.sleep(2500);

        concertFlux.subscribe(
                singer -> System.out.println("Singer: " + singer)
        );

        Thread.sleep(3000);
    }

    public void SequenceHttp() throws InterruptedException {
        URI worldTimeUri = UriComponentsBuilder.newInstance().scheme("http")
                .host("worldtimeapi.org")
                .port(80)
                .path("/api/timezone/Asia/Seoul")
                .build()
                .encode()
                .toUri();

        Mono<String> coldMono = getWorldTime(worldTimeUri);
        Mono<String> hotMono = getWorldTime(worldTimeUri).cache();
        Mono<String> mono = getWorldTime(worldTimeUri);
        mono.subscribe(dateTime -> System.out.println("dateTime = " + dateTime));
        Thread.sleep(2000);
        mono.subscribe(dateTime -> System.out.println("dateTime = " + dateTime));
        Thread.sleep(2000);
    }

    private Mono<String> getWorldTime(URI worldTimeUri) {
        return WebClient.create()
                .get()
                .uri(worldTimeUri)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    DocumentContext jsonContext = JsonPath.parse(response);
                    String dateTime = jsonContext.read("$.datetime");
                    return dateTime;
                });
    }


}
