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
@Table(name = "chat_settings", schema = "health")
public class ChatSettings {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "locale")
    private String locale;
}
