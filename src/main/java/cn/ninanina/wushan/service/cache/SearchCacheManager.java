package cn.ninanina.wushan.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 管理搜索缓存
 */
@Component
@Slf4j
public class SearchCacheManager {
    //cache的key为搜索词，value为一个Map；
    //Map的key为排序依据，value为搜索结果videoId的有序列表
    private Cache<String, Map<String, List<Long>>> searchCache;

    @PostConstruct
    public void init() {
        searchCache = Caffeine.newBuilder()
                .initialCapacity(30)
                .maximumSize(3000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build();
    }

    public Map<String, List<Long>> get(String query) {
        return searchCache.getIfPresent(query);
    }

    public void save(String query, Map<String, List<Long>> map) {
        searchCache.put(query, map);
    }
}
