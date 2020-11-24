package cn.ninanina.wushan.service.impl;

import cn.ninanina.wushan.common.util.LuceneUtil;
import cn.ninanina.wushan.domain.Comment;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.domain.Playlist;
import cn.ninanina.wushan.repository.CommentRepository;
import cn.ninanina.wushan.repository.UserRepository;
import cn.ninanina.wushan.repository.PlaylistRepository;
import cn.ninanina.wushan.repository.VideoRepository;
import cn.ninanina.wushan.service.VideoService;
import cn.ninanina.wushan.service.cache.RecommendCacheManager;
import cn.ninanina.wushan.service.cache.VideoAudienceCacheManager;
import cn.ninanina.wushan.service.cache.VideoCacheManager;
import cn.ninanina.wushan.service.driver.DriverManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>这是整个应用的核心类。
 * <p>注意：启动项目前需要先启动chrome。
 * <p>第一次开启chrome的命令：
 * <p>   nohup /opt/google/chrome/google-chrome --no-sandbox
 * --remote-debugging-port=9222 --user-data-dir="/home/data/chrome" &
 * <p>如果没有删除/home/data/chrome的数据，以后开启chrome可以添加--headless来节省内存
 * <p>保证使程序在9222端口操作chrome，并且后台运行
 */
@Service
@Slf4j
public class VideoServiceImpl implements VideoService {

    @Autowired
    private VideoCacheManager videoCacheManager;

    @Autowired
    private RecommendCacheManager recommendCacheManager;

    @Autowired
    private VideoAudienceCacheManager audienceCacheManager;

    @Autowired
    private DriverManager driverManager;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Override
    public List<VideoDetail> recommendVideos(Long userId, @Nonnull String appKey, @Nonnull String type, @Nonnull Integer limit) {
        switch (type) {
            case "hot":
                return hotVideos(appKey, limit);
            case "rihan":
                System.out.println("rihan");
                break;
            case "china":
                System.out.println("china");
                break;
            case "west":
                System.out.println("west");
                break;
            case "lesbian":
                System.out.println("lesbian");
                break;
        }
        User user = null;
        if (userId != null) user = userRepository.getOne(userId);
        if (user == null) return hotVideos(appKey, limit);
        List<VideoDetail> viewedVideos = user.getViewedVideos();
        List<Playlist> playlists = user.getPlaylists();
        List<VideoDetail> collectedVideos = new ArrayList<>();
        for (Playlist dir : playlists) {
            collectedVideos.addAll(dir.getCollectedVideos());
        }
        List<VideoDetail> downloadedVideos = user.getDownloadedVideos();
        if (CollectionUtils.isEmpty(viewedVideos)
                && CollectionUtils.isEmpty(collectedVideos)
                && CollectionUtils.isEmpty(downloadedVideos)) {
            return hotVideos(appKey, limit);
        }
        //TODO:根据收藏、下载、观看过的视频进行推荐
        return hotVideos(appKey, limit);
    }

    private List<VideoDetail> hotVideos(@Nonnull String appKey, @Nonnull Integer limit) {
        List<Long> videoIdList = videoCacheManager.getHotKeys();
        List<VideoDetail> result = new ArrayList<>(limit);
        //videoIdList是一个linkedList，长度小于3w，遍历一次id列表，取其中10个即可。
        for (Long id : videoIdList) {
            if (!recommendCacheManager.exist(appKey, id)) {
                VideoDetail videoDetail = videoCacheManager.getVideo(id);
                result.add(videoDetail);
                recommendCacheManager.save(appKey, id);
            }
            if (result.size() >= limit) break;
        }
        log.info("user with appKey {} get hot videos, now total: {}",
                appKey, recommendCacheManager.total(appKey));
        return result;
    }

