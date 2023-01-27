package org.zeveon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zeveon.entity.ChatSettings;

/**
 * @author Stanislav Vafin
 */
@Repository
public interface ChatSettingsRepository extends JpaRepository<ChatSettings, Long> {
}
