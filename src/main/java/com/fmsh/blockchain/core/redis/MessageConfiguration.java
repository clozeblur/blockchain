package com.fmsh.blockchain.core.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.util.Collections;
import java.util.List;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/23 14:53
 * @Description:
 */
@Configuration
public class MessageConfiguration {

    @Bean
    public RedisMessageListenerContainer leaderRedisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                   MessageListenerAdapter messageListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        List<PatternTopic> topics = Collections.singletonList(new PatternTopic("manager"));
        container.addMessageListener(messageListenerAdapter, topics);
        return container;
    }

    @Bean
    public Receiver receiver() {
        return new Receiver();
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(Receiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }
}
