package com.lay.reactiveprogramming.ch5;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.net.URI;
import java.util.Collections;

public class MonoAndFlux {

    public void start(){
        Flux<String> sequence = Flux.just("Hello", "Reactor");
        sequence.map(String::toLowerCase)
                .subscribe(data -> System.out.println("data = " + data));
    }

    public void mono(){
        Mono.just("Hello reactor")
                .subscribe(data -> System.out.println("data = " + data));
    }

    public void emptyMono(){
        Mono
                .empty()
                .subscribe(
                        none -> System.out.println("# emitted onNext signal"),
                        error -> {},
                        () -> System.out.println("# emitted onComplete signal")
                );
    }

    public void advancedMono(){
        URI worldTImeUri = UriComponentsBuilder.newInstance().scheme("http")
                .host("worldtimeapi.org")
                .port(80)
                .path("/api/timezone/Asia/Seoul")
                .build()
                .encode()
                .toUri();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Mono.just(
                restTemplate
                        .exchange(worldTImeUri,
                                HttpMethod.GET,
                                new HttpEntity<String>(headers),
                                String.class)
        )
                .map(response -> {
                    DocumentContext jsonContext = JsonPath.parse(response.getBody());
                    return jsonContext.<String>read("$.datetime");
                })
                .subscribe(
                        data -> System.out.println("# emitted data: " + data),
                        error -> {
                            System.out.println("error = " + error);
                        },
                        () -> System.out.println("# emitted onComplete signal")
                );
    }

    public void flux() {
        Flux.just(6, 9, 74)
                .map(num -> num % 2)
                .subscribe(System.out::println);
    }

    public void flux2(){
        Flux.fromArray(new Integer[]{3, 6, 9 ,74})
                .filter(num -> num > 6)
                .map(num -> num * 2)
                .subscribe(System.out::println);
    }

    public void advancedFlux(){
        Flux<String> flux = Mono.justOrEmpty("Steve")
                .concatWith(Mono.justOrEmpty("Jobs"));
        flux.subscribe(System.out::println);
    }

    public void advancedFlux2(){
        Flux.concat(
                Flux.just("Mercury", "Venus", "Earth"),
                Flux.just("Mars", "Jupiter", "Saturn"),
                Flux.just("Uranus", "Neptune", "Pluto"))
                .collectList()
                .subscribe(System.out::println);
    }


}
