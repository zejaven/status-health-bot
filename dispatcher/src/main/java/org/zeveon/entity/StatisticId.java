package org.zeveon.entity;

import jakarta.persistence.*;
import lombok.*;
import org.zeveon.model.Method;

import java.io.Serializable;

/**
 * @author Stanislav Vafin
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@EqualsAndHashCode
public class StatisticId implements Serializable {

    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(name = "method")
    @Enumerated(EnumType.STRING)
    private Method method;
}
