package cn.ninanina.wushan.service.cache;


import cn.ninanina.wushan.domain.DownloadInfo;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.repository.TagRepository;
import cn.ninanina.wushan.repository.VideoRepository;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p> xvideos下载API为https://www.xvideos.com/video-download/{视频id}/
 * <p> 需要登录，以session_token作为session识别号，并且每个session_token有下载频率限制
 * <p> 目前不清楚session的过期时间。但是实际运行后，session不可能过期，只可能超出限制。
 * <p> 目前采取的办法是：维护一个cookie队列，其中包含session_token，每次发送get请求都从队列取一个cookie出来，成功完成之后再入队。
 * <p> 如果token失效了，那么使用之后不入队，并且发出通知。
 * <p> cookie的增加由人工控制，登录了一个账号之后，复制cookie字符串加入队列。
 * <p> 为了方便管理，每个cookie串都有一个邮箱对应
 */
@Component("downloadManager")
@Slf4j
public class DownloadManager {
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private VideoCacheManager videoCacheManager;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostConstruct
    public void init() {
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(RedisSerializer.string());
    }

    public int addCookie(String email, String cookie) {
        ListOperations listOperations = redisTemplate.opsForList();
        long size = listOperations.size("cookie");
        String toSave = email + "divider" + cookie;
        for (long index = 0; index < size; index++) {
            String s = (String) listOperations.index("cookie", index);
            if (s.equals(toSave)) return Math.toIntExact(size);
        }
        listOperations.leftPush("cookie", toSave);
        log.info("add cookie successfully, new queue size {}", listOperations.size("cookie"));
        return Math.toIntExact(listOperations.size("cookie"));
    }

    public Pair<String, String> getCookie() {
        ListOperations listOperations = redisTemplate.opsForList();
        String strPair = (String) listOperations.rightPop("cookie");
        String email = strPair.substring(0, strPair.indexOf("divider"));
        String cookie = strPair.substring(strPair.indexOf("divider") + 7);
        return Pair.of(email, cookie);
    }

    public List<Pair<String, String>> getCookies(Integer offset, Integer limit) {
        List<Pair<String, String>> result = new ArrayList<>();
        ListOperations listOperations = redisTemplate.opsForList();
        long size = listOperations.size("cookie");
        if (offset == null || limit == null) {
            for (long index = 0; index < size; index++) {
                String s = (String) listOperations.index("cookie", index);
                String email = s.substring(0, s.indexOf("divider"));
                String cookie = s.substring(s.indexOf("divider") + 7);
                result.add(Pair.of(email, cookie));
            }
        }
        return result;
    }

    public boolean deleteCookie(String email) {
        ListOperations listOperations = redisTemplate.opsForList();
        Pair<String, String> pair = null;
        long size = listOperations.size("cookie");
        for (long index = 0; index < size; index++) {
            Pair<String, String> p = (Pair<String, String>) listOperations.index("cookie", index);
            if (p.getFirst().equals(email)) pair = p;
        }
        if (pair != null)
            return listOperations.remove("cookie", 1L, pair) > 0;
        else return false;
    }

    @SneakyThrows
    public void getVideoSrc(VideoDetail videoDetail) {
        ListOperations listOperations = redisTemplate.opsForList();
        CloseableHttpClient httpClient = HttpClients.createDefault();
        int originalId = Integer.parseInt(videoDetail.getUrl().substring(29, videoDetail.getUrl().indexOf('/', 29)));
        String url = "https://www.xvideos.com/video-download/" + originalId;
        HttpGet httpGet = new HttpGet(url);
        Pair<String, String> cookiePair = getCookie();
        if (cookiePair == null) {
            log.error("cookies are not enough!!!");
            return;
        }
        httpGet.addHeader("Cookie", cookiePair.getSecond());
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
        try {
            HttpEntity httpEntity = httpResponse.getEntity();
            String json = EntityUtils.toString(httpEntity, "UTF-8");
            EntityUtils.consume(httpEntity);
            Gson gson = new Gson();
            DownloadInfo result = gson.fromJson(json, DownloadInfo.class);
            if (result.getLOGGED().equals("false")) {
                log.error("cookie {} was timeout! now queue size {}", cookiePair.getFirst(), listOperations.size("cookie"));
                getVideoSrc(videoDetail);
                return;
            }
            if (!StringUtils.isEmpty(result.getERROR())) {
                log.error("getting src error, msg: {}", result.getERROR());
                if (result.getERROR().contains("delete") || result.getERROR().contains("\u522a\u9664")) {
                    log.warn("video with original id {} has been deleted while user getting src.", originalId);
                    videoCacheManager.deleteVideo(videoDetail);
                    addCookie(cookiePair.getFirst(), cookiePair.getSecond());
                } else {
                    log.error("cookie {} was overload! now queue size {}", cookiePair.getFirst(), listOperations.size("cookie"));
//                    getVideoSrc(videoDetail);
                }
                return;
            }
            if (StringUtils.isEmpty(result.getURL())) {
                log.error("cookie {} getting src of video {} failed, unknown error.", cookiePair.getFirst(), videoDetail.getId());
            }
            addCookie(cookiePair.getFirst(), cookiePair.getSecond());
            log.info("get src of video {} successfully, src: {}， session: {}", videoDetail.getId(), result.getURL(), cookiePair.getFirst());
            videoDetail.setSrc(result.getURL());
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
}
