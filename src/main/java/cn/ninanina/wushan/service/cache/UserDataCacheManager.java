package cn.ninanina.wushan.service.cache;

import cn.ninanina.wushan.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 这个类管理用户的数据，包括用户看过的视频id、用户喜欢/不喜欢的视频id、收藏的视频id、下载的视频id
 */
@Component("userDataCacheManager")
@Slf4j
public class UserDataCacheManager {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ViewedRepository viewedRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private DislikeRepository dislikeRepository;
    @Autowired
    private PlaylistRepository playlistRepository;
    @Autowired
    private DownloadRepository downloadRepository;

    @PostConstruct
    public void init() {
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(RedisSerializer.json());
    }

    public List<Long> getViewedIds(long userId) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String key = "user_viewed_" + userId;
        List<Long> ids;
        if (redisTemplate.hasKey(key)) {
            ids = (List<Long>) valueOperations.get(key);
            log.info("user {} get viewed ids from cache, size {}", userId, ids.size());
        } else {
            ids = viewedRepository.findViewedIds(userId);
            valueOperations.set(key, ids, 2, TimeUnit.HOURS);
        }
        return ids;
    }

    public List<Long> getDownloadedIds(long userId) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String key = "user_downloaded_" + userId;
        List<Long> ids;
        if (redisTemplate.hasKey(key)) {
            ids = (List<Long>) valueOperations.get(key);
        } else {
            ids = downloadRepository.findVideoIdsByUserId(userId);
            valueOperations.set(key, ids, 2, TimeUnit.HOURS);
        }
        return ids;
    }

    public List<Long> getCollectedIds(long userId) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String key = "user_collected_" + userId;
        List<Long> ids;
        if (redisTemplate.hasKey(key)) {
            ids = (List<Long>) valueOperations.get(key);
        } else {
            List<Long> playlistIds = playlistRepository.findAllPlaylistIds(userId);
            ids = new ArrayList<>();
            for (long playlistId : playlistIds) {
                ids.addAll(playlistRepository.findAllVideoIds(playlistId));
            }
            valueOperations.set(key, ids, 2, TimeUnit.HOURS);
        }
        return ids;
    }

    public List<Long> getLikedIds(long userId) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String key = "user_liked_" + userId;
        List<Long> ids;
        if (redisTemplate.hasKey(key)) {
            ids = (List<Long>) valueOperations.get(key);
        } else {
            ids = likeRepository.findByUserId(userId);
            valueOperations.set(key, ids, 2, TimeUnit.HOURS);
        }
        return ids;
    }

    public List<Long> getDislikedIds(long userId) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String key = "user_disliked_" + userId;
        List<Long> ids;
        if (redisTemplate.hasKey(key)) {
            ids = (List<Long>) valueOperations.get(key);
        } else {
            ids = dislikeRepository.findVideoIdsByUserId(userId);
            valueOperations.set(key, ids, 2, TimeUnit.HOURS);
        }
        return ids;
    }
}
