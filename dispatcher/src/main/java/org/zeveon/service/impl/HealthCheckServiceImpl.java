package org.zeveon.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.glassfish.grizzly.http.util.Header;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zeveon.data.Data;
import org.zeveon.entity.ChatSettings;
import org.zeveon.entity.Host;
import org.zeveon.entity.Statistic;
import org.zeveon.entity.StatisticId;
import org.zeveon.model.*;
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
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.zeveon.model.Method.*;
import static org.zeveon.util.Functions.distinctByKey;

/**
 * @author Stanislav Vafin
 */
@Slf4j
@Service
@AllArgsConstructor
public class HealthCheckServiceImpl implements HealthCheckService {

    private static final int MILLIS_IN_SECOND = 1000;
    private static final String HEALTH_TEMPLATE = "%s | %s";
    private static final String HTTP = "HTTP";
    private static final String URL_APPENDER = "/robots.txt";
    private static final String PROTOCOL_REGEX = "^https?";
    private static final String EXCLUDE_RIGHT_SIDE_PATTERN = "(%s).*";
    private static final String $ = "$";

    private final HostRepository hostRepository;

    @Transactional(rollbackFor = Exception.class)
    public void checkHealth(Long hostId, BotInfo botInfo, BiConsumer<Method, HealthInfo> reportStatusMethod) {
        var host = hostRepository.findById(hostId)
                .orElseThrow(() -> new RuntimeException("This host already removed"));
        host.getChatSettings().stream()
                .filter(distinctByKey(ChatSettings::getMethod))
                .forEach(chatSettings -> {
                    var method = chatSettings.getMethod();
                    var durationsRequestCountPair = Data.getRequestCount().get(host);
                    var botUsername = botInfo.getBotUsername();
                    var startTime = LocalDateTime.now();
                    var connectionTimeout = botInfo.getHealthCheckConnectionTimeout();
                    var responseCode = 0;
                    var statisticExists = true;
                    var responseCodeChanged = false;
                    var statistic = findHostStatisticByMethod(host, method);
                    var needAppender = statistic.map(Statistic::getNeedAppender);
                    var protocol = statistic.map(Statistic::getPreferredProtocol);
                    var connectionType = ConnectionType.getByFields(
                            needAppender.orElse(true),
                            protocol.isPresent());
                    switch (method) {
                        case APACHE_HTTP_CLIENT -> {
                            for (var entry : reorder(connectionType)) {
                                responseCode = checkHealthApache(
                                        modifyUrl(host, entry, protocol),
                                        botUsername,
                                        connectionTimeout);
                                if (isModified(needAppender, protocol) || responseCodeSuccessful(responseCode)) {
                                    needAppender = of(entry.isNeedAppender());
                                    protocol = of(getProtocol(host.getUrl(), protocol));
                                    break;
                                }
                            }
                            responseCodeChanged = checkResponseCodeChanged(statistic, responseCode);
                            saveStatistic(
                                    host,
                                    APACHE_HTTP_CLIENT,
                                    durationsRequestCountPair,
                                    startTime,
                                    responseCode,
                                    needAppender,
                                    protocol
                            );
                        }
                        case JAVA_HTTP_CLIENT -> {
                            for (var entry : reorder(connectionType)) {
                                responseCode = checkHealthJava(
                                        modifyUrl(host, entry, protocol),
                                        botUsername,
                                        connectionTimeout);
                                if (isModified(needAppender, protocol) || responseCodeSuccessful(responseCode)) {
                                    needAppender = of(entry.isNeedAppender());
                                    protocol = of(getProtocol(host.getUrl(), protocol));
                                    break;
                                }
                            }
                            responseCodeChanged = checkResponseCodeChanged(statistic, responseCode);
                            saveStatistic(
                                    host,
                                    JAVA_HTTP_CLIENT,
                                    durationsRequestCountPair,
                                    startTime,
                                    responseCode,
                                    needAppender,
                                    protocol
                            );
                        }
                        case CURL_PROCESS -> {
                            for (var entry : reorder(connectionType)) {
                                responseCode = checkHealthCurl(
                                        modifyUrl(host, entry, protocol),
                                        botUsername,
                                        connectionTimeout);
                                if (isModified(needAppender, protocol) || responseCodeSuccessful(responseCode)) {
                                    needAppender = of(entry.isNeedAppender());
                                    protocol = of(getProtocol(host.getUrl(), protocol));
                                    break;
                                }
                            }
                            responseCodeChanged = checkResponseCodeChanged(statistic, responseCode);
                            saveStatistic(
                                    host,
                                    CURL_PROCESS,
                                    durationsRequestCountPair,
                                    startTime,
                                    responseCode,
                                    needAppender,
                                    protocol
                            );
                        }
                    }
                    if (responseCodeChanged || !statisticExists) {
                        reportStatusMethod.accept(method, buildHealthInfo(responseCode, statisticExists, responseCodeChanged));
                    }
                });
    }

    private List<ConnectionType> reorder(ConnectionType connectionType) {
        return Stream.concat(
                Stream.of(connectionType),
                stream(ConnectionType.values())
                        .filter(c -> !connectionType.equals(c))
        ).toList();
    }

    private boolean isModified(Optional<Boolean> needAppenderOpt, Optional<Protocol> protocolOpt) {
        return needAppenderOpt.isPresent() || protocolOpt.isPresent();
    }

