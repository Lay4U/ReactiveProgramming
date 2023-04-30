package com.lay.reactiveprogramming.ch8;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.stream.IntStream;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;

public class Sink {
    public void start() throws InterruptedException {
        int tasks = 6;
        Flux
                .create((FluxSink<String> sink) -> {
                    IntStream
                            .range(1, tasks)
                            .forEach(n -> sink.next(doTask(n)));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(n -> System.out.println("create: " + n))
                .publishOn(Schedulers.parallel())
                .map(result -> result + " success!")
                .doOnNext(n -> System.out.println("map: " + n))
                .publishOn(Schedulers.parallel())
                .subscribe(data -> System.out.println("onNext = " + data));

        Thread.sleep(500L);
    }

    public String doTask(int taskNumber){
        return "task " + taskNumber + " result";
    }

    public void sink(){
        int tasks = 6;

        Sinks.Many<String> unicastSink = Sinks.many().unicast()
                .onBackpressureBuffer();
        Flux<String> fluxView = unicastSink.asFlux();

        IntStream
                .range(1, tasks)
                .forEach(n -> {
                    try {
                        new Thread(() -> {
                            unicastSink.emitNext(doTask(n), FAIL_FAST);
                            System.out.println("emitted: " + n);
                        }).start();
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        System.out.println("e = " + e);
                    }
                });

    }

    public void sinkOne(){
        Sinks.One<String> sinkOne = Sinks.one();
        Mono<String> mono = sinkOne.asMono();

        sinkOne.emitValue("Hello REactor", FAIL_FAST);
        sinkOne.emitValue("Hi Reactor", FAIL_FAST);
        mono.subscribe(data -> System.out.println("data1 = " + data));
        mono.subscribe(data -> System.out.println("data2 = " + data));
    }

    public void sinkMany(){
        Sinks.Many<Integer> unicastSink = Sinks.many().unicast()
                .onBackpressureBuffer();

        Sinks.Many<Integer> multicastSink = Sinks.many().multicast()
                .onBackpressureBuffer();

        Sinks.Many<Integer> replaySink = Sinks.many().replay().limit(2);


        Flux<Integer> fluxView = unicastSink.asFlux();

        unicastSink.emitNext(1, FAIL_FAST);
        unicastSink.emitNext(2, FAIL_FAST);

        fluxView.subscribe(data -> System.out.println("data1 = " + data));
        unicastSink.emitNext(3, FAIL_FAST);
//        fluxView.subscribe(data -> System.out.println("data2 = " + data));
    }




}
