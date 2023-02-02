package org.zeveon.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.zeveon.model.Method;

import java.time.ZonedDateTime;
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
@EntityListeners(AuditingEntityListener.class)
@Table(name = "chat_settings", schema = "health")
public class ChatSettings {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "chat_id")
    private Long chatId;

    @Builder.Default
    @Column(name = "locale", nullable = false)
    private String locale = "EN";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'APACHE_HTTP_CLIENT'")
    private Method method = Method.APACHE_HTTP_CLIENT;

    @Builder.Default
    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "chat_host",
            joinColumns = @JoinColumn(name = "chat_id"),
            inverseJoinColumns = @JoinColumn(name = "host_id"),
            schema = "health"
    )
    private Set<Host> hosts = new HashSet<>();

    @CreatedDate
    @TimeZoneStorage(TimeZoneStorageType.NATIVE)
    @Column(name = "created_date", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private ZonedDateTime createdDate;

    @LastModifiedDate
    @TimeZoneStorage(TimeZoneStorageType.NATIVE)
    @Column(name = "modified_date", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime modifiedDate;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, columnDefinition = "VARCHAR(255) DEFAULT 'SYSTEM'")
    private String createdBy;

    @LastModifiedBy
    @Column(name = "modified_by")
    private String modifiedBy;
}
