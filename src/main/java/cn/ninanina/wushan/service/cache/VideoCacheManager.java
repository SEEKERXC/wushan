package cn.ninanina.wushan.service.cache;

import cn.ninanina.wushan.common.util.CommonUtil;
import cn.ninanina.wushan.domain.TagDetail;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.repository.TagRepository;
import cn.ninanina.wushan.repository.VideoRepository;
import com.github.benmanes.caffeine.cache.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 初始化这个cache时，从精选视频中获取，保存在best_videos.txt文件里面
 * video对象都从redis中获取，目前设置3小时过期
 */
@Component("videoCacheManager")
@Slf4j
public class VideoCacheManager {
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private TagCacheManager tagCacheManager;
    @Autowired
    private RedisTemplate redisTemplate;

    private List<Long> bestIds;

    //定时任务，用于定期删除链接失效的videoId
    private ScheduledExecutorService scheduledExecutorService;

    @PostConstruct
    public void init() {
        bestIds = new ArrayList<>();
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(RedisSerializer.json());
        Set<Long> tempIds = new HashSet<>();
        try {
            loadBestVideoIds(tempIds);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!CollectionUtils.isEmpty(tempIds)) {
            log.info("start best ids initializing");
            Set<Long> ids = new HashSet<>(tempIds);
            for (long id : ids) { //一级相关就够了
                tempIds.addAll(videoRepository.findRelatedVideoIds(id));
                tempIds.addAll(videoRepository.findRelatedVideoIds_reverse(id));
            }
        }
        bestIds.addAll(tempIds);
        Collections.shuffle(bestIds);
        log.info("best ids initialized, size {}", bestIds.size());

        scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
    }

    /**
     * 根据id获取video对象，如果不存在，返回null
     */
    public VideoDetail getVideo(long id) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        if (!redisTemplate.hasKey(String.valueOf(id))) {
            VideoDetail videoDetail = videoRepository.findById(id).orElse(null);
            if (videoDetail == null) {
                return null;
            } else {
                loadTagsForVideo(videoDetail);
                valueOperations.set(String.valueOf(id), videoDetail, 3600 * 24, TimeUnit.SECONDS);
                return videoDetail;
            }
        } else return (VideoDetail) valueOperations.get(String.valueOf(id));
    }

    public VideoDetail getIfPresent(long videoId) {
        if (redisTemplate.hasKey(String.valueOf(videoId))) {
            ValueOperations valueOperations = redisTemplate.opsForValue();
            return (VideoDetail) valueOperations.get(String.valueOf(videoId));
        } else return null;
    }

    public List<Long> getBestIds() {
        return bestIds;
    }

    public void saveVideo(VideoDetail videoDetail) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set(String.valueOf(videoDetail.getId()), videoDetail, 10800, TimeUnit.SECONDS);
        log.info("put a video to cache, video id: {}", videoDetail.getId());
        if (CommonUtil.videoSrcValid(videoDetail.getSrc())) {
            ZSetOperations zsetOperations = redisTemplate.opsForZSet();
            zsetOperations.add("valid", String.valueOf(videoDetail.getId()), System.currentTimeMillis());
            log.info("put a video id to valid set. now size {}", zsetOperations.zCard("valid"));
        }
        videoRepository.save(videoDetail);
    }

    public void deleteVideo(VideoDetail videoDetail) {
        redisTemplate.delete(String.valueOf(videoDetail.getId()));
        videoRepository.delete(videoDetail);
    }

    /**
     * 获取所有有效视频id
     */
    public Set<Long> getValidIds() {
        ZSetOperations operations = redisTemplate.opsForZSet();
        long size = operations.zCard("valid");
        Set set = operations.range("valid", 0, size - 1);
        Set<Long> result = new HashSet<>();
        for (Object o : set) result.add(Long.parseLong(o.toString()));
        return result;
    }

    public void deleteValidId(long videoId) {
        ZSetOperations operations = redisTemplate.opsForZSet();
        long result = operations.remove("valid", String.valueOf(videoId));
        log.info("try to remove valid video id {}, result: {}", videoId, result);
    }

    private static void loadBestVideoIds(Set<Long> ids) throws IOException {
        ClassPathResource cpr = new ClassPathResource("static/best_videos.txt");
        byte[] keywordsData = FileCopyUtils.copyToByteArray(cpr.getInputStream());
        String[] strs = new String(keywordsData, StandardCharsets.UTF_8).split(" ");
        for (String s : strs) ids.add(Long.parseLong(s));
    }

    public void loadTagsForVideo(VideoDetail videoDetail) {
        videoDetail.setTags(new ArrayList<>());
        List<Long> tagIds = tagRepository.findTagIdsOfVideo(videoDetail.getId());
        for (long tagId : tagIds) {
            videoDetail.getTags().add(tagCacheManager.getTag(tagId));
        }
    }
}
