package com.lay.reactiveprogramming.ch12;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class Debug {
    public static Map<String, String> fruits = new HashMap<>();

    static  {
        fruits.put("banana", "바나나");
        fruits.put("apple", "사과");
        fruits.put("pear", "배");
        fruits.put("grape", "포도");
    }

    public void error() throws InterruptedException {
        Hooks.onOperatorDebug();

        Flux
                .fromArray(new String[]{"BANANAS", "APPLES", "PEARS", "MELONS"})
                .subscribeOn(Schedulers.boundedElastic())
                .publishOn(Schedulers.parallel())
                .map(String::toLowerCase)
                .map(fruit -> fruit.substring(0, fruit.length() - 1))
                .map(fruits::get)
                .map(translated -> "맛있는 " + translated)
                .checkpoint()
                .checkpoint("asdfsaf", true)
                .log()
                .log("Furit", Level.FINE)
                .subscribe(
                        System.out::println,
                        error -> System.err.println("에러: " + error)
                );

        Thread.sleep(100L);
    }
}
