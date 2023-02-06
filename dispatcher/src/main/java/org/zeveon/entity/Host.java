package org.zeveon.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

import static org.zeveon.util.ConstraintUtil.IP_REGEXP;
import static org.zeveon.util.ConstraintUtil.LINK_REGEXP;

/**
 * @author Stanislav Vafin
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "host", schema = "health")
public class Host {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(name = "url", unique = true, nullable = false)
    @Pattern(regexp = "(" + LINK_REGEXP + "|" + IP_REGEXP + ")")
    private String url;

    @Builder.Default
    @OneToMany(mappedBy = "id.host", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<Statistic> statistic = new HashSet<>();

    @Builder.Default
    @ManyToMany(mappedBy = "hosts")
    private Set<ChatSettings> chatSettings = new HashSet<>();

    @Builder.Default
    @Transient
    private Semaphore lock = new Semaphore(1);
}
