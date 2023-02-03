package org.zeveon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

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
@Table(name = "person", schema = "health")
public class Person {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Builder.Default
    @Column(name = "is_admin", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean admin = false;

    @Builder.Default
    @Column(name = "is_super_admin", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean superAdmin = false;
}
