package yzh.stock.business.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String AI_ANALYZE_EXCHANGE = "ai.analyze.exchange";
    public static final String AI_ANALYZE_QUEUE = "ai.analyze.queue";
    public static final String AI_ANALYZE_ROUTING_KEY = "photo.analyze";

    @Bean
    public TopicExchange aiAnalyzeExchange() {
        return ExchangeBuilder.topicExchange(AI_ANALYZE_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue aiAnalyzeQueue() {
        return QueueBuilder.durable(AI_ANALYZE_QUEUE).build();
    }

    @Bean
    public Binding aiAnalyzeBinding(Queue aiAnalyzeQueue, TopicExchange aiAnalyzeExchange) {
        return BindingBuilder.bind(aiAnalyzeQueue).to(aiAnalyzeExchange).with(AI_ANALYZE_ROUTING_KEY);
    }
}
