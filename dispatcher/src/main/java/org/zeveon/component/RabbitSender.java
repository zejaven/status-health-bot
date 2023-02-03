package org.zeveon.component;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.zeveon.config.RabbitConfig;

/**
 * @author Stanislav Vafin
 */
@Component
@RequiredArgsConstructor
public class RabbitSender {

    private final RabbitConfig rabbitConfig;

    private final RabbitTemplate rabbitTemplate;

    public void send(Long hostId) {
        rabbitTemplate.convertAndSend(rabbitConfig.getQueueName(), hostId);
    }
}
