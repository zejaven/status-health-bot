package org.zeveon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zeveon.entity.Host;

/**
 * @author Stanislav Vafin
 */
@Repository
public interface HostRepository extends JpaRepository<Host, Long> {
}
