package org.zeveon.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.CacheEvict;
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
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;
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
            return of(new InputFile()
                    .setMedia(new ByteArrayInputStream(outputStream.toByteArray()), DEFAULT_STATISTIC_FILE_NAME));
        } catch (IOException e) {
            log.error(e.getMessage());
            return empty();
        }
    }

    private void buildStatistic(XSSFWorkbook workbook) {
        var sheet = workbook.createSheet();
        var rowHeader = sheet.createRow(0);
        rowHeader.createCell(0).setCellValue(URL);
        for (int i = 0; i < Method.values().length; i++) {
            rowHeader.createCell(i + 1)
                    .setCellValue(STATISTIC_TEMPLATE_TIME.formatted(Method.values()[i].getDescription(), RESPONSE_TIME));
        }
        for (int i = 0; i < Method.values().length; i++) {
            rowHeader.createCell(i + 4)
                    .setCellValue(STATISTIC_TEMPLATE_CODE.formatted(Method.values()[i].getDescription(), RESPONSE_CODE));
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
