package org.zeveon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zeveon.entity.Person;

import java.util.Optional;

/**
 * @author Stanislav Vafin
 */
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByUsername(String username);
}
