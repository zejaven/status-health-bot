package org.zeveon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Duration;

/**
 * @author Stanislav Vafin
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "statistic", schema = "health")
public class Statistic {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private StatisticId id;

    @Column(name = "response_time")
    private Duration responseTime;

    @Column(name = "response_code")
    private Integer responseCode;
}
