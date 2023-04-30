package com.lay.reactiveprogramming.ch14;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.Disposable;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;
import reactor.math.MathFlux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

import static com.lay.reactiveprogramming.ch14.SampleData.getCovidVaccines;

public class Operator {
    public void justOrEmpty() {
        Mono
                .justOrEmpty(null)
                .subscribe(data -> {
                        },
                        error -> {
                        },
                        () -> System.out.println("onComplete"));
    }

    public void fromIterable() {
        Flux
                .fromIterable(SampleData.coins)
                .subscribe(coin -> System.out.println(
                        String.format("coin 이름: {}, 현재가: {}", coin.getT1(), coin.getT2())
                ));
    }

    public void fromStream() {
        Flux
                .fromStream(() -> SampleData.coinNames.stream())
                .filter(coin -> coin.equals("BTC") || coin.equals("ETH"))
                .subscribe(data -> System.out.println("구독 데이터: " + data));
    }

    public void range() {
        Flux
                .range(5, 10)
                .subscribe(data -> System.out.println("구독 데이터: " + data));
    }

    public void range2() {
        Flux
                .range(7, 5)
                .map(idx -> SampleData.btcTopPricesPerYear.get(idx))
                .subscribe(tuple -> System.out.println(
                        String.format("연도: %d, 최고가: %d", tuple.getT1(), tuple.getT2())
                ));
    }

    public void defer() throws InterruptedException {
        System.out.println("현재 시간: " + System.currentTimeMillis());
        Mono<LocalDateTime> justMono = Mono.just(LocalDateTime.now());
        Mono<LocalDateTime> deferMono = Mono.defer(() -> Mono.just(LocalDateTime.now()));

        Thread.sleep(2000);

        justMono.subscribe(Data -> System.out.println("구독 데이터: " + Data + ", 시간: " + System.currentTimeMillis()));
        deferMono.subscribe(Data -> System.out.println("구독 데이터: " + Data + ", 시간: " + System.currentTimeMillis()));

        Thread.sleep(3000);

        justMono.subscribe(Data -> System.out.println("구독 데이터: " + Data + ", 시간: " + System.currentTimeMillis()));
        deferMono.subscribe(Data -> System.out.println("구독 데이터: " + Data + ", 시간: " + System.currentTimeMillis()));
    }

