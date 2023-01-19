package org.zeveon.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.stereotype.Service;
import org.zeveon.data.Data;
import org.zeveon.entity.Site;
import org.zeveon.model.Method;
import org.zeveon.repository.HealthRepository;
import org.zeveon.service.HealthCheckService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.zeveon.util.StringUtil.HEALTH_TEMPLATE;

/**
 * @author Stanislav Vafin
 */
@Slf4j
@Service
@AllArgsConstructor
public class HealthCheckServiceImpl implements HealthCheckService {

    private final HealthRepository healthRepository;

    public void checkHealth(Site site, Method method) {
        var durationsRequestCountPair = Data.getRequestCount().get(site);
        var startTime = LocalDateTime.now();
        switch (method) {
            case APACHE_HTTP_CLIENT -> {
                checkHealthApache(site);
                setResponseTime(site, durationsRequestCountPair, startTime, site::setApacheResponseTime);
            }
            case JAVA_HTTP_CLIENT -> {
                checkHealthJava(site);
                setResponseTime(site, durationsRequestCountPair, startTime, site::setJavaResponseTime);
            }
            case CURL_PROCESS -> {
                checkHealthCurl(site);
                setResponseTime(site, durationsRequestCountPair, startTime, site::setCurlResponseTime);
            }
        }
        healthRepository.save(site);
    }

    private void setResponseTime(
            Site site, Pair<List<Duration>, Integer> durationsRequestCountPair,
            LocalDateTime startTime,
            Consumer<Duration> responseTimeMethod
    ) {
        var durations = durationsRequestCountPair.getLeft();
        durations.add(Duration.between(startTime, LocalDateTime.now()));
        var count = durationsRequestCountPair.getRight();
        final var newCount = ++count;
        responseTimeMethod.accept(durations.stream()
                .reduce(Duration::plus)
                .map(r -> r.dividedBy(newCount))
                .orElse(Duration.ZERO));
        Data.getRequestCount().put(site, Pair.of(durations, newCount));
    }

    private void checkHealthApache(Site site) {
        try (var httpClient = HttpClientBuilder.create()
                .setConnectionTimeToLive(1000L, TimeUnit.MILLISECONDS)
                .build()) {
            var request = new HttpGet(site.getUrl());
            var response = httpClient.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            log.info(HEALTH_TEMPLATE.formatted(site.getUrl(), responseCode));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void checkHealthJava(Site site) {
        var httpClient = HttpClient.newBuilder().build();
        var request = HttpRequest.newBuilder()
                .uri(URI.create(site.getUrl()))
                .timeout(Duration.ofSeconds(3L))
                .GET().build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int responseCode = response.statusCode();
            log.info(HEALTH_TEMPLATE.formatted(site.getUrl(), responseCode));
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    private void checkHealthCurl(Site site) {
        try {
            var process = Runtime.getRuntime().exec("curl -I -L -s -H \"User-Agent: HealthBot\" %s"
                    .formatted(site.getUrl()));
            var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            var output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("HTTP")) {
                    output.append(line).append(LF);
                }
            }
            int responseCode = stream(output.toString().split(LF))
                    .reduce((a, b) -> b)
                    .map(r -> r.split(SPACE)[1])
                    .map(Integer::parseInt)
                    .orElse(0);
            log.info(HEALTH_TEMPLATE.formatted(site.getUrl(), responseCode));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
