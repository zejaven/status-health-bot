package org.zeveon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    private String url;

    @Builder.Default
    @OneToMany(mappedBy = "id.host", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<Statistic> statistic = new HashSet<>();

    @Builder.Default
    @ManyToMany(mappedBy = "hosts")
    private Set<ChatSettings> chatSettings = new HashSet<>();

    @Builder.Default
    @Transient
    private Lock lock = new ReentrantLock();
}
