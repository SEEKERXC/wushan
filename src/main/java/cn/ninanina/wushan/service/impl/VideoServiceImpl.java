package cn.ninanina.wushan.service.impl;

import cn.ninanina.wushan.common.util.LuceneUtil;
import cn.ninanina.wushan.domain.Comment;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.domain.VideoDir;
import cn.ninanina.wushan.repository.CommentRepository;
import cn.ninanina.wushan.repository.UserRepository;
import cn.ninanina.wushan.repository.VideoDirRepository;
import cn.ninanina.wushan.repository.VideoRepository;
import cn.ninanina.wushan.service.VideoService;
import cn.ninanina.wushan.service.cache.RecommendCacheManager;
import cn.ninanina.wushan.service.cache.VideoAudienceCacheManager;
import cn.ninanina.wushan.service.cache.VideoCacheManager;
import cn.ninanina.wushan.service.crawler.CrawlerService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
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
    private VideoRepository videoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private VideoDirRepository videoDirRepository;

    @Autowired
    private CrawlerService crawlerService;

    private WebDriver driver;

    @PostConstruct
    public void init() {
        System.setProperty("webdriver.chrome.driver", "/opt/WebDriver/bin/chromedriver");
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", "127.0.0.1:9222");
        driver = new ChromeDriver(options);
        log.info("webdriver initialized. now title: {}", driver.getTitle());
    }

    @Override
    public List<VideoDetail> recommendVideos(@Nonnull User user, @Nonnull String appKey, @Nonnull Integer limit) {
        List<VideoDetail> viewedVideos = user.getViewedVideos();
        List<VideoDir> videoDirs = user.getVideoDirs();
        List<VideoDetail> collectedVideos = new ArrayList<>();
        for (VideoDir dir : videoDirs) {
            collectedVideos.addAll(dir.getCollectedVideos());
        }
        List<VideoDetail> downloadedVideos = user.getDownloadedVideos();
        if (CollectionUtils.isEmpty(viewedVideos)
                && CollectionUtils.isEmpty(collectedVideos)
                && CollectionUtils.isEmpty(downloadedVideos)) {
            return randomHotVideos(appKey, limit);
        }
        return null;
    }

    @Override
    public List<VideoDetail> randomHotVideos(@Nonnull String appKey, @Nonnull Integer limit) {
        List<Long> videoIdList = videoCacheManager.getKeys();
        List<VideoDetail> random10Videos = new ArrayList<>(limit);
        List<VideoDetail> result = new ArrayList<>(limit);
        //videoIdList是一个linkedList，长度小于3w，遍历一次id列表，随机取其中10个即可。
        Set<Integer> seqToFetch = new HashSet<>();
        Random random = new Random();
        while (seqToFetch.size() < limit) {
            int seq = random.nextInt(videoIdList.size());
            seqToFetch.add(seq);
        }
        int i = 0;
        for (Long id : videoIdList) {
            if (seqToFetch.contains(i)) {
                random10Videos.add(videoCacheManager.getVideo(id));
            }
            i++;
        }
        //如果重复了，替换
        //替换方法是，遍历videoIdList，如果遍历到的id没有被推荐过，那就推荐它
        //这样保证了最火热的视频优先推荐
        for (VideoDetail videoDetail : random10Videos) {
            if (!recommendCacheManager.exist(appKey, videoDetail.getId())) {
                result.add(videoDetail);
                recommendCacheManager.save(appKey, videoDetail.getId());
            } else {
                for (Long id : videoIdList) {
                    if (!recommendCacheManager.exist(appKey, id)) {
                        result.add(videoCacheManager.getVideo(id));
                        recommendCacheManager.save(appKey, id);
                        break;
                    }
                }
            }
        }
        log.info("user with appKey {} get random hot videos, total: {}",
                appKey, recommendCacheManager.total(appKey));
        return result;
    }

    @Override
    public VideoDetail getVideoDetail(@Nonnull Long videoId, User user) {
        VideoDetail detail = videoCacheManager.getVideo(videoId);
        if (StringUtils.isEmpty(detail.getSrc()) || !detail.getSrc().contains("xvideos")) {
            detail.setSrc(getSrc(detail.getUrl()));
            log.info("video {} get new video src: {}", videoId, detail.getSrc());
            videoCacheManager.saveVideo(detail);
            videoRepository.save(detail);
        }
        String src = detail.getSrc(); //这里要保证视频链接真实，虽然视频不一定有效
        long currentSeconds = System.currentTimeMillis() / 1000;
        long urlSeconds = Long.parseLong(src.substring(src.indexOf("?e=") + 3, src.indexOf("&h=")));
        long interval = 3600 * 5 / 2; //规定视频失效时间为2.5小时
        if (currentSeconds - urlSeconds >= interval) {
            detail.setSrc(getSrc(detail.getUrl()));
            log.info("update video src, videoId: {}, newSrc: {}", videoId, detail.getSrc());
            videoCacheManager.saveVideo(detail);
            videoRepository.save(detail);
        }
        if (user != null) {
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
    public List<VideoDetail> relatedVideos(@Nonnull Long videoId) {
        VideoDetail videoDetail = videoCacheManager.getVideo(videoId);
        Set<Long> relatedIds = videoRepository.findRelatedVideoIds(videoId);
        relatedIds.addAll(videoRepository.findRelatedVideoIds_reverse(videoId));
        relatedIds.remove(videoId);
        Set<VideoDetail> result = videoDetail.getRelated();
        if (CollectionUtils.isEmpty(result)) {
            result = new HashSet<>();
            videoDetail.setRelated(result);
        }
        for (long id : relatedIds) {
            result.add(videoRepository.getOne(id));
        }
        if (result.size() < 10) {
            log.info("related videos size {}, search for web.", result.size());
            new Thread(() -> {
                crawlerService.getSpider().addUrl(videoDetail.getUrl()).thread(1).run();
                try {
                    crawlerService.getSemaphore().acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String[] relatedUrls = crawlerService.getRelatedUrls();

                for (String url : relatedUrls) {
                    VideoDetail relatedVideo = videoRepository.findByUrl(url);
                    if (relatedVideo != null) {
                        if (videoRepository.existRelation(videoId, relatedVideo.getId()) <= 0)
                            videoRepository.insertRelated(videoId, relatedVideo.getId());
                    }
                }
            }).start();
        }
        log.info("get related videos, size: {}", result.size());
        return new ArrayList<>(result);
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
        for (int i = offset; i < offset + limit; i++) {
            ScoreDoc scoreDoc = topDocs.scoreDocs[i];
            Document document = indexSearcher.doc(scoreDoc.doc);
            long id = Long.parseLong(document.get("id"));
            VideoDetail videoDetail = videoRepository.getOne(id);
            result.add(videoDetail);
        }
        return result;
    }

    @Override
    public Comment commentOn(@Nonnull User user, @Nonnull Long videoId, @Nonnull String content, @Nullable Long parentId) {
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

    @Override
    public VideoDir createDir(@Nonnull User user, @Nonnull String name) {
        List<VideoDir> videoDirs = user.getVideoDirs();
        if (CollectionUtils.isEmpty(videoDirs)) {
            videoDirs = new ArrayList<>();
            user.setVideoDirs(videoDirs);
        }
        if (videoDirs.size() >= 50) {
            log.warn("user {} wanna create more than 50 dirs, refused.", user.getId());
            return null;
        }
        VideoDir videoDir = new VideoDir();
        videoDir.setName(name);
        videoDir.setCount(0);
        videoDir.setUser(user);
        videoDir.setCreateTime(System.currentTimeMillis());
        videoDir.setUpdateTime(System.currentTimeMillis());
        videoDir = videoDirRepository.save(videoDir);
        log.info("user created video dir, user id: {}, dir id: {}", user.getId(), videoDir.getId());
        return videoDir;
    }

    @Override
    public Boolean possessDir(@Nonnull User user, @Nonnull Long dirId) {
        List<VideoDir> dirs = user.getVideoDirs();
        VideoDir videoDir = videoDirRepository.getOne(dirId);
        return dirs.contains(videoDir);
    }

    @Override
    public void removeDir(@Nonnull Long id) {
        videoDirRepository.deleteById(id);
        log.info("removed dir, id:{}", id);
    }

    @Override
    public void renameDir(@Nonnull Long id, @Nonnull String name) {
        VideoDir dir = videoDirRepository.getOne(id);
        dir.setName(name);
        dir.setUpdateTime(System.currentTimeMillis());
        videoDirRepository.save(dir);
        log.info("renamed dir, id: {} new name: {}", id, name);
    }

    @Override
    public Boolean collect(@Nonnull Long videoId, @Nonnull Long dirId) {
        VideoDir dir = videoDirRepository.getOne(dirId);
        List<VideoDetail> collectedVideos = dir.getCollectedVideos();
        if (CollectionUtils.isEmpty(collectedVideos)) {
            collectedVideos = new ArrayList<>();
            dir.setCollectedVideos(collectedVideos);
        }
        VideoDetail videoDetail = videoCacheManager.getVideo(videoId);
        //已收藏过
        if (collectedVideos.contains(videoDetail)) {
            return false;
        }
        collectedVideos.add(videoDetail);
        dir.setUpdateTime(System.currentTimeMillis());
        dir.setCount(dir.getCount() + 1);
        log.info("collected video, dir id: {}, video id: {}", dirId, videoId);
        videoDirRepository.save(dir);
        return true;
    }

    @Override
    public Boolean cancelCollect(@Nonnull Long videoId, @Nonnull Long dirId) {
        VideoDir dir = videoDirRepository.getOne(dirId);
        List<VideoDetail> collectedVideos = dir.getCollectedVideos();
        if (CollectionUtils.isEmpty(collectedVideos)) {
            return false;
        }
        VideoDetail videoDetail = videoCacheManager.getVideo(videoId);
        if (!collectedVideos.contains(videoDetail)) return false;
        collectedVideos.remove(videoDetail);
        dir.setCount(dir.getCount() - 1);
        dir.setUpdateTime(System.currentTimeMillis());
        videoDirRepository.save(dir);
        log.info("canceled collect video, dir id: {}, video id: {}", dirId, videoId);
        return true;
    }

    @Override
    public List<VideoDir> collectedDirs(User user) {
        return user.getVideoDirs();
    }

    //TODO:分页获取看过的video，高优先级
    @Override
    public List<VideoDetail> viewedVideos(@Nonnull User user, @Nonnull Integer offset, @Nonnull Integer limit) {
        return null;
    }

    @Override
    public void download(@Nonnull User user, @Nonnull Long videoId) {
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

    private synchronized String getSrc(String url) {
        String result = "";
        driver.get(url);

        //点击下载按钮
        By downloadBtn = new By.ByCssSelector("#video-actions > ul > li:nth-child(2) > a");
        if (isElementExist(driver, downloadBtn)) {
            driver.findElement(downloadBtn).click();
        }

        //等待下载链接
        By href = new By.ByCssSelector("#tabDownload > p > a:nth-child(1)");
        if (isElementExist(driver, href)) {
            result = driver.findElement(href).getAttribute("href");
            log.info("get newest video src, url: {}, src: {}", url, result);
        } else {
            log.error("can't get download href!, errMsg: {}",
                    driver.findElement(new By.ByCssSelector("#tabDownload > h4")).getText());
        }
        return result;
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
