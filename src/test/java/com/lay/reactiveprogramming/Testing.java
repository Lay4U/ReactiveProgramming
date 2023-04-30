package com.lay.reactiveprogramming;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.Base64Utils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.StepVerifierOptions;
import reactor.test.publisher.PublisherProbe;
import reactor.test.publisher.TestPublisher;
import reactor.test.scheduler.VirtualTimeScheduler;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class Testing {

    public static Flux<String> sayHello() {
        return Flux
                .just("Hello", "reactor");
    }

    public static Flux<Integer> divideByTwo(Flux<Integer> source){
        return source
                .zipWith(Flux.just(2, 2, 2, 2, 0), (x, y) -> x/y);
    }

    public static Flux<Integer> takeNumber(Flux<Integer> source, long n) {
        return source
                .take(n);
    }


    @Test
    void sayHelloReactorTEst() {
        StepVerifier
                .create(Mono.just("Hello Reactor"))
                .expectNext("Hello Reactor")
                .expectComplete()
                .verify();
    }

    @Test
    void sayHelloTest() {
        StepVerifier
                .create(sayHello())
                .expectSubscription()
                .as("# expect subscription")
//                .expectNext("Hi")
//                .as("# expect Hi")
                .expectNext("Hello")
                .as("# expect Hello")
                .expectNext("Reactor")
                .as("# expect Reactor")
                .verifyComplete();
    }

    @Test
    void divideByTwoTest() {
        Flux<Integer> source = Flux.just(2, 4, 6, 8 ,10);
        StepVerifier
                .create(divideByTwo(source))
                .expectSubscription()
                .expectNext(1)
                .expectNext(2)
                .expectNext(3)
                .expectNext(4)
//                .expectNext(1, 2, 3, 4)
                .expectError()
                .verify();
    }

    @Test
    void takeNumberTest() {
        Flux<Integer> source = Flux.range(0, 10000);
        StepVerifier.create(takeNumber(source, 500),
                StepVerifierOptions.create().scenarioName("Verify from 0 to 499"))
                .expectSubscription()
                .expectNext(0)
                .expectNextCount(498)
                .expectNext(499)
                .expectComplete()
                .verify();
    }

    public static Flux<Tuple2<String, Integer>> getCOVID19Count(Flux<Long> source){
        return source
                .flatMap(notUse -> Flux.just(
                        Tuples.of("서울", 10),
                        Tuples.of("경기도", 5),
                        Tuples.of("강원도", 3),
                        Tuples.of("충청도", 6),
                        Tuples.of("경상도", 5),
                        Tuples.of("전라도", 8),
                        Tuples.of("인천", 2),
                        Tuples.of("대전", 1),
                        Tuples.of("대구", 2),
                        Tuples.of("부산", 3),
                        Tuples.of("제주도", 0)
                ));
    }

    public static Flux<Tuple2<String, Integer>> getVoteCount(Flux<Long> source){
        return source
                .zipWith(Flux.just(
                        Tuples.of("중구", 15400),
                        Tuples.of("서초구", 20020),
                        Tuples.of("강서구", 32040),
                        Tuples.of("강동구", 14506),
                        Tuples.of("서대문구", 35650)
                )).map(Tuple2::getT2);
    }

    public void getCOVID19CountTest(){
        StepVerifier
                .withVirtualTime(() -> getCOVID19Count(
                        Flux.interval(Duration.ofHours(1)).take(1)
                ))
                .expectSubscription()
                .then(() -> VirtualTimeScheduler
                        .get()
                        .advanceTimeBy(Duration.ofHours(1)))
                .expectNextCount(11)
                .expectComplete()
                .verify();

    }

    public void getCOVID10CountTEst2(){
        StepVerifier
                .create(getCOVID19Count(
                        Flux.interval(Duration.ofMinutes(1)).take(1)
                ))
                .expectSubscription()
                .expectNextCount(11)
                .expectComplete()
//                .verify(Duration.ofSeconds(3));
                .verify(Duration.ofMinutes(3));
    }

    @Test
    void getVoteCountTest() {
        StepVerifier
                .withVirtualTime(() -> getVoteCount(
                        Flux.interval(Duration.ofMinutes(1))
                ))
                .expectSubscription()
                .expectNoEvent(Duration.ofMinutes(1))
                .expectNoEvent(Duration.ofMinutes(1))
                .expectNoEvent(Duration.ofMinutes(1))
                .expectNoEvent(Duration.ofMinutes(1))
                .expectNoEvent(Duration.ofMinutes(1))
                .expectNextCount(5)
                .expectComplete()
                .verify();
    }

    public static Flux<Integer> generateNumber(){
        return Flux
                .create(emitter -> {
                    for (int i = 0; i < 100; i++) {
                        emitter.next(i);
                    }
                    emitter.complete();
                }, FluxSink.OverflowStrategy.ERROR);
    }

    @Test
    void generateNumberTest() {
        StepVerifier
                .create(generateNumber(), 1L)
                .thenConsumeWhile(num -> num >= 1)
//                .verifyComplete();
                .expectError()
                .verifyThenAssertThat()
                .hasDroppedElements();
    }

    public static Mono<String> getSecretMessage(Mono<String> keySource){
        return keySource
                .zipWith(Mono.deferContextual(ctx ->
                        Mono.just(ctx.get("secretKey"))))
                .filter(tp ->
                        tp.getT1().equals(new String(Base64Utils.decodeFromString((String) tp.getT2()))))
                .transformDeferredContextual(
                        (mono, ctx) -> mono.map(notUse -> ctx.get("secretMessage"))
                );
    }

    @Test
    void getSecretMessageTest() {
        Mono<String> source = Mono.just("hello");

        StepVerifier
                .create(
                        getSecretMessage(source)
                                .contextWrite(context -> context.put("secretMessage", "Hello, Reactor"))
                                .contextWrite(context -> context.put("secretKey", "aGVsbG8="))
                )
                .expectSubscription()
                .expectAccessibleContext()
                .hasKey("secretKey")
                .hasKey("secretMessage")
                .then()
                .expectNext("Hello, Reactor")
                .expectComplete()
                .verify();
    }

    public static Flux<String> getCapitalizedCountry(Flux<String> source) {
        return source
                .map(country -> country.substring(0, 1).toUpperCase() + country.substring(1));
    }

    public void getCityTest(){
        StepVerifier
                .create(getCapitalizedCountry(Flux.just("korea", "england", "canada", "india")))
                .expectSubscription()
                .recordWith(ArrayList::new)
                .thenConsumeWhile(country -> !country.isEmpty())
                .expectRecordedMatches(countries ->
                        countries.stream()
                                .allMatch(country -> Character.isUpperCase(country.charAt(0))))
                .consumeRecordedWith(countries -> {
                    assertThat(countries
                            .stream()
                            .allMatch(country -> Character.isUpperCase(country.charAt(0))),
                    is(true));
                })
                .expectComplete()
                .verify();
    }

    public void divideByTwoTest2(){
        TestPublisher<Integer> source = TestPublisher.create();

        StepVerifier
                .create(divideByTwo(source.flux()))
                .expectSubscription()
                .then(() -> source.emit(2, 4, 6, 8, 10))
                .expectNext(1, 2, 3, 4)
                .expectError()
                .verify();

    }

    public void divideByTwoTest3(){
//        TestPublisher<Integer> source = TestPublisher.create();
        TestPublisher<Integer> source =
                TestPublisher.createNoncompliant(TestPublisher.Violation.ALLOW_NULL);

        StepVerifier
                .create(divideByTwo(source.flux()))
                .expectSubscription()
                .then(() -> {
                    Arrays.asList(2, 4, 6, 8, null).stream()
                            .forEach(data -> source.next(data));
                    source.complete();
                })
                .expectNext(1,2,3,4,5)
                .expectComplete()
                .verify();

    }

    public static Mono<String> processTask(Mono<String> main, Mono<String> standby){
        return main
                .flatMap(message -> Mono.just(message))
                .switchIfEmpty(standby);
    }

    public static Mono<String> supplyMainPower(){
        return Mono.empty();
    }

    public static Mono supplyStandbyPower(){
        return Mono.just("# supply Standby Power");
    }

    @Test
    void publisherProbeTEst() {
        PublisherProbe<String> probe = PublisherProbe.of(supplyStandbyPower());

        StepVerifier
                .create(processTask(supplyMainPower(), probe.mono()))
                .expectNextCount(1)
                .verifyComplete();

        probe.assertWasSubscribed();
        probe.assertWasRequested();
        probe.assertWasNotCancelled();


    }
}
