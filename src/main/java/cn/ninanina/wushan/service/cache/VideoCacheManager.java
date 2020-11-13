package cn.ninanina.wushan.service.cache;

import cn.ninanina.wushan.common.Constant;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.repository.VideoRepository;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;

@Component("videoCacheManager")
@Slf4j
public class VideoCacheManager {
    @Autowired
    private VideoRepository videoRepository;

    //每当用户请求一个视频详情进行播放，都会将视频信息加入这个缓存。所以相当于用户都在看的视频。
    private LoadingCache<Long, VideoDetail> hotVideoCache;

    //保存实时的key，方便推荐。
    private List<Long> keyList;

    @PostConstruct
    public void init() {
        //暂时不设置过期，只通过LRU淘汰。
        //需要保证每个id都是有效的，一定不要存放空值。
        hotVideoCache = Caffeine.newBuilder()
                .initialCapacity(2500)
                .maximumSize(Constant.HOT_VIDEO_COUNT)
                .recordStats()
                .removalListener((RemovalListener<Long, VideoDetail>)
                        (aLong, videoDetail, removalCause) -> keyList.remove(aLong))
                .build(id -> {
                    VideoDetail videoDetail = videoRepository.findById(id).orElse(new VideoDetail());
                    keyList.add(videoDetail.getId());
                    return videoDetail;
                });
        //程序初始化时，先从数据库中取2500个播放数最高的视频
        List<VideoDetail> hotVideos = videoRepository.findHottest(2500);
        for (VideoDetail videoDetail : hotVideos)
            hotVideoCache.put(videoDetail.getId(), videoDetail);
        log.info("hot video cache initialized, size: {}", hotVideoCache.estimatedSize());

        keyList = new LinkedList<>(); //这个list要频繁增删
        keyList.addAll(hotVideoCache.asMap().keySet());
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
}
