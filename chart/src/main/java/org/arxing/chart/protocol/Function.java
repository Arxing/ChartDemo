package org.arxing.chart.protocol;

public interface Function<T, R> {
    R apply(T t);
}