    private Protocol getProtocol(String url, Optional<Protocol> protocol) {
        return protocol.orElse(Protocol.valueOf(url.replaceAll(EXCLUDE_RIGHT_SIDE_PATTERN.formatted(PROTOCOL_REGEX), group(1)).toUpperCase()));
    }

    @SuppressWarnings("SameParameterValue")
    private String group(Integer group) {
        return $ + group;
    }

    private String modifyUrl(Host host, ConnectionType connectionType, Optional<Protocol> protocol) {
        var modifiedProtocolUrl = protocol
                .map(p -> host.getUrl().replaceAll(PROTOCOL_REGEX, p.name().toLowerCase()))
                .orElse(host.getUrl());
        return connectionType.isNeedAppender()
                ? modifiedProtocolUrl.concat(URL_APPENDER)
                : modifiedProtocolUrl;
    }

    private boolean responseCodeSuccessful(int responseCode) {
        return responseCode != 0 && HttpStatus.valueOf(responseCode).is2xxSuccessful();
    }

    private boolean checkResponseCodeChanged(Optional<Statistic> statistic, int currentResponseCode) {
        return statistic
                .map(s -> s.getResponseCode() != currentResponseCode)
                .orElse(true);
    }

    private Optional<Statistic> findHostStatisticByMethod(Host host, Method method) {
        return host.getStatistic().stream()
                .filter(s -> s.getId().getMethod().equals(method))
                .findAny();
    }

    private int checkHealthApache(String url, String botUsername, Integer connectionTimeout) {
        try (var httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(connectionTimeout * MILLIS_IN_SECOND)
                        .build())
                .setUserAgent(botUsername)
                .build()) {
            var request = new HttpGet(url);
            var response = httpClient.execute(request);
            int responseCode = response.getStatusLine().getStatusCode();
            log.debug(HEALTH_TEMPLATE.formatted(url, responseCode));
            return responseCode;
        } catch (IOException e) {
            log.debug(e.getMessage());
            return 0;
        }
    }

    private int checkHealthJava(String url, String botUsername, Integer connectionTimeout) {
        var httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        var request = HttpRequest.newBuilder()
                .header(Header.UserAgent.name(), botUsername)
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(connectionTimeout))
                .GET().build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int responseCode = response.statusCode();
            log.debug(HEALTH_TEMPLATE.formatted(url, responseCode));
            return responseCode;
        } catch (IOException | InterruptedException e) {
            log.debug(e.getMessage());
            return 0;
        }
    }

    private int checkHealthCurl(String url, String botUsername, Integer connectionTimeout) {
        try {
            var process = Runtime.getRuntime()
                    .exec(CurlRequest.builder(url)
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
            log.debug(HEALTH_TEMPLATE.formatted(url, responseCode));
            return responseCode;
        } catch (IOException e) {
            log.debug(e.getMessage());
            return 0;
        }
    }

    private void saveStatistic(
            Host host,
            Method method,
            Pair<List<Duration>, Integer> durationsRequestCountPair,
            LocalDateTime startTime,
            int responseCode,
            Optional<Boolean> needAppender,
            Optional<Protocol> protocol
    ) {
        var durations = durationsRequestCountPair.getLeft();
        durations.add(Duration.between(startTime, LocalDateTime.now()));
        var count = durationsRequestCountPair.getRight();
        final var newCount = ++count;
        var statistic = host.getStatistic();
        statistic.stream()
                .filter(s -> s.getId().equals(buildStatisticId(host, method)))
                .findAny()
                .ifPresentOrElse(
                        s -> updateFields(s, responseCode, durations, newCount, needAppender.orElse(null), protocol.orElse(null)),
                        () -> statistic.add(buildStatistic(host, method, responseCode, durations, newCount, needAppender.orElse(null), protocol.orElse(null)))
                );
        Data.getRequestCount().put(host, Pair.of(durations, newCount));
    }

    private Statistic buildStatistic(Host host, Method method, int responseCode, List<Duration> durations, Integer newCount, Boolean needAppender, Protocol protocol) {
        return Statistic.builder()
                .id(buildStatisticId(host, method))
                .responseTime(calculateAverage(durations, newCount))
                .responseCode(responseCode)
                .needAppender(needAppender)
                .preferredProtocol(protocol)
                .build();
    }

    private StatisticId buildStatisticId(Host host, Method method) {
        return StatisticId.builder()
                .host(host)
                .method(method)
                .build();
    }

    private void updateFields(Statistic s, int responseCode, List<Duration> durations, Integer newCount, Boolean needAppender, Protocol protocol) {
        s.setResponseTime(calculateAverage(durations, newCount));
        s.setResponseCode(responseCode);
        s.setNeedAppender(needAppender);
        s.setPreferredProtocol(protocol);
    }

    private Duration calculateAverage(List<Duration> durations, Integer newCount) {
        return durations.stream()
                .reduce(Duration::plus)
                .map(r -> r.dividedBy(newCount))
                .orElse(Duration.ZERO);
    }

    private HealthInfo buildHealthInfo(int responseCode, boolean statisticExists, boolean responseCodeChanged) {
        return HealthInfo.builder()
                .statisticExists(statisticExists)
                .responseCodeChanged(responseCodeChanged)
                .responseCode(responseCode)
                .build();
    }
}
