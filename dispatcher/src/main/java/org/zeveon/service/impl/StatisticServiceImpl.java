package org.zeveon.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.zeveon.cache.Cache;
import org.zeveon.entity.Site;
import org.zeveon.entity.Statistic;
import org.zeveon.model.Method;
import org.zeveon.service.HealthService;
import org.zeveon.service.StatisticService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * @author Stanislav Vafin
 */
@Slf4j
@Service
@AllArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    public static final double NANOS_IN_SECOND = 1_000_000_000.0;
    public static final String DEFAULT_STATISTIC_FILE_NAME = "speed-test-statistic.xlsx";
    public static final String STATISTIC_TEMPLATE_TIME = "%s, %s, %s";
    public static final String STATISTIC_TEMPLATE_CODE = "%s, %s";
    public static final String URL = "URL";
    public static final Map<Method, String> DESCRIPTION = Map.of(
            Method.APACHE_HTTP_CLIENT, "method.apache",
            Method.JAVA_HTTP_CLIENT, "method.java",
            Method.CURL_PROCESS, "method.curl"
    );

    private final MessageSource messageSource;

    private final HealthService healthService;

    @Override
    @CacheEvict(value = Cache.SITES, allEntries = true)
    @Transactional(readOnly = true)
    public Optional<InputFile> generateStatistic(Long chatId) {
        try (var workbook = new XSSFWorkbook(); var outputStream = new ByteArrayOutputStream()) {
            buildStatistic(workbook, chatId);
            workbook.write(outputStream);
            return of(new InputFile()
                    .setMedia(new ByteArrayInputStream(outputStream.toByteArray()), DEFAULT_STATISTIC_FILE_NAME));
        } catch (IOException e) {
            log.error(e.getMessage());
            return empty();
        }
    }

    private void buildStatistic(XSSFWorkbook workbook, Long chatId) {
        var locale = healthService.getLocale(chatId);
        var sheet = workbook.createSheet();
        var rowHeader = sheet.createRow(0);
        rowHeader.createCell(0).setCellValue(URL);
        for (int i = 0; i < Method.values().length; i++) {
            rowHeader.createCell(i + 1)
                    .setCellValue(STATISTIC_TEMPLATE_TIME.formatted(
                            messageSource.getMessage(DESCRIPTION.get(Method.values()[i]), null, locale),
                            messageSource.getMessage("statistic.response_time", null, locale),
                            messageSource.getMessage("statistic.secs", null, locale)
                    ));
        }
        for (int i = 0; i < Method.values().length; i++) {
            rowHeader.createCell(i + 4)
                    .setCellValue(STATISTIC_TEMPLATE_CODE.formatted(
                            messageSource.getMessage(DESCRIPTION.get(Method.values()[i]), null, healthService.getLocale(chatId)),
                            messageSource.getMessage("statistic.response_code", null, locale)
                    ));
        }
        var sites = healthService.getSites();
        for (int i = 0; i < sites.size(); i++) {
            var row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(sites.get(i).getUrl());
            for (int j = 0; j < Method.values().length; j++) {
                row.createCell(j + 1).setCellValue(getResponseTimeInSeconds(sites, i, Method.values()[j]));
            }
            for (int j = 0; j < Method.values().length; j++) {
                row.createCell(j + 4).setCellValue(getResponseCode(sites, i, Method.values()[j]));
            }
        }
        for (int i = 0; i < 7; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private Double getResponseTimeInSeconds(List<Site> sites, int i, Method apacheHttpClient) {
        return sites.get(i).getStatistic().stream()
                .filter(s -> s.getId().getMethod().equals(apacheHttpClient))
                .map(s -> s.getResponseTime().toNanos())
                .map(n -> n / NANOS_IN_SECOND)
                .findAny().orElse(Double.NaN);
    }

    private Integer getResponseCode(List<Site> sites, int i, Method apacheHttpClient) {
        return sites.get(i).getStatistic().stream()
                .filter(s -> s.getId().getMethod().equals(apacheHttpClient))
                .map(Statistic::getResponseCode)
                .findAny().orElse(0);
    }
}
