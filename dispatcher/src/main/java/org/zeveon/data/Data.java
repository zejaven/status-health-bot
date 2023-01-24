package org.zeveon.data;

import org.apache.commons.lang3.tuple.Pair;
import org.zeveon.entity.Site;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * @author Stanislav Vafin
 */
public class Data {

    private static List<Site> sites;

    private static volatile int currentIndex;

    private static Map<Site, Pair<List<Duration>, Integer>> requestCount;

    public static synchronized void initialize(List<Site> initializationList) {
        sites = new ArrayList<>(initializationList);
        requestCount = sites.stream()
                .collect(Collectors.toMap(e -> e, e -> Pair.of(new ArrayList<>(), 0)));
    }

    public static synchronized void addAll(List<Site> newElements) {
        sites.addAll(newElements);
    }

    public static synchronized void removeAllById(List<Long> elementsToRemove) {
        sites.removeIf(s -> elementsToRemove.contains(s.getId()));
    }

    public static synchronized Optional<Site> getCurrentSite() {
        return !sites.isEmpty()
                ? of(sites.get(currentIndex < sites.size() ? currentIndex++ : (currentIndex = 0)))
                : empty();
    }

    public static synchronized Map<Site, Pair<List<Duration>, Integer>> getRequestCount() {
        return requestCount;
    }
}
