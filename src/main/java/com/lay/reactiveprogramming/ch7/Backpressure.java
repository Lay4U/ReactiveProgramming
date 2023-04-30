package com.lay.reactiveprogramming.ch7;

import lombok.SneakyThrows;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

public class Backpressure {
    public void start() {
        Flux.range(1, 5)
                .doOnRequest(data -> System.out.println("data = " + data))
                .subscribe(new BaseSubscriber<Integer>() {

                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        request(1);
                    }

                    @SneakyThrows
                    @Override
                    protected void hookOnNext(Integer value) {
                        Thread.sleep(2000L);
                        System.out.println("value = " + value);
                        request(1);
                    }
                });
    }

    public static void starategyOfBackpressure() throws InterruptedException {
        Flux
                .interval(Duration.ofMillis(1L))
//                .onBackpressureError()
//                .onBackpressureLatest()
//                .onBackpressureDrop(dropped -> System.out.println("dropped = " + dropped))
//                .doOnNext(data -> System.out.println("# doOnNext: " + data))

                .onBackpressureBuffer(2,
                        dropped -> System.out.println("dropped = " + dropped),
                        BufferOverflowStrategy.DROP_LATEST
                        )
                .doOnNext(data -> System.out.println("# doOnNext: " + data))
                .publishOn(Schedulers.parallel())
                .subscribe(data -> {
                            try {
                                Thread.sleep(5L);
                            } catch (InterruptedException e) {
                            }
                            System.out.println("onNext: " + data);
                        },
                        error -> System.out.println("error = " + error)
                );
        Thread.sleep(2000L);
    }

    public static void main(String[] args) throws InterruptedException {
        starategyOfBackpressure();
    }
}
