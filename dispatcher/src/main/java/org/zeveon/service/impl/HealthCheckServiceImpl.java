package org.zeveon.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.glassfish.grizzly.http.util.Header;
import org.springframework.stereotype.Service;
import org.zeveon.data.Data;
import org.zeveon.entity.Site;
import org.zeveon.model.BotInfo;
import org.zeveon.repository.HealthRepository;
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
import java.util.function.Consumer;

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.LF;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.zeveon.util.StringUtil.HEALTH_TEMPLATE;
import static org.zeveon.util.StringUtil.HTTP;

/**
 * @author Stanislav Vafin
 */
@Slf4j
@Service
@AllArgsConstructor
public class HealthCheckServiceImpl implements HealthCheckService {

    private final HealthRepository healthRepository;

    public void checkHealth(Site site, BotInfo botInfo) {
        var durationsRequestCountPair = Data.getRequestCount().get(site);
        var botUsername = botInfo.getBotUsername();
        var startTime = LocalDateTime.now();
        switch (botInfo.getHealthCheckMethod()) {
            case APACHE_HTTP_CLIENT -> {
                var responseCode = checkHealthApache(site, botUsername);
                setResponseTime(site, durationsRequestCountPair, startTime, site::setApacheResponseTime);
                site.setApacheResponseCode(responseCode);
            }
            case JAVA_HTTP_CLIENT -> {
                var responseCode = checkHealthJava(site, botUsername);
                setResponseTime(site, durationsRequestCountPair, startTime, site::setJavaResponseTime);
                site.setJavaResponseCode(responseCode);
            }
            case CURL_PROCESS -> {
                var responseCode = checkHealthCurl(site, botUsername);
                setResponseTime(site, durationsRequestCountPair, startTime, site::setCurlResponseTime);
                site.setCurlResponseCode(responseCode);
            }
        }
        healthRepository.save(site);
    }

    private int checkHealthApache(Site site, String botUsername) {
        try (var httpClient = HttpClientBuilder.create()
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

    private int checkHealthJava(Site site, String botUsername) {
        var httpClient = HttpClient.newBuilder().build();
        var request = HttpRequest.newBuilder()
                .uri(URI.create(site.getUrl()))
                .timeout(Duration.ofSeconds(3L))
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

    private int checkHealthCurl(Site site, String botUsername) {
        try {
            var process = Runtime.getRuntime()
                    .exec(CurlRequest.builder(site.getUrl())
                                    .head()
                                    .location()
                                    .silent()
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
}
