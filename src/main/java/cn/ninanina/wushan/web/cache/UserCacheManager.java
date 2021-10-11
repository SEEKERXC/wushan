package cn.ninanina.wushan.web.cache;

import cn.ninanina.wushan.common.Constant;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 这个类用于活跃用户，token值在每次注册/登录之后都返回给客户端，登录了的客户端发出任何请求都要请求cache
 * token代替sessionId
 * 同时在本地和redis保存，每次请求都先访问本地，如果没有再访问redis
 */
@Component("userCacheManager")
@Slf4j
public class UserCacheManager {
    private Cache<String, Long> userCache;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostConstruct
    public void init() {
        userCache = Caffeine.newBuilder()
                .initialCapacity(50)
                .expireAfterAccess(3600, TimeUnit.SECONDS)
                .build();
    }

    public void save(String token, Long userId) {
        userCache.put(token, userId);
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set("user_token_" + token, String.valueOf(userId), 3600, TimeUnit.SECONDS);
    }

    public Long get(String token) {
        Long userid = userCache.getIfPresent(token);
        if (userid == null) {
            ValueOperations valueOperations = redisTemplate.opsForValue();
            String s = String.valueOf(valueOperations.get("user_token_" + token));
            if (!StringUtils.isEmpty(s) && !s.equals("null"))
                userid = Long.parseLong(s);
        }
        return userid;
    }

    public void delete(String token) {
        userCache.invalidate(token);
    }
}
