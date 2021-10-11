package cn.ninanina.wushan.service.cache;

import cn.ninanina.wushan.domain.TagDetail;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.repository.TagRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component("tagCacheManager")
@Slf4j
public class TagCacheManager {
    @Autowired
    private TagRepository tagRepository;

    private LoadingCache<Long, TagDetail> tagCache;

    private Map<TagDetail, List<Long>> videoIdMap;

    @PostConstruct
    public void init() {
        tagCache = Caffeine.newBuilder()
                .initialCapacity(250)
                .maximumSize(10000)
                .build(key -> {
                    TagDetail tagDetail = tagRepository.findById(key).orElse(null);
                    if (tagDetail != null)
                        videoIdMap.put(tagDetail, tagRepository.findVideoIdsForTag(
                                tagDetail.getId(), 0, tagDetail.getVideoCount()));
                    return tagDetail;
                });
        videoIdMap = new HashMap<>();
        List<TagDetail> tags = tagRepository.findByVideoCountGreaterThan(1000);
        for (TagDetail tag : tags) {
            tagCache.put(tag.getId(), tag);
            List<Long> videoIds = tagRepository.findVideoIdsForTag(tag.getId(), 0, tag.getVideoCount());
            videoIdMap.put(tag, videoIds);
        }
        log.info("tag cache initialized, now size: {}", tagCache.estimatedSize());
        //每隔几个小时对所有tag下的videoId洗一次牌
        //如果客户端正在浏览时洗牌了，需要客户端做一个重复过滤
        ScheduledExecutorService shuffleTask = Executors.newScheduledThreadPool(1);
        shuffleTask.scheduleAtFixedRate(() -> {
            for (List<Long> ids : videoIdMap.values()) {
                Collections.shuffle(ids);
            }
            log.info("tag video ids have been shuffled.");
        }, 1, 6, TimeUnit.HOURS);
    }

    /**
     * 如果存在返回tag，否则返回null
     */
    public TagDetail getTag(long tagId) {
        return tagCache.get(tagId);
    }

    public List<Long> getVideoIdsOfTag(TagDetail tag) {
        if (!videoIdMap.containsKey(tag)) return null;
        return videoIdMap.get(tag);
    }
}
