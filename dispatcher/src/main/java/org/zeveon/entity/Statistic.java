package org.zeveon.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.zeveon.model.Protocol;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * @author Stanislav Vafin
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "statistic", schema = "health")
public class Statistic {

    @EqualsAndHashCode.Include
    @EmbeddedId
    private StatisticId id;

    @Column(name = "response_time")
    private Duration responseTime;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "need_appender")
    private Boolean needAppender;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_protocol")
    private Protocol preferredProtocol;

    @CreatedDate
    @TimeZoneStorage(TimeZoneStorageType.NATIVE)
    @Column(name = "created_date", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private ZonedDateTime createdDate;

    @LastModifiedDate
    @TimeZoneStorage(TimeZoneStorageType.NATIVE)
    @Column(name = "modified_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime modifiedDate;
}