    @Override
    public VideoDetail getVideoDetail(@Nonnull Long videoId, Long userId) {
        VideoDetail detail = videoCacheManager.getVideo(videoId);
        if (StringUtils.isEmpty(detail.getSrc()) || !detail.getSrc().contains("xvideos")) {
            getVideoSrc(detail);
            if (StringUtils.isEmpty(detail.getSrc())) {//这里表示视频已经被网站删除，下面同理
                return detail;
            }
        }
        String src = detail.getSrc(); //这里要保证视频链接真实，虽然视频不一定有效
        long currentSeconds = System.currentTimeMillis() / 1000;
        long urlSeconds = Long.parseLong(src.substring(src.indexOf("?e=") + 3, src.indexOf("&h="))) - 1800;
        if (currentSeconds >= urlSeconds) {
            getVideoSrc(detail);
            if (StringUtils.isEmpty(detail.getSrc())) return detail;
        }
        if (userId != null) {
            User user = userRepository.getOne(userId);
            List<VideoDetail> viewedVideos = user.getViewedVideos();
            if (CollectionUtils.isEmpty(viewedVideos)) {
                viewedVideos = new ArrayList<>();
                user.setViewedVideos(viewedVideos);
            }
            viewedVideos.add(detail);
            userRepository.save(user);
        }
        audienceCacheManager.increase(videoId);
        return detail;
    }

    //TODO:获取相关视频，高优先级
    @Override
    public List<VideoDetail> relatedVideos(@Nonnull Long videoId, @Nonnull Integer offset, @Nonnull Integer limit) {
        List<Long> relatedIds = videoRepository.findRelatedVideoIds(videoId);
        List<Long> reverseIds = videoRepository.findRelatedVideoIds_reverse(videoId);
        for (Long id : reverseIds) {
            if (!relatedIds.contains(id)) relatedIds.add(id);
        }
        relatedIds.remove(videoId);
        List<VideoDetail> result = new ArrayList<>(limit);
        for (long id : relatedIds) {
            if (--offset >= 0) continue;
            videoRepository.findById(id).ifPresentOrElse(result::add, () -> videoRepository.deleteFromRelated(id));
            if (result.size() >= limit) break;
        }
        if (result.size() < limit) {
            for (Long id : relatedIds) {
                boolean completed = false;
                List<Long> secondRelatedIds = videoRepository.findRelatedVideoIds(id);
                List<Long> secondReverseIds = videoRepository.findRelatedVideoIds_reverse(id);
                for (Long relatedId : secondRelatedIds) {
                    if (!relatedIds.contains(relatedId)) relatedIds.add(relatedId);
                }
                for (Long reverseId : secondReverseIds) {
                    if (!relatedIds.contains(reverseId))
                        relatedIds.add(reverseId);
                }
                for (Long relatedId : relatedIds) {
                    if (--offset >= 0) continue;
                    videoRepository.findById(relatedId).ifPresentOrElse(result::add, () -> videoRepository.deleteFromRelated(relatedId));
                    if (result.size() >= limit) {
                        completed = true;
                        break;
                    }
                }
                if (completed) break;
            }
        }
        return result;
    }

    //TODO:设置同义词
    @SneakyThrows
    @Override
    public List<VideoDetail> search(@Nonnull String word, @Nonnull Integer offset, @Nonnull Integer limit) {
        List<VideoDetail> result = new ArrayList<>();
        IndexSearcher indexSearcher = LuceneUtil.get().getIndexSearcher();
        String[] fields = {"title", "titleZh", "tagZh"};
        QueryParser parser = new MultiFieldQueryParser(Version.LUCENE_4_10_4, fields, LuceneUtil.get().getAnalyzer());
        Query query = parser.parse(word);
        TopDocs topDocs = indexSearcher.search(query, offset + limit);
        for (int i = offset; i < offset + limit && i < topDocs.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = topDocs.scoreDocs[i];
            Document document = indexSearcher.doc(scoreDoc.doc);
            long id = Long.parseLong(document.get("id"));
            videoRepository.findById(id).ifPresent(result::add);
            //TODO：删除被删除视频的索引
        }
        return result;
    }

    @Override
    public Comment commentOn(@Nonnull Long userId, @Nonnull Long videoId, @Nonnull String content, @Nullable Long parentId) {
        User user = userRepository.getOne(userId);
        Comment comment = new Comment();
        comment.setContent(content);
        //被评论的视频需要放到热点视频中
        VideoDetail videoDetail = videoCacheManager.getVideo(videoId);
        if (videoDetail == null) return null;
        comment.setVideo(videoDetail);
        comment.setUser(user);
        comment.setApproved(0);
        if (parentId != null) comment.setParentId(parentId);
        comment.setTime(System.currentTimeMillis());
        comment = commentRepository.save(comment);
        return comment;
    }

