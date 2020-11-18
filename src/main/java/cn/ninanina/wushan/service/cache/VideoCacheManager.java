package cn.ninanina.wushan.service.cache;

import cn.ninanina.wushan.common.Constant;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.repository.VideoRepository;
import cn.ninanina.wushan.service.crawler.SeleniumCrawler;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 初始化这个cache时，从精选视频中获取，保存在best_videos.txt文件里面
 * 实现多级相关，直到视频数量达到3000-5000即可
 */
@Component("videoCacheManager")
@Slf4j
public class VideoCacheManager {
    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private SeleniumCrawler seleniumCrawler;

    //每当用户请求一个视频详情进行播放，都会将视频信息加入这个缓存。所以相当于用户都在看的视频。
    private LoadingCache<Long, VideoDetail> hotVideoCache;

    //保存实时的key，方便推荐。
    private List<Long> keyList;

    @PostConstruct
    public void init() {
        //暂时不设置过期，只通过LRU淘汰。
        //需要保证每个id都是有效的，一定不要存放空值。
        hotVideoCache = Caffeine.newBuilder()
                .initialCapacity(3000)
                .maximumSize(Constant.HOT_VIDEO_COUNT)
                .recordStats()
                .removalListener((RemovalListener<Long, VideoDetail>)
                        (aLong, videoDetail, removalCause) -> keyList.remove(aLong))
                .build(id -> {
                    VideoDetail videoDetail = videoRepository.findById(id).orElse(new VideoDetail());
                    keyList.add(videoDetail.getId());
                    return videoDetail;
                });
        //程序初始化时，先从数据库中取一些精选的视频，大概3000到5000个
        List<Long> bestIds = null;
        try {
            bestIds = loadBestVideoIds();
        } catch (IOException e) {
            e.printStackTrace();
        }
        keyList = new LinkedList<>(); //这个list要频繁增删
        if (!CollectionUtils.isEmpty(bestIds)) {
            log.info("start first layer initializing");
            for (Long id : bestIds) {
                saveVideo(videoRepository.getOne(id));
            }
            List<Long> ids = new ArrayList<>(keyList);
            log.info("start second layer initializing");
            for (long id : ids) {
                Set<Long> relatedIds = videoRepository.findRelatedVideoIds(id);
                relatedIds.addAll(videoRepository.findRelatedVideoIds_reverse(id));
                relatedIds.remove(id);
                for (Long relatedId : relatedIds) {
                    saveVideo(videoRepository.getOne(relatedId));
                }
            }
            log.info("start third layer initializing");
            int size = keyList.size();
            for (int i = bestIds.size(); i < size; i++) {
                long id = keyList.get(i);
                Set<Long> relatedIds = videoRepository.findRelatedVideoIds(id);
                relatedIds.addAll(videoRepository.findRelatedVideoIds_reverse(id));
                relatedIds.remove(id);
                for (Long relatedId : relatedIds) {
                    saveVideo(videoRepository.getOne(relatedId));
                }
                if (keyList.size() >= 3000) {
                    //启动SeleniumCrawler
//                    seleniumCrawler.start(this);
                    break;
                }
            }
        }

        log.info("hot video cache initialized, size: {}", hotVideoCache.estimatedSize());
    }

    public VideoDetail getVideo(long id) {
        return hotVideoCache.get(id);
    }

    public List<Long> getKeys() {
        return keyList;
    }

    public void saveVideo(VideoDetail videoDetail) {
        hotVideoCache.put(videoDetail.getId(), videoDetail);
        keyList.add(videoDetail.getId());
        log.info("put a video to cache, now size: {}", hotVideoCache.estimatedSize());
    }

    private static List<Long> loadBestVideoIds() throws IOException {
        List<Long> result = new ArrayList<>();
        ClassPathResource cpr = new ClassPathResource("static/best_videos.txt");
        byte[] keywordsData = FileCopyUtils.copyToByteArray(cpr.getInputStream());
        String[] strs = new String(keywordsData, StandardCharsets.UTF_8).split(" ");
        for (String s : strs) result.add(Long.parseLong(s));
        return result;
    }
}
