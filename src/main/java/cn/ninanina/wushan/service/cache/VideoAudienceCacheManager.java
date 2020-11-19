package cn.ninanina.wushan.service.cache;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理当前正在观看视频的观众人数，每当有人看视频是增加entry，没人看的时候马上淘汰。
 * <p>由于需要统计所有观众，包括登录和未登录的，因此不记录user信息
 * <p>缓存通过hashmap实现即可，不需要loadingCache。后期可以采用redis实现。
 */
@Component("videoAudienceCacheManager")
@Slf4j
public class VideoAudienceCacheManager {
    private final Map<Long, Integer> audienceCache;

    {
        audienceCache = new ConcurrentHashMap<>();
    }

    public void increase(long videoId) {
        audienceCache.merge(videoId, 1, Integer::sum);
    }

    public int get(long videoId) {
        return audienceCache.get(videoId) == null ? 0 : audienceCache.get(videoId);
    }

    public void decrease(long videoId) {
        audienceCache.merge(videoId, -1, Integer::sum);
        if (audienceCache.get(videoId) <= 0) {
            audienceCache.remove(videoId);
            log.info("video {} was removed from cache.", videoId);
        }
    }

    public void delete(long videoId) {
        audienceCache.remove(videoId);
    }

    /**
     * 获取视频观众排行，降序排列
     *
     * @return pair列表，左为视频id，右为观众数目
     */
    public List<Pair<Long, Integer>> rank(int limit) {
        List<Pair<Long, Integer>> list = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : audienceCache.entrySet()) {
            Pair<Long, Integer> pair = Pair.of(entry.getKey(), entry.getValue());
            list.add(pair);
        }
        list.sort((o1, o2) -> o2.getRight() - o1.getRight());
        if (limit <= list.size()) {
            return list.subList(0, limit);
        } else {
            return list;
        }
    }
}
