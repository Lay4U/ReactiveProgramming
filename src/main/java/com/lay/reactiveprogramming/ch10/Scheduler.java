package com.lay.reactiveprogramming.ch10;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Flow;

public class Scheduler {
    public void subscribeOn() throws InterruptedException {
        Flux.fromArray(new Integer[] {1, 3, 5, 7})
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(data -> System.out.println("doOnNext: " + data))
                .doOnSubscribe(subscription -> System.out.println("doOnSubscribe: " + subscription))
                .subscribe(data -> System.out.println("onNext: " + data));

        Thread.sleep(500L);
    }

    public void publishOn() throws InterruptedException {
        Flux.fromArray(new Integer[] {1, 3, 5, 7})
                .doOnNext(data -> System.out.println("doOnNext: " + data))
                .doOnSubscribe(subscription -> System.out.println("doOnSubscribe: " + subscription))
                .publishOn(Schedulers.parallel())
                .subscribe(data -> System.out.println("onNext: " + data));

        Thread.sleep(500L);
    }

    public void parallel() throws InterruptedException {
        Flux.fromArray(new Integer[]{1, 3, 5, 7, 9, 11, 13, 15, 17, 19})
//                .parallel()
                .parallel(4)
                .runOn(Schedulers.parallel())
                .subscribe(data -> System.out.println("onNext: " + data));

        Thread.sleep(100L);
    }

    public void doMainThread(){
        Flux.fromArray(new Integer[]{1, 3, 5, 7})
                .doOnNext(data -> System.out.println("doOnNext fromArray: " + data))
                .filter(data -> data > 3)
                .doOnNext(data -> System.out.println("doOnNext filter: " + data))
                .map(data -> data * 10)
                .doOnNext(data -> System.out.println("doOnNext map: " + data))
                .subscribe(data -> System.out.println("onNext: " + data));
    }

    public void onePublishOn() throws InterruptedException {
        Flux.fromArray(new Integer[]{1, 3, 5, 7})
                .doOnNext(data -> System.out.println("doOnNext fromArray: " + data))
                .publishOn(Schedulers.parallel())
                .filter(data -> data > 3)
                .doOnNext(data -> System.out.println("doOnNext filter: " + data))
                .map(data -> data * 10)
                .doOnNext(data -> System.out.println("doOnNext map: " + data))
                .subscribe(data -> System.out.println("onNext: " + data));

        Thread.sleep(500L);
    }

    public void twoPublishOn() {
        Flux.fromArray(new Integer[]{1, 3, 5, 7})
                .doOnNext(data -> System.out.println("doOnNext fromArray: " + data))
                .publishOn(Schedulers.parallel())
                .filter(data -> data > 3)
                .doOnNext(data -> System.out.println("doOnNext filter: " + data))
                .publishOn(Schedulers.parallel())
                .map(data -> data * 10)
                .doOnNext(data -> System.out.println("doOnNext map: " + data))
                .subscribe(data -> System.out.println("onNext: " + data));
    }

    public void subscribeOnAndPublishOn() throws InterruptedException {
        Flux.fromArray(new Integer[] {1, 3, 5, 7})
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(data -> System.out.println("doOnNext: " + data))
                .filter(data -> data > 3)
                .doOnNext(data -> System.out.println("doOnNext filter: " + data))
                .publishOn(Schedulers.parallel())
                .map(data -> data * 10)
                .doOnNext(data -> System.out.println("doOnNext map: " + data))
                .subscribe(data -> System.out.println("onNext: " + data));

        Thread.sleep(500L);
    }

    public void immediateScheduler() throws InterruptedException {
        Flux.fromArray(new Integer[]{1, 3, 5, 7})
                .publishOn(Schedulers.parallel())
                .filter(data -> data > 3)
                .doOnNext(data -> System.out.println("doOnNext filter: " + data))
                .publishOn(Schedulers.immediate())
                .map(data -> data * 10)
                .doOnNext(data -> System.out.println("doOnNext map: " + data))
                .subscribe(data -> System.out.println("onNext: " + data));

        Thread.sleep(500L);
    }

    public void singleScheduler() throws InterruptedException {
        doTask("task1")
                .subscribe(data -> System.out.println("onNext: " + data));

        doTask("task2")
                .subscribe(data -> System.out.println("onNext: " + data));

        Thread.sleep(500L);
    }

    private Flux<Integer> doTask(String taskName) {
        return Flux.fromArray(new Integer[] {1, 3, 5, 7})
                .publishOn(Schedulers.single())
                .filter(data -> data > 3)
                .doOnNext(data -> System.out.println("doOnNext filter: " + data))
                .map(data -> data * 10)
                .doOnNext(data -> System.out.println("doOnNext map: " + data));
    }

    public void newSingle() throws InterruptedException {
        doTaskNewSingle("task1")
                .subscribe(data -> System.out.println("onNext: " + data));

        doTaskNewSingle("task2")
                .subscribe(data -> System.out.println("onNext: " + data));

        Thread.sleep(500L);
    }

    private Flux<Integer> doTaskNewSingle(String taskName) {
        return Flux.fromArray(new Integer[] {1, 3, 5, 7})
                .publishOn(Schedulers.newSingle("new-single", true))
                
                .filter(data -> data > 3)
                .doOnNext(data -> System.out.println("doOnNext filter: " + data))
                .map(data -> data * 10)
                .doOnNext(data -> System.out.println("doOnNext map: " + data));
    }


}
