package cn.ninanina.wushan.web.cache;

import cn.ninanina.wushan.common.Constant;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 这个类用于活跃用户，token值在每次注册/登录之后都返回给客户端，登录了的客户端发出任何请求都要请求cache
 * token代替sessionId
 */
@Component("userCacheManager")
@Slf4j
public class UserCacheManager {
    private Cache<String, Long> userCache;

    @PostConstruct
    public void init() {
        userCache = Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(Constant.USER_SCALE)
                .expireAfterAccess(3600, TimeUnit.SECONDS)
                .build();
    }

    public void save(String token, Long userId) {
        userCache.put(token, userId);
    }

    public Long get(String token) {
        return userCache.getIfPresent(token);
    }

    public void delete(String token) {
        userCache.invalidate(token);
    }
}
