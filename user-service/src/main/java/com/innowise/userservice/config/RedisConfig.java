package com.innowise.userservice.config;
import com.innowise.userservice.dto.UserWithCardsDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  @Bean
  public RedisTemplate<String, UserWithCardsDto> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, UserWithCardsDto> template = new RedisTemplate<>();
    template.setConnectionFactory(redisConnectionFactory);

    template.setKeySerializer(new StringRedisSerializer());

    JacksonJsonRedisSerializer<UserWithCardsDto> jacksonJsonRedisSerializer = new JacksonJsonRedisSerializer<>(UserWithCardsDto.class);
    template.setValueSerializer(jacksonJsonRedisSerializer);

    return template;
  }
}
