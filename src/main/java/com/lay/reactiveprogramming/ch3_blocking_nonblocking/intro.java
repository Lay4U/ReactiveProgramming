package com.lay.reactiveprogramming.ch3_blocking_nonblocking;

import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Flow;

public class intro {
    public static void main(String[] args) {
        List<Integer> numbers = Arrays.asList(1, 3, 21, 10, 8, 11);
        int sum = 0;
        for (Integer number : numbers) {
            if(number > 6 && (number % 2 == 0)) {
                sum += number;
            }
        }

        System.out.println("합계: " + sum);
    }
}

class Example2{
    public static void main(String[] args) {
        List<Integer> numbers = Arrays.asList(1, 3, 21, 10, 8, 11);
        int sum = numbers.stream()
                .filter(number -> number > 6 && (number % 2 != 0))
                .mapToInt(number -> number)
                .sum();

        System.out.println("합계: " + sum);

    }

    public void upstream_downstream(){
        Flux
                .just(1,2,3,4,5,6)
                .filter(n -> n % 2 == 0)
                .map(n -> n * 2)
                .subscribe(System.out::println);
    }
}
