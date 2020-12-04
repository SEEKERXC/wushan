package cn.ninanina.wushan.service.driver;

import cn.ninanina.wushan.common.BrowserResult;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.repository.VideoRepository;
import cn.ninanina.wushan.service.cache.VideoAudienceCacheManager;
import cn.ninanina.wushan.service.cache.VideoCacheManager;
import cn.ninanina.wushan.web.result.ResultMsg;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * 管理selenium的WebDriver，负责各个浏览器之间的负载平衡，并且支持动态增删浏览器，以维持服务可用
 * 负载平衡策略为：采用队列进行负载平衡。取用webdriver的时候出队，用完了入队。如果用的时候发现失效，则不入队。
 */
@Component("driverManager")
@Slf4j
public class DriverManager {
    @Autowired
    private VideoCacheManager videoCacheManager;
    @Autowired
    private VideoAudienceCacheManager audienceCacheManager;
    @Autowired
    private VideoRepository videoRepository;

    private BlockingDeque<String> ipQueue;

    @PostConstruct
    public void init() {
        ipQueue = new LinkedBlockingDeque<>();
    }

    /**
     * 增加指定机器的浏览器
     */
    public boolean register(String ip) {
        if (ipQueue.contains(ip)) {
            log.error("ip {} has been registered, cannot register again!", ip);
            return false;
        }
        ipQueue.offer(ip);
        log.info("new browser registered, ip: {}", ip);
        return true;
    }

    /**
     * 调用队头的浏览器，获取视频链接，完成后归队，若返回错误信息则进行相应处理
     */
    @SneakyThrows
    public void getVideoSrc(VideoDetail videoDetail) {
        String url = videoDetail.getUrl();
        String ip = "";
        try {
            ip = ipQueue.poll(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            //TODO:这里发出严重警告，机器资源不够用
        }
        String path = "http://" + ip + "/videosrc";
        CloseableHttpClient httpClient = HttpClients.createDefault();
        List<BasicNameValuePair> paramsList = new ArrayList<>();
        paramsList.add(new BasicNameValuePair("url", String.valueOf(url)));
        HttpPost httpPost = new HttpPost(path);
        httpPost.setEntity(new UrlEncodedFormEntity(paramsList, "UTF-8"));
        log.info("request on {} for video {} src.", ip, url);
        CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
        try {
            HttpEntity httpEntity = httpResponse.getEntity();
            String json = EntityUtils.toString(httpEntity, "UTF-8");
            EntityUtils.consume(httpEntity);
            Gson gson = new Gson();
            BrowserResult result = gson.fromJson(json, BrowserResult.class);
            if (result.getRspCode().equals(ResultMsg.SUCCESS.getCode())) {
                restore(ip);
                videoDetail.setSrc(result.getData());
                videoDetail.setUpdateTime(System.currentTimeMillis());
                log.info("update video src, videoId: {}, newSrc: {}", videoDetail.getId(), videoDetail.getSrc());
                videoCacheManager.saveVideo(videoDetail);
                videoRepository.save(videoDetail);
            } else if (result.getRspCode().equals(ResultMsg.VIDEO_INVALID.getCode())) {
                restore(ip);
                videoRepository.delete(videoDetail);
                videoCacheManager.removeVideo(videoDetail);
                audienceCacheManager.delete(videoDetail.getId());
                log.warn("video {} has been deleted while fetching source.", videoDetail.getId());
                videoDetail.setSrc("");
            } else if (result.getRspCode().equals(ResultMsg.BROWSER_INVALID.getCode())) {
                log.error("browser crashed! ip:{}", ip);
                getVideoSrc(videoDetail);
            }
        } finally {
            try {
                if (httpResponse != null) {
                    httpResponse.close();
                    httpClient.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void restore(String ip) {
        ipQueue.offer(ip);
    }

}
