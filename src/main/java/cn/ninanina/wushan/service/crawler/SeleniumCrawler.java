package cn.ninanina.wushan.service.crawler;

import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.repository.VideoRepository;
import cn.ninanina.wushan.service.cache.VideoCacheManager;
import cn.ninanina.wushan.service.impl.VideoServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 这个类用于获取指定的1000多个视频的实时链接
 * 当videoCache初始化完成后自启动
 * xvideos对单个账号有下载限制
 * 目前不清楚多少时间内可以下载多少次，只能先降低下载频率试试。
 */
@Component("seleniumCrawler")
@Slf4j
public class SeleniumCrawler {
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private VideoServiceImpl videoService;

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    /**
     * 下载cache中的视频，20秒下载一个
     * 一个小时可以下载180个，3小时即540个。
     */
    public void start(VideoCacheManager cacheManager) {
        List<Long> keys = cacheManager.getKeys();
        final int[] index = {0};
        executorService.scheduleAtFixedRate(() -> {
            long id = keys.get(index[0]);
            index[0]++;
            if (index[0] >= 3000) index[0] = 0;
            VideoDetail videoDetail = videoRepository.getOne(id);
            log.info("get video src of {}", videoDetail.getId());
            String src = videoService.getSrc(videoDetail);
            videoDetail.setSrc(src);
            videoRepository.save(videoDetail);
        }, 0, 20, TimeUnit.SECONDS);


    }
}
