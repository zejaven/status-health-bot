package org.zeveon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zeveon.entity.Site;

/**
 * @author Stanislav Vafin
 */
@Repository
public interface HealthRepository extends JpaRepository<Site, Long> {
}
