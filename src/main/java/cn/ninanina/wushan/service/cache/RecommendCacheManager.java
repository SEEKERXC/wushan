package cn.ninanina.wushan.service.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用于管理推荐给用户的视频，防止同一用户同一设备重复推荐。
 * <p>cache的键是appKey，表示当前会话的用户。值是已经推荐了的视频ID的集合。
 * <p>cache的size大于服务器的会话数即可。
 */
@Component("recommendCacheManager")
@Slf4j
public class RecommendCacheManager {
    private LoadingCache<String, Set<Long>> recommendedCache;

    @PostConstruct
    public void init() {
        recommendedCache = Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterAccess(3600, TimeUnit.SECONDS)
                .removalListener((RemovalListener<String, Set<Long>>) (s, longs, removalCause) -> {
                    //TODO:appKey过期处理
                })
                .build(s -> new HashSet<>());
        log.info("recommend cache initialized.");
    }

    /**
     * 保存对此appKey推荐了的videoId
     */
    public void save(String appKey, long videoId) {
        Set<Long> set = recommendedCache.get(appKey);
        set.add(videoId);
    }

    /**
     * 查看是否对此appKey推荐了videoId
     */
    public boolean exist(String appKey, long videoId) {
        Set<Long> set = recommendedCache.get(appKey);
        return set.contains(videoId);
    }

    /**
     * 获取用户被推荐的视频id总数
     */
    public int total(String appKey) {
        return recommendedCache.get(appKey).size();
    }
}
