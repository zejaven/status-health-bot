package org.zeveon.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.stereotype.Service;
import org.zeveon.entity.Site;
import org.zeveon.service.HealthCheckService;

import java.io.IOException;
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
    }
}
