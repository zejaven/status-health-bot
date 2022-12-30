package org.zeveon.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.stereotype.Service;
import org.zeveon.entity.Site;
import org.zeveon.service.HealthCheckService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.zeveon.util.StringUtil.HEALTH_TEMPLATE;

/**
 * @author Stanislav Vafin
 */
@Slf4j
@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    @Override
    public void checkHealth(Site site) {
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
//        var httpClient = HttpClient.newBuilder().build();
//        var request = HttpRequest.newBuilder()
//                .uri(URI.create(site.getUrl()))
//                .timeout(Duration.ofSeconds(3L))
//                .GET().build();
//        try {
//            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//            int responseCode = response.statusCode();
//            log.info(HEALTH_TEMPLATE.formatted(site.getUrl(), responseCode));
//        } catch (IOException | InterruptedException e) {
//            log.error(e.getMessage());
//        }
    }
}
