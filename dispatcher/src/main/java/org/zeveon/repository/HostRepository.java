package org.zeveon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zeveon.entity.Host;

import java.util.Set;

/**
 * @author Stanislav Vafin
 */
@Repository
public interface HostRepository extends JpaRepository<Host, Long> {

    Set<Host> findByChatSettingsChatId(Long chatId);
}
