package cn.ninanina.wushan.service.crawler;

import cn.ninanina.wushan.domain.VideoJson;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * 这个类的作用只有一个：实时获取相关视频。这并不是爬虫。
 */
@Component("crawlerService")
@Slf4j
public class CrawlerService implements PageProcessor {

    private final Semaphore semaphore = new Semaphore(0);

    private String[] relatedUrls;

    private final Site site = Site.me()
            .setRetryTimes(3)
            .setTimeOut(3000)
            .setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.11 (KHTML, like Gecko) Ubuntu/18.04 Chromium/27.0.1453.93 Chrome/27.0.1453.93 Safari/537.36");

    private Spider spider;

    @Override
    public synchronized void process(Page page) {
        relatedUrls = extractRelatedUrls(page.getHtml().get());
        semaphore.release(1);
    }

    @Override
    public Site getSite() {
        return site;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public String[] getRelatedUrls() {
        return relatedUrls;
    }

    private String[] extractRelatedUrls(String html) {
        int startIndex = html.indexOf("<script>var video_related=") + 26;
        int endIndex = html.indexOf(";window.wpn_categories", startIndex);
        String json = html.substring(startIndex, endIndex);
        Gson gson = new Gson();
        VideoJson[] videoJsons = gson.fromJson(json, VideoJson[].class);
        String[] result = new String[videoJsons.length];
        for (int i = 0; i < result.length; i++) {
            VideoJson videoJson = videoJsons[i];
            result[i] = "https://www.xvideos.com" + videoJson.getU().replace('\\', '\0');
        }
        return result;
    }

    @PostConstruct
    public void init() {
        spider = Spider.create(this);
    }

    public Spider getSpider() {
        return spider;
    }
}
