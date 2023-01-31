package org.zeveon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

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
@Table(name = "chat_settings", schema = "health")
public class ChatSettings {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "locale", nullable = false)
    @Builder.Default
    private String locale = "EN";

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "chat_host",
            joinColumns = @JoinColumn(name = "chat_id"),
            inverseJoinColumns = @JoinColumn(name = "host_id"),
            schema = "health"
    )
    @Builder.Default
    private Set<Host> hosts = new HashSet<>();
}
