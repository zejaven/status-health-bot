package org.zeveon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zeveon.entity.ChatSettings;
import org.zeveon.model.Method;

import java.util.Collection;
import java.util.Set;

/**
 * @author Stanislav Vafin
 */
@Repository
public interface ChatSettingsRepository extends JpaRepository<ChatSettings, Long> {

    Set<ChatSettings> findChatSettingsByHostsInAndMethod(Collection<?> hosts, Method method);
}
