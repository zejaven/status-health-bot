package org.zeveon.data;

import org.apache.commons.lang3.tuple.Pair;
import org.zeveon.entity.Host;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * @author Stanislav Vafin
 */
public class Data {

    private static List<Host> hosts;

    private static volatile int currentIndex;

    private static Map<Host, Pair<List<Duration>, Integer>> requestCount;

    public static synchronized void initialize(List<Host> initializationList) {
        hosts = new ArrayList<>(initializationList);
        requestCount = hosts.stream()
                .collect(Collectors.toMap(e -> e, e -> Pair.of(new ArrayList<>(), 0)));
    }

    public static synchronized void addAll(List<Host> newElements) {
        hosts.addAll(newElements);
        newElements.forEach(e -> requestCount.put(e, Pair.of(new ArrayList<>(), 0)));
    }

    public static synchronized void removeAllById(Set<Long> elementsToRemove) {
        hosts.removeIf(h -> elementsToRemove.contains(h.getId()));
        requestCount.entrySet().removeIf(e -> elementsToRemove.contains(e.getKey().getId()));
    }

    public static synchronized Optional<Host> getCurrentHost() {
        return !hosts.isEmpty()
                ? of(hosts.get(currentIndex < hosts.size() ? currentIndex++ : (currentIndex = 0)))
                : empty();
    }

    public static synchronized Map<Host, Pair<List<Duration>, Integer>> getRequestCount() {
        return requestCount;
    }
}
