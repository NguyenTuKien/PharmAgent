package ct01.n07.backend.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "email.otp.queue";
    public static final String EXCHANGE_NAME = "email.exchange";
    public static final String ROUTING_KEY = "email.otp.routing.key";

    // 1. Tạo Queue
    @Bean
    public Queue otpEmailQueue() {
        return new Queue(QUEUE_NAME, true); // true = durable (không mất dữ liệu khi RabbitMQ restart)
    }

    // 2. Tạo Exchange (Trung tâm phân loại tin nhắn)
    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    // 3. Liên kết Queue với Exchange qua Routing Key
    @Bean
    public Binding binding(Queue otpEmailQueue, DirectExchange emailExchange) {
        return BindingBuilder.bind(otpEmailQueue).to(emailExchange).with(ROUTING_KEY);
    }

    // 4. Cấu hình Converter để tự động chuyển Java Object thành JSON
    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
