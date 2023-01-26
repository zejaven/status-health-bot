package org.zeveon.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.zeveon.cache.Cache;
import org.zeveon.entity.Statistic;
import org.zeveon.service.HealthService;
import org.zeveon.service.StatisticService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.zeveon.model.Method.*;
import static org.zeveon.util.StringUtil.*;

/**
 * @author Stanislav Vafin
 */
@Slf4j
@Service
@AllArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    public static final double NANOS_IN_SECOND = 1_000_000_000.0;

    private final HealthService healthService;

    @Override
    @CacheEvict(value = Cache.SITES, allEntries = true)
    @Transactional(readOnly = true)
    public Optional<InputFile> generateStatistic() {
        try (var workbook = new XSSFWorkbook(); var outputStream = new ByteArrayOutputStream()) {
            buildStatistic(workbook);
            workbook.write(outputStream);
            var inputFile = new InputFile();
            inputFile.setMedia(new ByteArrayInputStream(outputStream.toByteArray()), DEFAULT_STATISTIC_FILE_NAME);
            return of(inputFile);
        } catch (IOException e) {
            log.error(e.getMessage());
            return empty();
        }
    }

    private void buildStatistic(XSSFWorkbook workbook) {
        var sheet = workbook.createSheet();
        var rowHeader = sheet.createRow(0);
        rowHeader.createCell(0).setCellValue(URL);
        rowHeader.createCell(1).setCellValue(DEFAULT_TEMPLATE.formatted(APACHE_HTTP_CLIENT.name(), RESPONSE_TIME));
        rowHeader.createCell(2).setCellValue(DEFAULT_TEMPLATE.formatted(JAVA_HTTP_CLIENT.name(), RESPONSE_TIME));
        rowHeader.createCell(3).setCellValue(DEFAULT_TEMPLATE.formatted(CURL_PROCESS.name(), RESPONSE_TIME));
        rowHeader.createCell(4).setCellValue(DEFAULT_TEMPLATE.formatted(APACHE_HTTP_CLIENT.name(), RESPONSE_CODE));
        rowHeader.createCell(5).setCellValue(DEFAULT_TEMPLATE.formatted(JAVA_HTTP_CLIENT.name(), RESPONSE_CODE));
        rowHeader.createCell(6).setCellValue(DEFAULT_TEMPLATE.formatted(CURL_PROCESS.name(), RESPONSE_CODE));
        var sites = healthService.getSites();
        for (int i = 0; i < sites.size(); i++) {
            var row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(sites.get(i).getUrl());
            row.createCell(1).setCellValue(sites.get(i).getStatistic().stream()
                    .filter(s -> s.getId().getMethod().equals(APACHE_HTTP_CLIENT))
                    .map(s -> s.getResponseTime().toNanos())
                    .map(n -> n / NANOS_IN_SECOND)
                    .findAny().orElse(Double.NaN));
            row.createCell(2).setCellValue(sites.get(i).getStatistic().stream()
                    .filter(s -> s.getId().getMethod().equals(JAVA_HTTP_CLIENT))
                    .map(s -> s.getResponseTime().toNanos())
                    .map(n -> n / NANOS_IN_SECOND)
                    .findAny().orElse(Double.NaN));
            row.createCell(3).setCellValue(sites.get(i).getStatistic().stream()
                    .filter(s -> s.getId().getMethod().equals(CURL_PROCESS))
                    .map(s -> s.getResponseTime().toNanos())
                    .map(n -> n / NANOS_IN_SECOND)
                    .findAny().orElse(Double.NaN));
            row.createCell(4).setCellValue(sites.get(i).getStatistic().stream()
                    .filter(s -> s.getId().getMethod().equals(APACHE_HTTP_CLIENT))
                    .map(Statistic::getResponseCode)
                    .findAny().orElse(0));
            row.createCell(5).setCellValue(sites.get(i).getStatistic().stream()
                    .filter(s -> s.getId().getMethod().equals(JAVA_HTTP_CLIENT))
                    .map(Statistic::getResponseCode)
                    .findAny().orElse(0));
            row.createCell(6).setCellValue(sites.get(i).getStatistic().stream()
                    .filter(s -> s.getId().getMethod().equals(CURL_PROCESS))
                    .map(Statistic::getResponseCode)
                    .findAny().orElse(0));
        }
        for (int i = 0; i < 7; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
