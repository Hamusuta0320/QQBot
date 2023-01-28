package com.cxj;

import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.function.*;
import java.util.stream.Stream;

class User implements Flow.Subscriber<String> {

    private Flow.Subscription subscription;


    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
    }

    public void getData() {
        this.subscription.request(100);
    }

    @Override
    public void onNext(String item) {
        System.out.println(item);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {
        System.out.println("结束");
    }
}

class Money implements Flow.Publisher<String>, Flow.Subscription {

    private final Set<Flow.Subscriber<? super String>> subscribers = new HashSet<>();

    @Override
    public void subscribe(Flow.Subscriber<? super String> subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public void request(long n) {
        System.out.println(n);
        for(int i=0;i<n;i++) {
            int finalI = i;
            subscribers.forEach(subscriber -> {
                subscriber.onNext(String.valueOf(finalI));
            });
        }
        subscribers.forEach(Flow.Subscriber::onComplete);
    }

    @Override
    public void cancel() {

    }
}

class A {
    public A() {
        System.out.println("A init");
    }

    private static final class InstanceHolder {
        private static final A instance = new A();
    }

    public static A getInstance() {
        return InstanceHolder.instance;
    }
}

public class MyTest {

    @Test
    public void testFlow() {
        User u = new User();
        Money money = new Money();
        u.onSubscribe(money);
        money.subscribe(u);
        u.getData();
    }

    @Test
    public void testSingleton() {
//        A.getInstance();
    }

    @Test
    public void t1() {
        BiConsumer<Integer, String> biConsumer = (i, s)->{
            if(i%2 == 0) {
                System.out.println(s + "奇数");
            } else {
                System.out.println(s + "偶数");
            }
        };
        biConsumer.andThen((l, r)->{
            System.out.println("第二个函数");
        }).andThen((l, r)->{
            System.out.println("第三个函数");
        }).accept(10, "hamu");
    }
    @Test
    public void t2() {
        BiFunction<Integer, Integer, Integer> biConsumer = Integer::sum;
        Integer a = biConsumer.andThen(v->{
            return v+1;
        }).andThen(v->{
            return v+1;
        }).apply(10, 10);
        System.out.println(a);
    }
    @Test
    public void t3() {
        Predicate<Object> myAssert = Objects::nonNull;
        if(!myAssert.test(null)) {
            System.out.println("null");
        }
    }
    @Test
    public void t4() {
        Supplier<Integer> get = ()->10;
        Stream.of(10).count();
        System.out.println(get.get());
    }
}
