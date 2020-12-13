package cn.ninanina.wushan.service.cache;

import cn.ninanina.wushan.domain.VideoDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 管理当前有人在看的video，每当有人看视频时更新entry，没人看的时候马上删除。
 * <p>由于需要统计所有观众，包括登录和未登录的，因此不记录user信息
 * <p>通过TreeMap实现排序即可，不需要loadingCache。后期可以采用redis实现。
 * <p>现在不用redis，因为不想增加系统复杂度，没时间维护..
 */
@Component("AudienceManager")
@Slf4j
public class AudienceManager {
    @Autowired
    private RedisTemplate redisTemplate;

    @PostConstruct
    public void init() {
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(RedisSerializer.string());
    }

    public void increase(long videoId) {
        ZSetOperations operations = redisTemplate.opsForZSet();
        Long rank = operations.rank("audience", videoId);
        if (rank != null && rank > 0)
            operations.incrementScore("audience", videoId, 1);
        else operations.add("audience", videoId, 1);
    }

    public void decrease(long videoId) {
        ZSetOperations operations = redisTemplate.opsForZSet();
        Long rank = operations.rank("audience", videoId);
        if (rank != null && rank > 0) {
            operations.incrementScore("audience", videoId, -1);
            if (operations.score("audience", videoId) <= 0) operations.remove("audience", videoId);
        }
    }

    public int get(long videoId) {
        ZSetOperations operations = redisTemplate.opsForZSet();
        Long rank = operations.rank("audience", videoId);
        if (rank != null && rank > 0) {
            return operations.score("audience", videoId).intValue();
        }
        return 0;
    }

    /**
     * 获取视频观众排行，降序排列
     */
    public List<Pair<Long, Integer>> rank(int offset, int limit) {
        List<Pair<Long, Integer>> result = new ArrayList<>();
        ZSetOperations operations = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple> set = operations.reverseRangeWithScores("audience", offset, offset + limit);
        for (ZSetOperations.TypedTuple typedTuple : set) {
            result.add(Pair.of((Long) typedTuple.getValue(), typedTuple.getScore().intValue()));
        }
        return result;
    }
}