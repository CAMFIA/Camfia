package s05.p12a104.mafia.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import s05.p12a104.mafia.redispubsub.DayDiscussionFinSubscriber;
import s05.p12a104.mafia.redispubsub.DayEliminationFinSubscriber;
import s05.p12a104.mafia.redispubsub.DayToNightFinSubscriber;
import s05.p12a104.mafia.redispubsub.EndSubscriber;
import s05.p12a104.mafia.redispubsub.NightVoteFinSubscriber;
import s05.p12a104.mafia.redispubsub.StartFinSubscriber;

@Configuration
@EnableRedisRepositories
public class RedisConfig {

  @Bean
  @Primary
  public RedisProperties redisProperties() {
    return new RedisProperties();
  }

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisProperties properties = redisProperties();
    return new LettuceConnectionFactory(properties.getHost(), properties.getPort());
  }

//  @Bean
//  public ObjectMapper objectMapper() {
//    ObjectMapper mapper = new ObjectMapper();
//
//    SimpleModule module = new JavaTimeModule();
////    module.addSerializer(new StringRedisSerializer());
//    module.addKeySerializer(new StringRedisSerializer());
////    mapper.registerModule(new JavaTimeModule());
////    return mapper;
//  }

  @Bean
  public ChannelTopic topicStartFin() {
    return new ChannelTopic("START_FIN");
  }

  @Bean
  public ChannelTopic topicDayDiscussionFin() {
    return new ChannelTopic("DAY_DISCUSSION_FIN");
  }

  @Bean
  public ChannelTopic topicDayEliminationFin() {
    return new ChannelTopic("DAY_ELIMINAION_FIN");
  }

  @Bean
  public ChannelTopic topicDayToNightFin() {
    return new ChannelTopic("DAY_TO_NIGHT_FIN");
  }

  @Bean
  public ChannelTopic topicNightVoteFin() {
    return new ChannelTopic("NIGHT_VOTE_FIN");
  }

  @Bean
  public ChannelTopic topicEnd() {
    return new ChannelTopic("END");
  }

  @Bean
  public RedisMessageListenerContainer redisMessageListener(
      RedisConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter,
      MessageListenerAdapter dayDisculistenerAdapter,
      MessageListenerAdapter dayEliminationlistenerAdapter,
      MessageListenerAdapter dayToNightlistenerAdapter,
      MessageListenerAdapter nightVotelistenerAdapter, MessageListenerAdapter endlistenerAdapter,
      ChannelTopic topicStartFin, ChannelTopic topicDayDiscussionFin,
      ChannelTopic topicDayEliminationFin, ChannelTopic topicDayToNightFin,
      ChannelTopic topicNightVoteFin, ChannelTopic topicEnd) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(listenerAdapter, topicStartFin);
    container.addMessageListener(dayDisculistenerAdapter, topicDayDiscussionFin);
    container.addMessageListener(dayEliminationlistenerAdapter, topicDayEliminationFin);
    container.addMessageListener(dayToNightlistenerAdapter, topicDayToNightFin);
    container.addMessageListener(nightVotelistenerAdapter, topicNightVoteFin);
    container.addMessageListener(endlistenerAdapter, topicEnd);
    return container;
  }

  @Bean
  public MessageListenerAdapter listenerAdapter(StartFinSubscriber subscriber) {
    return new MessageListenerAdapter(subscriber, "sendMessage");
  }

  @Bean
  public MessageListenerAdapter dayDisculistenerAdapter(DayDiscussionFinSubscriber subscriber) {
    return new MessageListenerAdapter(subscriber, "sendMessage");
  }

  @Bean
  public MessageListenerAdapter dayEliminationlistenerAdapter(
      DayEliminationFinSubscriber subscriber) {
    return new MessageListenerAdapter(subscriber, "sendMessage");
  }

  @Bean
  public MessageListenerAdapter dayToNightlistenerAdapter(DayToNightFinSubscriber subscriber) {
    return new MessageListenerAdapter(subscriber, "sendMessage");
  }

  @Bean
  public MessageListenerAdapter nightVotelistenerAdapter(NightVoteFinSubscriber subscriber) {
    return new MessageListenerAdapter(subscriber, "sendMessage");
  }

  @Bean
  public MessageListenerAdapter endlistenerAdapter(EndSubscriber subscriber) {
    return new MessageListenerAdapter(subscriber, "sendMessage");
  }

  @Bean
  public RedisTemplate<?, ?> redisTemplate() {
    RedisTemplate<byte[], byte[]> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory());
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(String.class));
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    return redisTemplate;
  }

  @Bean
  public RedisTemplate<String, Object> objRedisTemplate() {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory());
    redisTemplate.setKeySerializer(new StringRedisSerializer());
//    redisTemplate.setValueSerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(String.class));
    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashValueSerializer(new StringRedisSerializer());
//    redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    return redisTemplate;
  }

}
