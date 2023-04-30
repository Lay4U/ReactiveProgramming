package com.lay.reactiveprogramming.ch4;

import io.netty.util.internal.StringUtil;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FunctionalInterface {
    public static void main(String[] args) {
        List<CryptoCurrency> cryptoCurrencies = SampleData.cryptoCurrencies;

        Collections.sort(cryptoCurrencies, new Comparator<CryptoCurrency>() {
            @Override
            public int compare(CryptoCurrency o1, CryptoCurrency o2) {
                return o1.getUnit().name().compareTo(o2.getUnit().name());
            }
        });

        for (CryptoCurrency cryptoCurrency : cryptoCurrencies) {

            System.out.println("암호 화폐명: " + cryptoCurrency.getName()
            + ", 가격: " + cryptoCurrency.getUnit());
        }
    }

    public void mainToLambda(){
        List<CryptoCurrency> cryptoCurrencies = SampleData.cryptoCurrencies;

        Collections.sort(cryptoCurrencies,
                Comparator.comparing(cc -> cc.getUnit().name()));

        for (CryptoCurrency cryptoCurrency : cryptoCurrencies) {
                System.out.println("암호 화폐명: " + cryptoCurrency.getName()
                        + ", 가격: " + cryptoCurrency.getUnit());
        }
    }

    public void lambdaCapturing(){
        List<CryptoCurrency> cryptoCurrencies = SampleData.cryptoCurrencies;

        String korBTC = "비트코인";
        cryptoCurrencies.stream()
                .filter(cc -> cc.getUnit() == CryptoCurrency.CurrencyUnit.BTC)
                .map(cc -> cc.getName() + "(" + korBTC + ")")
                .forEach(System.out::println);
    }

    public void MethodReference(){
        List<CryptoCurrency> cryptoCurrencies = SampleData.cryptoCurrencies;

        cryptoCurrencies.stream()
                .filter(cc -> cc.getUnit() == CryptoCurrency.CurrencyUnit.BTC)
//                .map(cc -> cc.getName())
                .map(CryptoCurrency::getName)
//                .map(name -> name.toUpperCase())
                .map(String::toUpperCase)
//                .map(name -> StringUtils.trimAllWhitespace(name))
                .map(StringUtils::trimAllWhitespace)
//                .forEach(name -> System.out.println(name));
                .forEach(System.out::println);

    }

    public void Predicate(){
        List<CryptoCurrency> cryptoCurrencies = SampleData.cryptoCurrencies;

        List<CryptoCurrency> result =
                filter(cryptoCurrencies, cc -> cc.getPrice() > 500_000);

        for (CryptoCurrency cryptoCurrency : result) {
            System.out.println(cryptoCurrency.getName());
        }


    }

    private List<CryptoCurrency> filter(List<CryptoCurrency> cryptoCurrencies,
                                        Predicate<CryptoCurrency> p){
        List<CryptoCurrency> result = new ArrayList<>();
        for (CryptoCurrency cc : result) {
            if(p.test(cc)){
                result.add(cc);
            }
        }

        return result;
    }

    public void Consumer(){
        List<CryptoCurrency> cryptoCurrencies = SampleData.cryptoCurrencies;
        List<CryptoCurrency> filtered = filter(cryptoCurrencies,
                cc -> cc.getUnit() == CryptoCurrency.CurrencyUnit.BTC ||
                        cc.getUnit() == CryptoCurrency.CurrencyUnit.ETH);

        addBookmark(filtered, this::saveBookMark);
    }

    private void addBookmark(List<CryptoCurrency> cryptoCurrencies,
                             Consumer<CryptoCurrency> consumer){
        for (CryptoCurrency cc : cryptoCurrencies) {
            consumer.accept(cc);
        }
    }

    private void saveBookMark(CryptoCurrency cryptoCurrency){
        System.out.println("# Save " + cryptoCurrency.getUnit());
    }

    public void Function(){
        List<CryptoCurrency> cryptoCurrencies = SampleData.cryptoCurrencies;
        List<CryptoCurrency> filtered = filter(cryptoCurrencies,
                cc -> cc.getUnit() == CryptoCurrency.CurrencyUnit.BTC ||
                        cc.getUnit() == CryptoCurrency.CurrencyUnit.ETH);

        calculatePayment(filtered, cc -> cc.getPrice() * 2);
    }

    public int calculatePayment(List<CryptoCurrency> cryptoCurrencies,
                                Function<CryptoCurrency, Integer> function){
        int totalPayment = 0;
        for (CryptoCurrency cc : cryptoCurrencies) {
            totalPayment += function.apply(cc);
        }

        return totalPayment;
    }

    public void Supplier(){
        String mnemonic = createMnemonic();
        System.out.println("mnemonic = " + mnemonic);
    }

    public String createMnemonic() {
        return Stream
                .generate(this::getMnemonic)
                .limit(12)
                .collect(Collectors.joining(" "));
    }

    public String getMnemonic() {
        List<String>mnemonic = Arrays.asList(
                "alpha", "bravo", "charlie", "delta", "echo", "foxtrot",
                "golf", "hotel", "india", "juliet", "kilo", "lima",
                "mike", "november", "oscar", "papa", "quebec", "romeo",
                "sierra", "tango", "uniform", "victor", "whiskey", "x-ray",
                "yankee", "zulu"
        );

        Collections.shuffle(mnemonic);
        return mnemonic.get(0);
    }



}
