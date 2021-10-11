package cn.ninanina.wushan.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 用于管理推荐给用户的视频，防止短时间内对同一用户推荐相同视频
 * <p>cache的键是appKey，表示当前会话的用户。值是已经推荐了的视频ID的集合。
 */
@Component("recommendCacheManager")
@Slf4j
public class RecommendCacheManager {
    @Autowired
    private RedisTemplate redisTemplate;

    @PostConstruct
    public void init() {
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(RedisSerializer.json());
    }

    /**
     * 保存对此appKey推荐了的videoId
     */
    public void save(String appKey, long videoId) {
        SetOperations setOperations = redisTemplate.opsForSet();
        setOperations.add(appKey, String.valueOf(videoId));
        redisTemplate.expire(appKey, 24, TimeUnit.HOURS);
    }

    /**
     * 查看是否对此appKey推荐了videoId
     */
    public boolean exist(String appKey, long videoId) {
        SetOperations setOperations = redisTemplate.opsForSet();
        return setOperations.isMember(appKey, String.valueOf(videoId));
    }

    /**
     * 获取用户被推荐的视频id总数
     */
    public long total(String appKey) {
        SetOperations setOperations = redisTemplate.opsForSet();
        return setOperations.size(appKey);
    }
}
