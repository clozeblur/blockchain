package com.fmsh.blockchain.core.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Collections;
import java.util.List;

/**
 * @Author: yuanjiaxin
 * @Date: 2018/7/23 14:53
 * @Description:
 */
@Configuration
public class MessageConfiguration {

    @Bean(name = "leaderConnectionFactory")
    @ConditionalOnMissingBean(name = "leaderConnectionFactory")
    public RedisConnectionFactory leaderConnectionFactory() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(RedisConfig.getMaxTotal());
        jedisPoolConfig.setMaxIdle(RedisConfig.getMaxIdle());
        jedisPoolConfig.setMinIdle(RedisConfig.getMinIdle());
        jedisPoolConfig.setMaxWaitMillis(RedisConfig.getMaxWaitMillis());
        jedisPoolConfig.setTestWhileIdle(RedisConfig.getTestWhileIdle());

        RedisClusterConfiguration configuration = new RedisClusterConfiguration();
        String[] arr = RedisConfig.getNodes().split(",");
        for(String subArr : arr){
            String[] hap  = subArr.split(":");
            configuration.addClusterNode(new RedisNode(hap[0],Integer.valueOf(hap[1])));
        }
        configuration.setMaxRedirects(RedisConfig.getMaxRedirects());

        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(configuration,jedisPoolConfig);
        connectionFactory.setTimeout(RedisConfig.getTimeout());
        return connectionFactory;
    }

    @Bean(name = "leaderRedisMessageListenerContainer")
    @ConditionalOnBean(name = {"leaderConnectionFactory", "leaderListenerAdapter"})
    @ConditionalOnMissingBean(name = "leaderRedisMessageListenerContainer")
    public RedisMessageListenerContainer container(RedisConnectionFactory leaderConnectionFactory,
                                                   MessageListenerAdapter leaderListenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(leaderConnectionFactory);
        List<PatternTopic> topics = Collections.singletonList(new PatternTopic("manager"));
        container.addMessageListener(leaderListenerAdapter, topics);
        return container;
    }

    @Bean(name = "leaderReceiver")
    @ConditionalOnMissingBean(name = "leaderReceiver")
    public Receiver configClientReceiver() {
        return new Receiver();
    }

    @Bean(name = "leaderListenerAdapter")
    @ConditionalOnBean(name = "leaderReceiver")
    @ConditionalOnMissingBean(name = "leaderListenerAdapter")
    public MessageListenerAdapter configClientListenerAdapter(Receiver receiver) {
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }
}
