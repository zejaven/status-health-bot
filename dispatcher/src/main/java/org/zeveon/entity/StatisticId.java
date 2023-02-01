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
    @JoinColumn(name = "host_id")
    private Host host;

    @Enumerated(EnumType.STRING)
    @Column(name = "method")
    private Method method;
}
