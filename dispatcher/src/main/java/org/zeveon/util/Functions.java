package org.zeveon.util;

import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.concurrent.ConcurrentHashMap.newKeySet;

/**
 * @author Stanislav Vafin
 */
public class Functions {

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        return t -> newKeySet().add(keyExtractor.apply(t));
    }
}