    public void defer2() throws InterruptedException {
        System.out.println("현재 시간: " + System.currentTimeMillis());
        Mono
                .just("Hello")
                .delayElement(Duration.ofSeconds(3))
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println("say Hi");
                    return Mono.just("Hi");
                }))
                .subscribe(data -> System.out.println("구독 데이터: " + data + ", 시간: " + System.currentTimeMillis()
                ));
        Thread.sleep(3500);
    }

    public void generate() {
        Flux.
                generate(() -> 0, (state, sink) -> {
                    sink.next(state);
                    if (state == 10)
                        sink.complete();
                    return ++state;
                })
                .subscribe(data -> System.out.println("구독 데이터: " + data));
    }

    public void generate2() {
        final int dan = 3;
        Flux
                .generate(() -> Tuples.of(dan, 1), (state, sink) -> {
                    sink.next(state.getT1() + " * " + state.getT2()
                            + " = " + (state.getT1() * state.getT2()));
                    if (state.getT2() == 9)
                        sink.complete();
                    return Tuples.of(state.getT1(), state.getT2() + 1);
                }, state -> System.out.println("최종 상태: " + state))
                .subscribe(data -> System.out.println("구독 데이터: " + data));
    }

    public void generate3() {
        Map<Integer, Tuple2<Integer, Long>> map = SampleData.getBtcTopPricesPerYearMap();
        Flux
                .generate(() -> 2019, (state, sink) -> {
                    if (state > 2021) {
                        sink.complete();
                    } else {
                        sink.next(map.get(state));
                    }

                    return ++state;
                })
                .subscribe(data -> System.out.println("구독 데이터: " + data));
    }

    static int SIZE = 0;
    static int COUNT = -1;

    public void create() {

        final List<Integer> DATA_SOURCE = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        System.out.println("현재 시간: " + System.currentTimeMillis());
        Flux.create((FluxSink<Integer> sink) -> {
            sink.onRequest(n -> {
                try {
                    Thread.sleep(1000L);
                    for (int i = 0; i < n; i++) {
                        if (COUNT > +9) {
                            sink.complete();
                        } else {
                            COUNT++;
                            sink.next(DATA_SOURCE.get(COUNT));
                        }
                    }
                } catch (InterruptedException e) {
                }
            });
            sink.onDispose(() -> System.out.println("구독 해제"));
        }).subscribe(new BaseSubscriber<>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                request(2);
            }

            @Override
            protected void hookOnNext(Integer value) {
                SIZE++;
                System.out.println("구독 데이터: " + value + ", SIZE: " + SIZE);
                if (SIZE == 2) {
                    request(2);
                    SIZE = 0;
                }
            }

            @Override
            protected void hookOnComplete() {
                System.out.println("onComplete");
            }
        });
    }

    public void create2() throws InterruptedException {
        CryptoCurrencyPriceEmitter priceEmitter = new CryptoCurrencyPriceEmitter();

        Flux.create((FluxSink<Integer> sink) ->
                        priceEmitter.setListener(new CryptoCurrencyPriceListener() {
                            @Override
                            public void onPrice(List<Integer> priceList) {
                                priceList.stream()
                                        .forEach(price -> sink.next(price));
                            }

                            @Override
                            public void onComplete() {
                                sink.complete();
                            }
                        }))
                .publishOn(Schedulers.parallel())
                .subscribe(
                        data -> System.out.println("구독 데이터: " + data),
                        error -> System.out.println("에러: " + error),
                        () -> System.out.println("onComplete")
                );
        Thread.sleep(3000L);

        priceEmitter.flowInto();

        Thread.sleep(2000L);
        priceEmitter.complete();
    }

    static int start = 1;
    static int end = 4;

    public void create3() throws InterruptedException {
        Flux.create((FluxSink<Integer> sink) -> {
                    sink.onRequest(n -> {
                        System.out.println("request: " + n);
                        try {
                            Thread.sleep(500L);
                            for (int i = start; i < end; i++) {
                                sink.next(i);
                            }
                            start += 4;
                            end += 4;
                        } catch (InterruptedException e) {
                        }
                    });

                    sink.onDispose(() -> {
                        System.out.println("구독 해제");
                    });
                }, FluxSink.OverflowStrategy.DROP)
                .subscribeOn(Schedulers.boundedElastic())
                .publishOn(Schedulers.parallel(), 2)
                .subscribe(data -> System.out.println("구독 데이터: " + data));
        Thread.sleep(3000L);
    }

    public void filter() {
        Flux
                .range(1, 20)
                .filter(num -> num % 2 != 0)
                .subscribe(data -> System.out.println("구독 데이터: " + data));
    }

    public void filter2() {
        Flux
                .fromIterable(SampleData.btcTopPricesPerYear)
                .filter(tuple -> tuple.getT2() > 20_000_000)
                .subscribe(data -> System.out.println("구독 데이터: " + data.getT1() + " - " + data.getT2()));
    }

    public void filter3() throws InterruptedException {
        Map<SampleData.CovidVaccine, Tuple2<SampleData.CovidVaccine, Integer>> vaccineMap = getCovidVaccines();
        Flux
                .fromIterable(SampleData.coronaVaccineNames)
                .filterWhen(vaccine -> Mono
                        .just(vaccineMap.get(vaccine).getT2() >= 30_000_000))
                .publishOn(Schedulers.parallel())
                .subscribe(data -> System.out.println("구독 데이터: " + data));

        Thread.sleep(1000L);
    }

    public void skip() throws InterruptedException {
        Flux
                .interval(Duration.ofSeconds(1))
                .skip(2)
                .subscribe(data -> System.out.println("구독 데이터: " + data));

        Thread.sleep(5500L);
    }

    public void skip2() throws InterruptedException {
        Flux
                .interval(Duration.ofMillis(300))
                .skip(Duration.ofSeconds(1))
                .subscribe(data -> System.out.println("구독 데이터: " + data));

        Thread.sleep(2000L);
    }

    public void skip3() {
        Flux
                .fromIterable(SampleData.btcTopPricesPerYear)
                .filter(tuple -> tuple.getT2() >= 20_000_000)
                .skip(2)
                .subscribe(tuple -> System.out.println("구독 데이터: " + tuple.getT1() + " - " + tuple.getT2()));

    }

    public void take() throws InterruptedException {
        Flux
                .interval(Duration.ofSeconds(1))
                .take(3)
                .subscribe(data -> System.out.println("구독 데이터: " + data));

        Thread.sleep(4000L);
    }

    public void take2() throws InterruptedException {
        Flux
                .interval(Duration.ofSeconds(1))
                .take(Duration.ofMillis(2500))
                .subscribe(data -> System.out.println("구독 데이터: " + data));

        Thread.sleep(2000L);
    }

    public void takeLast() {
        Flux
                .fromIterable(SampleData.btcTopPricesPerYear)
                .takeUntil(tuple -> tuple.getT2() >= 20_000_000)
                .subscribe(tuple -> System.out.println("구독 데이터: " + tuple.getT1() + " - " + tuple.getT2()));
    }

    public void next() {
        Flux
                .fromIterable(SampleData.btcTopPricesPerYear)
                .next()
                .subscribe(tuple -> System.out.println("구독 데이터: " + tuple.getT1() + " - " + tuple.getT2()));

    }

    public void Map() {
        Flux
                .just("1-Circle", "3-Circle", "5-Circle")
                .map(circle -> circle.replace("Circle", "Reactangle"))
                .subscribe(data -> System.out.println("data = " + data));
    }

    public void map2() {
        final double buyPrice = 50_000_000;
        Flux
                .fromIterable(SampleData.btcTopPricesPerYear)
                .filter(tuple -> tuple.getT1() == 2021)
                .doOnNext(data -> System.out.println("doOnNext: " + data.getT1() + " - " + data.getT2()))
                .map(tuple -> calculateProfitRate(buyPrice, tuple.getT2()))
                .subscribe(data -> System.out.println("구독 데이터: " + data));
    }

    public static double calculateProfitRate(final double buyPrice, final Long topPrice) {
        return (topPrice - buyPrice) / buyPrice * 100;
    }

    public void flatMap() {
        Flux
                .just("Good", "Bad")
                .flatMap(feeling -> Flux
                        .just("Morning", "Afternoon", "Evening")
                        .map(time -> feeling + " " + time))
                .subscribe(System.out::println);

    }

    public void flatMap2() {
        Flux
                .range(2, 8)
                .flatMap(dan -> Flux
                        .range(1, 9)
                        .publishOn(Schedulers.parallel())
                        .map(n -> dan + " * " + n + " = " + dan * n))
                .subscribe(System.out::println);
    }

    public void concat() {
        Flux
                .concat(Flux.just(1, 2, 3), Flux.just(4, 5))
                .subscribe(data -> System.out.println("구독 데이터: " + data));
    }

    public void concat2() {
        Flux
                .concat(
                        Flux.fromIterable(getViralVector()),
                        Flux.fromIterable(getMRNA()),
                        Flux.fromIterable(getSubunit())
                )
                .subscribe(data -> System.out.println("구독 데이터: " + data));
    }

    public List<Tuple2<SampleData.CovidVaccine, Integer>> getViralVector() {
        return SampleData.viralVectorVaccines;
    }

    public List<Tuple2<SampleData.CovidVaccine, Integer>> getMRNA() {
        return SampleData.mRNAVaccines;
    }

    public List<Tuple2<SampleData.CovidVaccine, Integer>> getSubunit() {
        return SampleData.subunitVaccines;
    }

    public void merge() throws InterruptedException {
        Flux
                .merge(Flux.just(1, 2, 3, 4).delayElements(Duration.ofMillis(300L)),
                        Flux.just(5, 6, 7, 8).delayElements(Duration.ofMillis(500L)))
                .subscribe(data -> System.out.println("구독 데이터: " + data));

        Thread.sleep(2000L);
    }

    public void merge2() {
        String[] usaStates = {
                "Alaska", "Alabama", "Arkansas", "Arizona", "California", "Colorado", "Connecticut", "Delaware",
                "Florida", "Georgia", "Hawaii", "Iowa", "Idaho", "Illinois", "Indiana", "Kansas", "Kentucky",
                "Louisiana", "Massachusetts", "Maryland", "Maine", "Michigan", "Minnesota", "Missouri", "Mississippi",
                "Montana", "North Carolina", "North Dakota", "Nebraska", "New Hampshire", "New Jersey", "New Mexico",
                "Nevada", "New York", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina",
                "South Dakota", "Tennessee", "Texas", "Utah", "Virginia", "Vermont", "Washington", "Wisconsin",
                "West Virginia", "Wyoming"
        };

        Flux
                .merge(getMeltDownRecoveryMessage(usaStates))
                .subscribe(data -> System.out.println("구독 데이터: " + data));
    }

    private List<Mono<String>> getMeltDownRecoveryMessage(String[] usaStates) {
        List<Mono<String>> messages = new ArrayList<>();
        for (String state : usaStates) {
            messages.add(SampleData.nppMap.get(state));
        }
        return messages;
    }

    public void zip() throws InterruptedException {
        Flux
                .zip(
                        Flux.just(1, 2, 3).delayElements(Duration.ofMillis(300L)),
                        Flux.just(4, 5, 6).delayElements(Duration.ofMillis(500L))
                )
                .subscribe(tuple2 -> System.out.println("구독 데이터: " + tuple2.getT1() + " - " + tuple2.getT2()));

        Thread.sleep(2500L);
    }

    public void zip2() throws InterruptedException {
        Flux
                .zip(
                        Flux.just(1, 2, 3).delayElements(Duration.ofMillis(300L)),
                        Flux.just(4, 5, 6).delayElements(Duration.ofMillis(500L)),
                        (n1, n2) -> n1 * n2
                )
                .subscribe(data -> System.out.println("구독 데이터: " + data));

        Thread.sleep(2500L);
    }

    public void zip3() {
        int start = 10;
        int end = 20;

        Flux.zip(
                        Flux.fromIterable(SampleData.seoulInfected)
                                .filter(t2 -> t2.getT1() >= start && t2.getT1() <= end),
                        Flux.fromIterable(SampleData.incheonInfected)
                                .filter(t2 -> t2.getT1() >= start && t2.getT1() <= end),
                        Flux.fromIterable(SampleData.suwonInfected)
                                .filter(t2 -> t2.getT1() >= start && t2.getT2() <= end)
                )
                .subscribe(tuples -> {
                    Tuple3<Tuple2, Tuple2, Tuple2> t3 = (Tuple3) tuples;
                    int sum = (int) t3.getT1().getT2() +
                            (int) t3.getT2().getT2() +
                            (int) t3.getT3().getT2();
                    System.out.println("구독 데이터: " + t3.getT1().getT1() + " - " + sum);
                });
    }

    public void add() throws InterruptedException {
        Mono
                .just("Task 1")
                .delayElement(Duration.ofSeconds(1))
                .doOnNext(data -> System.out.println("doOnNext: " + data))
                .and(
                        Flux.just("Task 2", "Task 3")
                                .delayElements(Duration.ofMillis(600))
                                .doOnNext(data -> System.out.println("doOnNext: " + data))
                )
                .subscribe(
                        data -> System.out.println("구독 데이터: " + data),
                        error -> System.out.println("에러: " + error),
                        () -> System.out.println("종료")
                );
        Thread.sleep(5000L);
    }

    public void add2() {
        restartApplicationServer()
                .and(restartDBServer())
                .subscribe(
                        data -> System.out.println("구독 데이터: " + data),
                        error -> System.out.println("에러: " + error),
                        () -> System.out.println("종료")
                );
    }

    private Mono<String> restartApplicationServer() {
        return Mono
                .just("Application Server was restarted succesfully.")
                .delayElement(Duration.ofSeconds(2))
                .doOnNext(data -> System.out.println("doOnNext: " + data));
    }

    private Publisher<String> restartDBServer() {
        return Mono
                .just("DB server was restarted successfully.")
                .delayElement(Duration.ofSeconds(4))
                .doOnNext(data -> System.out.println("doOnNext: " + data));
    }

    public void collectionList() {
        Flux
                .just("...", "---", "...")
                .map(code -> SampleData.morseCodeMap.get(code))
                .collectList()
                .subscribe(list -> System.out.println(list.stream().collect(Collectors.joining())));

    }

    public void collectMap() {
        Flux
                .range(0, 26)
                .collectMap(key -> SampleData.morseCodes[key],
                        value -> Character.toString((char) ('a' + value)))
                .subscribe(map -> System.out.println(map));
    }

    public void doOn() {
        Flux.range(1, 5)
                .doFinally(singalType -> System.out.println("doFinally: " + singalType))
                .doFinally(signalType -> System.out.println("doFinally: " + signalType))
                .doOnNext(data -> System.out.println("doOnNext: " + data))
                .doOnRequest(data -> System.out.println("doOnRequest: " + data))
                .doOnSubscribe(subscription -> System.out.println("doOnSubscribe: " + subscription))
                .doFirst(() -> System.out.println("doFirst"))
                .filter(num -> num % 2 == 1)
                .doOnNext(data -> System.out.println("doOnNext: " + data))
                .doOnComplete(() -> System.out.println("doOnComplete"))
                .doOnTerminate(() -> System.out.println("doOnTerminate"))
                .doAfterTerminate(() -> System.out.println("doAfterTerminate"))
                .doOnCancel(() -> System.out.println("doOnCancel"))
                .doFinally(signalType -> System.out.println("doFinally: " + signalType))
                .subscribe(new BaseSubscriber<>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        request(1);
                    }

                    @Override
                    protected void hookOnNext(Integer value) {
                        System.out.println("hookOnNext: " + value);
                        request(1);
                    }
                });
    }

    public void error() {
        Flux
                .range(1, 5)
                .flatMap(num -> {
                    if ((num * 2) % 3 == 0) {
                        return Flux.error(new IllegalArgumentException());
                    } else {
                        return Mono.just(num * 2);
                    }
                })
                .subscribe(data -> System.out.println("구독 데이터: " + data),
                        error -> System.out.println("에러: " + error));

    }

    public void error2() {
        Flux
                .just('a', 'b', 'c', '3', 'd')
                .flatMap(letter -> {
                    try {
                        return convert(letter);
                    } catch (DataFormatException e) {
                        return Flux.error(e);
                    }
                })
                .subscribe(
                        data -> System.out.println("구독 데이터: " + data),
                        error -> System.out.println("에러: " + error)
                );

    }

    public Mono<String> convert(char ch) throws DataFormatException {
        if (!Character.isAlphabetic(ch)) {
            throw new DataFormatException("알파벳이 아닙니다.");
        }
        return Mono.just("Converted to" + Character.toUpperCase(ch));
    }

    public void onErrorReturn() {
        Flux
                .fromIterable(SampleData.books)
                .map(book -> book.getPenName().toUpperCase())
                .onErrorReturn("No pen name")
                .subscribe(data -> System.out.println("구독 데이터: " + data));
    }

    public void onErrorReturn2() {
        Flux
                .fromIterable(SampleData.books)
                .map(book -> book.getPenName().toUpperCase())
                .onErrorReturn(NullPointerException.class, "no pen name")
                .onErrorReturn(IllegalFormatException.class, "Illegal pen name")
                .subscribe(data -> System.out.println("구독 데이터: " + data));
    }

    public void onErrorResume() {
        final String keyword = "DDD";
        Flux
                .fromIterable(SampleData.books)
                .filter(book -> book.getBookName().contains(keyword))
                .switchIfEmpty(Flux.error(new NoSuchBookException("No such Book")))
                .onErrorResume(error -> {
                    ArrayList<Book> books = new ArrayList<>(SampleData.books);
                    books.add(new Book("DDD: Domain Driven Design",
                            "Joy", "ddd-man", 35000, 200));
                    return Flux.fromIterable(books)
                            .filter(book -> book.getBookName().contains(keyword))
                            .switchIfEmpty(Flux.error(new NoSuchBookException("No such Book")));
                })
                .subscribe(data -> System.out.println("구독 데이터: " + data),
                        error -> System.out.println("에러: " + error));

    }

    public static class NoSuchBookException extends RuntimeException {
        NoSuchBookException(String message) {
            super(message);
        }
    }

    public void onErrorContinue() {
        Flux
                .just(1, 2, 4, 0, 6, 12)
                .map(num -> 12 / num)
                .onErrorContinue((error, num) ->
                        System.out.println("에러: " + error))
                .subscribe(data -> System.out.println("구독 데이터: " + data),
                        error -> System.out.println("에러: " + error));
    }

    public void retry() throws InterruptedException {
        final int[] count = {1};
        Flux
                .range(1, 3)
                .delayElements(Duration.ofSeconds(1))
                .map(num -> {
                    try {
                        if (num == 3 && count[0] == 1) {
                            count[0]++;
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                    }
                    return num;
                })
                .timeout(Duration.ofMillis(1500))
                .retry(1)
                .subscribe(data -> System.out.println("구독 데이터: " + data),
                        error -> System.out.println("에러: " + error),
                        () -> System.out.println("종료"));
        Thread.sleep(7000);
    }

    public void retry2() throws InterruptedException {
        getBooks()
                .collect(Collectors.toSet())
                .subscribe(bookSet -> bookSet.stream()
                                .forEach(book -> System.out.println("책 제목: " + book.getBookName())),
                        error -> System.out.println("에러: " + error));
        Thread.sleep(12000);
    }

    private Flux<Book> getBooks() {
        final int[] count = {0};
        return Flux
                .fromIterable(SampleData.books)
                .delayElements(Duration.ofMillis(500))

                .map(book -> {
                    try {
                        count[0]++;
                        if (count[0] == 3) {
                            Thread.sleep(2000);
                        }
                    } catch (InterruptedException e) {
                    }
                    return book;
                })
                .timeout(Duration.ofSeconds(2))
                .retry(1)
                .doOnNext(book -> System.out.println("책 제목: " + book.getBookName()));
    }

    public void elapsed() throws InterruptedException {
        Flux
                .range(1, 5)
                .delayElements(Duration.ofSeconds(1))
                .elapsed()
                .subscribe(data -> System.out.println("구독 데이터: " + data));
        Thread.sleep(6000);
    }

    public void elapsed2() {
        URI worldTimeUri = UriComponentsBuilder.newInstance().scheme("http")
                .host("worldtimeapi.org")
                .port(80)
                .path("/api/timezone/Asia/Seoul")
                .build()
                .encode()
                .toUri();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));


        Mono.defer(() -> Mono.just(
                        restTemplate.exchange(worldTimeUri,
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                String.class)
                ))
                .repeat(4)
                .elapsed()
                .map(response -> {
                    DocumentContext jsonContext = JsonPath.parse(response.getT2().getBody());
                    String dateTime = jsonContext.read("$.datetime");
                    return Tuples.of(dateTime, response.getT1());
                })
                .subscribe(
                        data -> System.out.println("구독 데이터: " + data),
                        error -> System.out.println("에러: " + error)
                );
    }

    public void window() {
        Flux.range(1, 11)
                .window(3)
                .flatMap(flux -> {
                    System.out.println("==============");
                    return flux;
                })
                .subscribe(new BaseSubscriber<>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        subscription.request(2);
                    }

                    @Override
                    protected void hookOnNext(Integer value) {
                        System.out.println("구독 데이터: " + value);
                        request(2);
                    }
                });
    }

    public void window2() {
        Flux.fromIterable(SampleData.monthlyBookSales2021)
                .window(3)
                .flatMap(flux -> MathFlux.sumInt(flux))
                .subscribe(new BaseSubscriber<>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        subscription.request(2);
                    }

                    @Override
                    protected void hookOnNext(Integer value) {
                        System.out.println("구독 데이터: " + value);
                        request(2);
                    }
                });
    }

    public void buffer() {
        Flux.range(1, 95)
                .buffer(10)
                .subscribe(buffer -> System.out.println("구독 데이터: " + buffer));
    }

    public void bufferTimeout() {
        Flux
                .range(1, 20)
                .map(num -> {
                    try {
                        if (num < 10) {
                            Thread.sleep(100L);
                        } else {
                            Thread.sleep(300L);
                        }
                    } catch (InterruptedException e) {
                    }
                    return num;
                })
                .bufferTimeout(3, Duration.ofMillis(400L))
                .subscribe(buffer -> System.out.println("구독 데이터: " + buffer));
    }

    public void groupBy() {
        Flux.fromIterable(SampleData.books)
                .groupBy(book -> book.getAuthorName())
                .flatMap(groupedFlux ->
                        groupedFlux
                                .map(book -> book.getBookName() + "(" + book.getAuthorName() + ")")
                                .collectList())
                .subscribe(data -> System.out.println("구독 데이터: " + data));

    }

    public void groupBy2() {
        Flux.fromIterable(SampleData.books)
                .groupBy(book -> book.getAuthorName(),
                        book -> book.getBookName() + "(" + book.getAuthorName() + ")")
                .flatMap(groupedFlux -> groupedFlux.collectList())
                .subscribe(data -> System.out.println("구독 데이터: " + data));
    }

    public void groupBy3() {
        Flux.fromIterable(SampleData.books)
                .groupBy(book -> book.getAuthorName())
                .flatMap(groupedFlux ->
                        Mono.just(groupedFlux.key())
                                .zipWith(
                                        groupedFlux.map(book ->
                                                        (int) (book.getPrice() * book.getStockQuantity() * 0.1))
                                                .reduce((y1, y2) -> y1 + y2),
                                        (authorName, sumRoyalty) -> authorName + "'s royalty: " + sumRoyalty)
                )
                .subscribe(data -> System.out.println("구독 데이터: " + data));
    }

    public void publish() throws InterruptedException {
        ConnectableFlux<Integer> flux = Flux
                .range(1, 5)
                .delayElements(Duration.ofMillis(300L))
                .publish();

        Thread.sleep(500L);
        flux.subscribe(data -> System.out.println("구독자 1: " + data));

        Thread.sleep(200L);
        flux.subscribe(data -> System.out.println("구독자 2: " + data));

        flux.connect();

        Thread.sleep(1000L);
        flux.subscribe(data -> System.out.println("구독자 3: " + data));

        Thread.sleep(2000L);
    }

    private static ConnectableFlux<String> publisher;
    public static int checkedAudience;
    static {
        publisher = Flux.just("Concert part1", "Concert par2", "Concert part3")
                .delayElements(Duration.ofMillis(300L))
                .publish();
    }
    public void publish2() throws InterruptedException {
        if (checkedAudience >= 2) {
            publisher.connect();
        }
        Thread.sleep(500L);
        publisher.subscribe(data -> System.out.println("구독자 1: " + data));
        checkedAudience++;

        Thread.sleep(500L);
        publisher.subscribe(data -> System.out.println("구독자 2: " + data));
        checkedAudience++;

        if (checkedAudience >= 2) {
            publisher.connect();
        }

        Thread.sleep(500L);
        publisher.subscribe(data -> System.out.println("구독자 3: " + data));

        Thread.sleep(1000L);
    }

    public void autoConnect() throws InterruptedException {
        Flux<String> publisher = Flux
                .just("Concert part1", "Concert par2", "Concert part3")
                .delayElements(Duration.ofMillis(300L))
                .publish()
                .autoConnect(2);


        Thread.sleep(500L);
        publisher.subscribe(data -> System.out.println("구독자 1: " + data));

        Thread.sleep(500L);
        publisher.subscribe(data -> System.out.println("구독자 2: " + data));

        Thread.sleep(500L);
        publisher.subscribe(data -> System.out.println("구독자 3: " + data));

        Thread.sleep(1000L);
    }

    public void refCount() throws InterruptedException {
        Flux<Long> publisher = Flux
                .interval(Duration.ofMillis(500))
                .publish().refCount(1);
        Disposable disposable = publisher.subscribe(data -> System.out.println("구독자 1: " + data));

        Thread.sleep(2100L);

        publisher.subscribe(data -> System.out.println("구독자 2: " + data));

        Thread.sleep(2500L);
    }



}


