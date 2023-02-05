package org.zeveon.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zeveon.cache.Cache;
import org.zeveon.entity.Person;
import org.zeveon.repository.PersonRepository;
import org.zeveon.service.PersonService;

import java.util.Optional;

/**
 * @author Stanislav Vafin
 */
@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(key = "#userId", value = Cache.USERS)
    public Optional<Person> findByUserId(Long userId) {
        return personRepository.findById(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Person save(Person person) {
        return personRepository.save(person);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = Cache.USERS, allEntries = true)
    public Optional<Person> updateAdminRights(String username, boolean isAdmin) {
        var person = personRepository.findByUsername(username);
        person.ifPresent(p -> p.setAdmin(isAdmin));
        return person;
    }
}
