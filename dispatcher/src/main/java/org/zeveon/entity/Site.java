package org.zeveon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Stanislav Vafin
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "site", schema = "health")
public class Site {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "url")
    private String url;

    @Column(name = "apache_response_time")
    private Duration apacheResponseTime;

    @Column(name = "java_response_time")
    private Duration javaResponseTime;

    @Column(name = "curl_response_time")
    private Duration curlResponseTime;

    @Column(name = "apache_response_code")
    private Integer apacheResponseCode;

    @Column(name = "java_response_code")
    private Integer javaResponseCode;

    @Column(name = "curl_response_code")
    private Integer curlResponseCode;

    @Transient
    @Builder.Default
    private Lock lock = new ReentrantLock();
}
