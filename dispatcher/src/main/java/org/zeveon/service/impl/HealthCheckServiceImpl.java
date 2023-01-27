package org.zeveon.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.glassfish.grizzly.http.util.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zeveon.data.Data;
import org.zeveon.entity.Site;
import org.zeveon.entity.Statistic;
import org.zeveon.entity.StatisticId;
import org.zeveon.model.BotInfo;
import org.zeveon.model.Method;
import org.zeveon.repository.SiteRepository;
import org.zeveon.service.HealthCheckService;
import org.zeveon.util.CurlRequest;

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

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.zeveon.model.Method.*;

/**
 * @author Stanislav Vafin
 */
@Slf4j
@Service
@AllArgsConstructor
public class HealthCheckServiceImpl implements HealthCheckService {

    public static final int MILLIS_IN_SECOND = 1000;
    public static final String HEALTH_TEMPLATE = "%s | %s";
    public static final String HTTP = "HTTP";

    private final SiteRepository siteRepository;

    @Transactional(rollbackFor = Exception.class)
    public void checkHealth(Site site, BotInfo botInfo) {
        var durationsRequestCountPair = Data.getRequestCount().get(site);
        var botUsername = botInfo.getBotUsername();
        var startTime = LocalDateTime.now();
        var connectionTimeout = botInfo.getHealthCheckConnectionTimeout();
        switch (botInfo.getHealthCheckMethod()) {
            case APACHE_HTTP_CLIENT -> saveStatistic(
                    site,
                    APACHE_HTTP_CLIENT,
                    durationsRequestCountPair,
                    startTime,
                    checkHealthApache(site, botUsername, connectionTimeout)
            );
            case JAVA_HTTP_CLIENT -> saveStatistic(
                    site,
                    JAVA_HTTP_CLIENT,
                    durationsRequestCountPair,
                    startTime,
                    checkHealthJava(site, botUsername, connectionTimeout)
            );
            case CURL_PROCESS -> saveStatistic(
                    site,
                    CURL_PROCESS,
                    durationsRequestCountPair,
                    startTime,
                    checkHealthCurl(site, botUsername, connectionTimeout)
            );
        }
        siteRepository.save(site);
    }

    private int checkHealthApache(Site site, String botUsername, Integer connectionTimeout) {
        try (var httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(connectionTimeout * MILLIS_IN_SECOND)
                        .build())
                .setUserAgent(botUsername)
                .build()) {
            var request = new HttpGet(site.getUrl());
            var response = httpClient.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            log.info(HEALTH_TEMPLATE.formatted(site.getUrl(), responseCode));
            return responseCode;
        } catch (IOException e) {
            log.error(e.getMessage());
            return 0;
        }
    }

    private int checkHealthJava(Site site, String botUsername, Integer connectionTimeout) {
        var httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        var request = HttpRequest.newBuilder()
                .header(Header.UserAgent.name(), botUsername)
                .uri(URI.create(site.getUrl()))
                .timeout(Duration.ofSeconds(connectionTimeout))
                .GET().build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int responseCode = response.statusCode();
            log.info(HEALTH_TEMPLATE.formatted(site.getUrl(), responseCode));
            return responseCode;
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            return 0;
        }
    }

    private int checkHealthCurl(Site site, String botUsername, Integer connectionTimeout) {
        try {
            var process = Runtime.getRuntime()
                    .exec(CurlRequest.builder(site.getUrl())
                            .head()
                            .location()
                            .silent()
                            .connectTimeout(connectionTimeout)
                            .header(Header.UserAgent.name(), botUsername)
                            .build()
                            .getCommand());
            var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            var output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(HTTP)) {
                    output.append(line).append(LF);
                }
            }
            int responseCode = stream(output.toString().split(LF))
                    .reduce((a, b) -> b)
                    .filter(r -> !r.isEmpty())
                    .map(r -> r.split(SPACE)[1])
                    .map(Integer::parseInt)
                    .orElse(0);
            log.info(HEALTH_TEMPLATE.formatted(site.getUrl(), responseCode));
            return responseCode;
        } catch (IOException e) {
            log.error(e.getMessage());
            return 0;
        }
    }

    private void saveStatistic(
            Site site,
            Method method,
            Pair<List<Duration>, Integer> durationsRequestCountPair,
            LocalDateTime startTime,
            int responseCode
    ) {
        var durations = durationsRequestCountPair.getLeft();
        durations.add(Duration.between(startTime, LocalDateTime.now()));
        var count = durationsRequestCountPair.getRight();
        final var newCount = ++count;
        var statistic = Statistic.builder()
                .id(StatisticId.builder()
                        .site(site)
                        .method(method)
                        .build())
                .responseTime(durations.stream()
                        .reduce(Duration::plus)
                        .map(r -> r.dividedBy(newCount))
                        .orElse(Duration.ZERO))
                .responseCode(responseCode)
                .build();
        site.getStatistic().removeIf(s -> s.equals(statistic));
        site.getStatistic().add(statistic);
        Data.getRequestCount().put(site, Pair.of(durations, newCount));
    }
}
