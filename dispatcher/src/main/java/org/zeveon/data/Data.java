package org.zeveon.data;

import org.zeveon.entity.Site;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stanislav Vafin
 */
public class Data {

    private static List<Site> sites;

    private static volatile int currentIndex;

    public static synchronized void initialize(List<Site> initializationList) {
        sites = new ArrayList<>(initializationList);
    }

    public static synchronized void addAll(List<Site> newElements) {
        sites.addAll(newElements);
    }

    public static synchronized void removeAllById(List<Long> elementsToRemove) {
        sites.removeIf(s -> elementsToRemove.contains(s.getId()));
    }

    public static synchronized Site getCurrentSite() {
        return sites.get(currentIndex < sites.size()
                ? currentIndex++
                : (currentIndex = 0));
    }
}
