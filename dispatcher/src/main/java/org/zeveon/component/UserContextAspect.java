package org.zeveon.component;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.zeveon.context.UserContext;
import org.zeveon.entity.Person;
import org.zeveon.service.PersonService;

/**
 * @author Stanislav Vafin
 */
@Aspect
@Component
@RequiredArgsConstructor
public class UserContextAspect {

    private final PersonService personService;

    @Before("execution(* org.zeveon.controller.UpdateController.processUpdate(..))")
    public void onUpdateReceived(JoinPoint joinPoint) {
        var update = (Update) joinPoint.getArgs()[0];
        if (update == null) {
            throw new RuntimeException("Received update is null");
        } else if (update.getMessage() == null) {
            throw new RuntimeException("Received unsupported message type: " + update);
        }
        var user = update.getMessage().getFrom();
        var person = personService.findByUserId(user.getId())
                .orElseGet(() -> personService.save(buildPerson(user)));
        UserContext.setInstance(user, person.isAdmin(), person.isSuperAdmin());
    }

    private Person buildPerson(User user) {
        return Person.builder()
                .userId(user.getId())
                .username(user.getUserName())
                .build();
    }
}
