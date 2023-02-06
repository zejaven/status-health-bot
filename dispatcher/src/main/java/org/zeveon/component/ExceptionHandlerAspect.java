package org.zeveon.component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.validator.internal.metadata.descriptor.ConstraintDescriptorImpl;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.zeveon.entity.ChatSettings;
import org.zeveon.entity.Host;
import org.zeveon.model.ClassType;
import org.zeveon.model.Violation;
import org.zeveon.service.ChatSettingsService;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.zeveon.model.ClassType.HOST;
import static org.zeveon.model.Violation.PATTERN;

/**
 * @author Stanislav Vafin
 */
@Aspect
@Component
@RequiredArgsConstructor
public class ExceptionHandlerAspect {

    private static final Map<Class<?>, Violation> VIOLATIONS = Map.of(
            Pattern.class, PATTERN
    );

    private static final Map<Class<?>, ClassType> CLASSES = Map.of(
            Host.class, HOST
    );

    private final HealthBot healthBot;

    private final ChatSettingsService chatSettingsService;

    private final MessageSource messageSource;

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @AfterThrowing(value = "execution(* org.zeveon.service.*.*(..))", throwing = "exception")
    public void handleConstraintViolationException(JoinPoint joinPoint, ConstraintViolationException exception) {
        exception.getConstraintViolations().forEach(violation -> {
            var violationType = getViolationType(violation);
            if (VIOLATIONS.containsKey(violationType)) {
                switch (VIOLATIONS.get(violationType)) {
                    case PATTERN -> sendIfPossible(joinPoint, violation);
                }
            }
        });
    }

    private Class<?> getViolationType(ConstraintViolation<?> violation) {
        return ((ConstraintDescriptorImpl<?>) violation.getConstraintDescriptor()).getAnnotationDescriptor().getType();
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    private void sendIfPossible(JoinPoint joinPoint, ConstraintViolation<?> violation) {
        var longArgs = stream(joinPoint.getArgs())
                .filter(a -> a instanceof Long)
                .map(a -> (Long) a)
                .collect(toSet());
        var classType = violation.getRootBean().getClass();
        if (!longArgs.isEmpty() && CLASSES.containsKey(classType)) {
            switch (CLASSES.get(classType)) {
                case HOST -> getChatId(longArgs, (Host) violation.getRootBean())
                        .ifPresent(id -> sendResponse(buildPatternResponse(id, violation), id));
            }
        }
    }

    private Optional<Long> getChatId(Set<Long> longArgs, Host host) {
        return longArgs.stream()
                .filter(id -> host.getChatSettings().stream()
                        .map(ChatSettings::getChatId)
                        .anyMatch(id::equals))
                .findAny();
    }

    private void sendResponse(String message, Long chatId) {
        var response = new SendMessage();
        response.setChatId(chatId);
        response.setText(message);
        sendResponse(response);
    }

    private void sendResponse(SendMessage message) {
        try {
            healthBot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String buildPatternResponse(Long chatId, ConstraintViolation<?> violation) {
        return getLocalizedMessage("exception.pattern", chatId)
                .formatted(violation.getPropertyPath(), violation.getInvalidValue());
    }

    @SuppressWarnings("SameParameterValue")
    private String getLocalizedMessage(String code, Long chatId) {
        return messageSource.getMessage(code, null, chatSettingsService.getLocale(chatId));
    }
}