    //TODO:分页获取看过的video，高优先级
    @Override
    public List<VideoDetail> viewedVideos(@Nonnull Long userId, @Nonnull Integer offset, @Nonnull Integer limit) {
        return null;
    }

    @Override
    public void download(@Nonnull Long userId, @Nonnull Long videoId) {
        User user = userRepository.getOne(userId);
        VideoDetail videoDetail = videoRepository.getOne(videoId);
        List<VideoDetail> downloadedVideos = user.getDownloadedVideos();
        if (downloadedVideos == null) {
            downloadedVideos = new ArrayList<>();
            user.setDownloadedVideos(downloadedVideos);
        }
        downloadedVideos.add(videoDetail);
        userRepository.save(user);
    }

    @Override
    public void exitDetail(@Nonnull Long videoId) {
        audienceCacheManager.decrease(videoId);
    }

    @Override
    public int audiences(@Nonnull Long videoId) {
        return audienceCacheManager.get(videoId);
    }

    @Override
    public List<Pair<VideoDetail, Integer>> onlineRank(@Nonnull Integer limit) {
        List<Pair<VideoDetail, Integer>> result = new ArrayList<>();
        List<Pair<Long, Integer>> rank = audienceCacheManager.rank(limit);
        for (Pair<Long, Integer> original : rank) {
            VideoDetail videoDetail = videoCacheManager.getVideo(original.getLeft());
            result.add(Pair.of(videoDetail, original.getRight()));
        }
        return result;
    }

    /**
     * <p>从浏览器获取视频有效链接
     * <p>实现步骤为，从当前可用的机器的浏览器驱动队列中取出一个进行操作
     * <p>如果操作失败了，那台机器的账号达到次数限制，此驱动将不入队，发出警告，
     * 然后手动恢复之后手动调用接口入队。如果操作成功，则必须立马归队。
     * 参见{@link cn.ninanina.wushan.web.CommonController#registerDriver(String, String, Integer)}
     * <p>为了尽可能保证下载次数不达到限制，必须平衡QPS与机器数。
     * <p>这是一个相当耗时的操作，一般需要5-10秒
     */
    private void getVideoSrc(VideoDetail videoDetail) {
        //TODO：获取有效的浏览器driver
        String url = videoDetail.getUrl();
        Pair<String, WebDriver> pair = driverManager.access();
        WebDriver driver = pair.getRight();
        driver.get(url);

        //TODO:对于没有评论按钮的页面做特殊处理。高优先级
        //点击下载按钮
        By downloadBtn;
        if (isElementExist(driver, new By.ByCssSelector("#tabComments_btn")))
            downloadBtn = new By.ByCssSelector("#video-actions > ul > li:nth-child(2) > a");
        else downloadBtn = new By.ByCssSelector("#video-actions > ul > li:nth-child(1) > a");
        if (isElementExist(driver, downloadBtn)) {
            driver.findElement(downloadBtn).click();
        } else {
            //没有下载按钮，表示这个视频已经从网站删除，这里也应该删除
            //返回空src表示视频已经被删除
            driverManager.restore(pair);
            videoDetail.setSrc("");
            videoRepository.delete(videoDetail);
            videoCacheManager.removeVideo(videoDetail);
            audienceCacheManager.delete(videoDetail.getId());
            log.warn("video {} has been deleted while fetching source.", videoDetail.getId());
            return;
        }
        //等待下载链接
        By href = new By.ByCssSelector("#tabDownload > p > a:nth-child(1)");
        if (isElementExist(driver, href)) {
            String src = driver.findElement(href).getAttribute("href");
            videoDetail.setSrc(src);
            videoDetail.setUpdateTime(System.currentTimeMillis());
            log.info("update video src, videoId: {}, newSrc: {}", videoDetail.getId(), videoDetail.getSrc());
            videoCacheManager.saveVideo(videoDetail);
        } else {
            //TODO:发出警告，提示此机器不可用，此driver不归队
            //转移请求
            getVideoSrc(videoDetail);
            log.error("can't get download href!, driver addr: {}", "localhost");
        }
        videoRepository.save(videoDetail);
    }

    private boolean isElementExist(WebDriver driver, By locator) {
        driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
        try {
            driver.findElement(locator);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
        }
    }

}
