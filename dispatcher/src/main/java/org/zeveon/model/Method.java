package org.zeveon.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Stanislav Vafin
 */
@Getter
@AllArgsConstructor
public enum Method {

    APACHE_HTTP_CLIENT("Библиотека Apache"),
    JAVA_HTTP_CLIENT("Библиотека Java"),
    CURL_PROCESS("Утилита cURL");

    private final String description;
}
