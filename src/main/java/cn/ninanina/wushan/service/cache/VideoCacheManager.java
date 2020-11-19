package cn.ninanina.wushan.service.cache;

import cn.ninanina.wushan.common.Constant;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.repository.VideoRepository;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 初始化这个cache时，从精选视频中获取，保存在best_videos.txt文件里面
 * 实现多级相关，直到视频数量达到1500即可
 */
@Component("videoCacheManager")
@Slf4j
public class VideoCacheManager {
    @Autowired
    private VideoRepository videoRepository;

    //保存最值得推荐的视频，永远不过期，但是需要不断地更新
    //目的是减轻mysql压力
    private LoadingCache<Long, VideoDetail> hotVideoCache;

    //保存hot videos的key，方便推荐。
    private List<Long> hotKeyList;

    @PostConstruct
    public void init() {
        //暂时不设置过期，只通过LRU淘汰。
        //需要保证每个id都是有效的，一定不要存放空值。
        hotVideoCache = Caffeine.newBuilder()
                .initialCapacity(1000)
                .maximumSize(Constant.HOT_VIDEO_COUNT)
                .recordStats()
                .removalListener((RemovalListener<Long, VideoDetail>)
                        (aLong, videoDetail, removalCause) -> hotKeyList.remove(aLong))
                .build(id -> {
                    VideoDetail videoDetail = videoRepository.findById(id).orElse(new VideoDetail());
                    hotKeyList.add(videoDetail.getId());
                    return videoDetail;
                });
        //程序初始化时，先从数据库中取一些精选的视频，大概1500个
        List<Long> bestIds = null;
        try {
            bestIds = loadBestVideoIds();
        } catch (IOException e) {
            e.printStackTrace();
        }
        hotKeyList = new LinkedList<>();
        if (!CollectionUtils.isEmpty(bestIds)) {
            log.info("start first layer initializing");
            for (Long id : bestIds) {
                saveVideo(videoRepository.getOne(id));
            }
            List<Long> ids = new ArrayList<>(hotKeyList);
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
            int size = hotKeyList.size();
            for (int i = bestIds.size(); i < size; i++) {
                long id = hotKeyList.get(i);
                Set<Long> relatedIds = videoRepository.findRelatedVideoIds(id);
                relatedIds.addAll(videoRepository.findRelatedVideoIds_reverse(id));
                relatedIds.remove(id);
                for (Long relatedId : relatedIds) {
                    saveVideo(videoRepository.getOne(relatedId));
                }
                if (hotKeyList.size() >= 1500) {
                    break;
                }
            }
            Collections.shuffle(hotKeyList);
        }

        log.info("hot video cache initialized, size: {}", hotVideoCache.estimatedSize());

    }

    public VideoDetail getVideo(long id) {
        return hotVideoCache.get(id);
    }

    public List<Long> getHotKeys() {
        return hotKeyList;
    }

    public void saveVideo(VideoDetail videoDetail) {
        hotVideoCache.put(videoDetail.getId(), videoDetail);
        hotKeyList.remove(videoDetail.getId());
        hotKeyList.add(0, videoDetail.getId());
        log.info("put a video to cache, video id: {} now size: {}", videoDetail.getId(), hotVideoCache.estimatedSize());
    }

    //在hotCache和validCache中都移除这个video。用于移除失效video
    public void removeVideo(VideoDetail videoDetail) {
        hotVideoCache.invalidate(videoDetail.getId());
        hotKeyList.remove(videoDetail.getId());
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
