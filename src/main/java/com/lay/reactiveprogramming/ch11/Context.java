package com.lay.reactiveprogramming.ch11;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class Context {
    public void start() throws InterruptedException {
        Mono
                .deferContextual(ctx ->
                        Mono
                                .just("Hello" + " " + ctx.get("firstName"))
                                .doOnNext(data -> System.out.println("doOnNext: " + data))
                )
                .subscribeOn(Schedulers.boundedElastic())
                .publishOn(Schedulers.parallel())
                .transformDeferredContextual(
                        (mono, ctx) -> mono.map(data ->
                                    data + " " + ctx.get("lastName")
                                )
                )
                .contextWrite(context -> context.put("lastName", "Jobs"))
                .contextWrite(context -> context.put("firstName", "Steve"))
                .subscribe(data -> System.out.println("onNext: " + data));

        Thread.sleep(100L);
    }

    public void contextApi() throws InterruptedException {
        final String key1 = "company";
        final String key2 = "firstName";
        final String key3 = "lastName";

        Mono
                .deferContextual(ctx ->
                        Mono.just(ctx.get(key1) + ", " + ctx.get(key2) + " " + ctx.get(key3))
                )
                .publishOn(Schedulers.parallel())
                .contextWrite(context ->
                    context.putAll(
                            reactor.util.context.Context.of(key2, "Steve", key3, "Jbos").readOnly()
                    ))
                .contextWrite(context -> context.put(key1, "Apple"))
                .subscribe(data -> System.out.println("onNext: " + data));

        Thread.sleep(100L);
    }

    public void contextView() throws InterruptedException {
        final String key1 = "company";
        final String key2 = "firstName";
        final String key3 = "lastName";

        Mono
                .deferContextual(ctx ->
                        Mono.just(ctx.get(key1) + ", " +
                                ctx.getOrEmpty(key2).orElse("no firstName") + " " +
                                ctx.getOrDefault(key3, "no lastName"))
                )
                .publishOn(Schedulers.parallel())
                .contextWrite(context -> context.put(key1, "Apple"))
                .subscribe(data -> System.out.println("onNext: " + data));

        Thread.sleep(100L);
    }

}

