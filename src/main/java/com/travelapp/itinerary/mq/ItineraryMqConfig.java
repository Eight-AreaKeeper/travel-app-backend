package com.travelapp.itinerary.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Asynchronous design of the itinerary generation engine: complete()/regenerate() are only responsible for
 * inserting a "generating" placeholder plan and publishing a message, then returning immediately; the actual
 * generation logic runs asynchronously in {@link ItineraryGenerationListener}.
 * Corresponds to section 3.2 (Asynchronous Design) of the "Project Technical Architecture Document".
 */
@Configuration
public class ItineraryMqConfig {

    public static final String EXCHANGE = "itinerary.generation.exchange";
    public static final String QUEUE = "itinerary.generation.queue";
    public static final String ROUTING_KEY = "itinerary.generate";

    @Bean
    public DirectExchange itineraryGenerationExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue itineraryGenerationQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding itineraryGenerationBinding(Queue itineraryGenerationQueue, DirectExchange itineraryGenerationExchange) {
        return BindingBuilder.bind(itineraryGenerationQueue).to(itineraryGenerationExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jackson2JsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter);
        return template;
    }
}
