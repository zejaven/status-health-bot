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
import org.zeveon.entity.Host;
import org.zeveon.entity.Statistic;
import org.zeveon.entity.StatisticId;
import org.zeveon.model.BotInfo;
import org.zeveon.model.Method;
import org.zeveon.repository.HostRepository;
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

    private final HostRepository hostRepository;

    @Transactional(rollbackFor = Exception.class)
    public void checkHealth(Host host, BotInfo botInfo) {
        var durationsRequestCountPair = Data.getRequestCount().get(host);
        var botUsername = botInfo.getBotUsername();
        var startTime = LocalDateTime.now();
        var connectionTimeout = botInfo.getHealthCheckConnectionTimeout();
        switch (botInfo.getHealthCheckMethod()) {
            case APACHE_HTTP_CLIENT -> saveStatistic(
                    host,
                    APACHE_HTTP_CLIENT,
                    durationsRequestCountPair,
                    startTime,
                    checkHealthApache(host, botUsername, connectionTimeout)
            );
            case JAVA_HTTP_CLIENT -> saveStatistic(
                    host,
                    JAVA_HTTP_CLIENT,
                    durationsRequestCountPair,
                    startTime,
                    checkHealthJava(host, botUsername, connectionTimeout)
            );
            case CURL_PROCESS -> saveStatistic(
                    host,
                    CURL_PROCESS,
                    durationsRequestCountPair,
                    startTime,
                    checkHealthCurl(host, botUsername, connectionTimeout)
            );
        }
        hostRepository.save(host);
    }

    private int checkHealthApache(Host host, String botUsername, Integer connectionTimeout) {
        try (var httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(connectionTimeout * MILLIS_IN_SECOND)
                        .build())
                .setUserAgent(botUsername)
                .build()) {
            var request = new HttpGet(host.getUrl());
            var response = httpClient.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            log.info(HEALTH_TEMPLATE.formatted(host.getUrl(), responseCode));
            return responseCode;
        } catch (IOException e) {
            log.error(e.getMessage());
            return 0;
        }
    }

    private int checkHealthJava(Host host, String botUsername, Integer connectionTimeout) {
        var httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        var request = HttpRequest.newBuilder()
                .header(Header.UserAgent.name(), botUsername)
                .uri(URI.create(host.getUrl()))
                .timeout(Duration.ofSeconds(connectionTimeout))
                .GET().build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int responseCode = response.statusCode();
            log.info(HEALTH_TEMPLATE.formatted(host.getUrl(), responseCode));
            return responseCode;
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            return 0;
        }
    }

    private int checkHealthCurl(Host host, String botUsername, Integer connectionTimeout) {
        try {
            var process = Runtime.getRuntime()
                    .exec(CurlRequest.builder(host.getUrl())
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
            log.info(HEALTH_TEMPLATE.formatted(host.getUrl(), responseCode));
            return responseCode;
        } catch (IOException e) {
            log.error(e.getMessage());
            return 0;
        }
    }

    private void saveStatistic(
            Host host,
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
                        .host(host)
                        .method(method)
                        .build())
                .responseTime(durations.stream()
                        .reduce(Duration::plus)
                        .map(r -> r.dividedBy(newCount))
                        .orElse(Duration.ZERO))
                .responseCode(responseCode)
                .build();
        host.getStatistic().removeIf(s -> s.equals(statistic));
        host.getStatistic().add(statistic);
        Data.getRequestCount().put(host, Pair.of(durations, newCount));
    }
}
