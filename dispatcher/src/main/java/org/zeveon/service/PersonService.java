package org.zeveon.service;

import org.zeveon.entity.Person;

import java.util.Optional;

/**
 * @author Stanislav Vafin
 */
public interface PersonService {

    Optional<Person> findByUserId(Long userId);

    Person save(Person person);

    Optional<Person> updateAdminRights(String username, boolean isAdmin);
}
