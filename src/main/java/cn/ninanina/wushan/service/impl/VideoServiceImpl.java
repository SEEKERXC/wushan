package cn.ninanina.wushan.service.impl;

import cn.ninanina.wushan.common.util.CommonUtil;
import cn.ninanina.wushan.common.util.LuceneUtil;
import cn.ninanina.wushan.domain.*;
import cn.ninanina.wushan.repository.*;
import cn.ninanina.wushan.service.VideoService;
import cn.ninanina.wushan.service.cache.RecommendCacheManager;
import cn.ninanina.wushan.service.cache.TagCacheManager;
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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
    private TagCacheManager tagCacheManager;

    @Autowired
    private DriverManager driverManager;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PlaylistRepository playlistRepository;

    @Override
    public List<VideoDetail> recommendVideos(Long userId, @Nonnull String appKey, @Nonnull String type, @Nonnull Integer offset, @Nonnull Integer limit) {
        List<TagDetail> tags = null;
        switch (type) {
            case "hot": //最新更新链接的视频
                return hotVideos(appKey, limit);
            case "recommend": //根据用户的观看、收藏、下载与不喜欢等行为进行推荐
                List<VideoDetail> recommendResult = new ArrayList<>();
                if (userId == null) return hotVideos(appKey, limit);
                List<Long> viewedVideoIds = videoRepository.findViewedIds(userId);
                List<Long> playlistIds = playlistRepository.findAllPlaylistIds(userId);
                List<Long> collectedVideoIds = new ArrayList<>();
                for (long playlistId : playlistIds) {
                    collectedVideoIds.addAll(playlistRepository.findAllVideoIds(playlistId));
                }
                if (CollectionUtils.isEmpty(viewedVideoIds)
                        && CollectionUtils.isEmpty(collectedVideoIds)) {
                    return hotVideos(appKey, limit);
                }
                Collections.shuffle(viewedVideoIds);
                Collections.shuffle(collectedVideoIds);
                List<Long> ids = new ArrayList<>();
                ids.addAll(collectedVideoIds);
                ids.addAll(viewedVideoIds);
                for (long id : ids) {
                    List<Long> relatedIds = getRelatedVideoIds(id);
                    for (long relatedId : relatedIds) {
                        if (!recommendCacheManager.exist(appKey, relatedId) //TODO:添加下载过的和不喜欢的
                                && !collectedVideoIds.contains(relatedId)) { //暂定观看过的视频仍然可以推荐
                            VideoDetail video = videoCacheManager.getIfPresent(relatedId);
                            if (video == null)  //TODO:配置redis缓存，一级缓存如果没有，先从redis缓存中获取，没有再访问数据库。
                                videoRepository.findById(relatedId).ifPresent(recommendResult::add);
                            else recommendResult.add(video);
                            recommendCacheManager.save(appKey, relatedId);
                        }
                        if (recommendResult.size() >= limit) break;
                    }
                    if (recommendResult.size() >= limit) break;
                }
                return recommendResult;
            case "asian":
                //标签id
                long asian = 1551;
                long japanese = 1400;
                long jav = 1738;
                long chinese = 1555;
                long china = 4204;
                tags = new ArrayList<>() {{
                    add(tagCacheManager.getTag(asian));
                    add(tagCacheManager.getTag(japanese));
                    add(tagCacheManager.getTag(jav));
                    add(tagCacheManager.getTag(chinese));
                    add(tagCacheManager.getTag(china));
                }};
                break;
            case "west":
                long european = 1553;
                long euro = 1601;
                long blonde = 1488;
                long pornstar = 1743;
                tags = new ArrayList<>() {{
                    add(tagCacheManager.getTag(european));
                    add(tagCacheManager.getTag(euro));
                    add(tagCacheManager.getTag(blonde));
                    add(tagCacheManager.getTag(pornstar));
                }};
                break;
            case "lesbian":
                long lesbian = 1495;
                long lesbians = 1497;
                long lesbo = 2068;
                long lesbian_sex = 1912;
                long lesbian_porn = 3106;
                long lesbiansex = 4076;
                tags = new ArrayList<>() {{
                    add(tagCacheManager.getTag(lesbian));
                    add(tagCacheManager.getTag(lesbians));
                    add(tagCacheManager.getTag(lesbo));
                    add(tagCacheManager.getTag(lesbian_sex));
                    add(tagCacheManager.getTag(lesbian_porn));
                    add(tagCacheManager.getTag(lesbiansex));
                }};
                break;
        }
        assert tags != null;
        Collections.shuffle(tags);
        List<VideoDetail> result = new ArrayList<>();
        Random random = new Random();
        do {
            for (TagDetail tag : tags) {
                List<Long> videoIdsOfTag = tagCacheManager.getVideoIdsOfTag(tag);
                int luckyGuy = random.nextInt(videoIdsOfTag.size());
                while (recommendCacheManager.exist(appKey, videoIdsOfTag.get(luckyGuy))) {
                    luckyGuy = random.nextInt(videoIdsOfTag.size());
                }
                videoRepository.findById(videoIdsOfTag.get(luckyGuy)).ifPresent(result::add);
                recommendCacheManager.save(appKey, videoIdsOfTag.get(luckyGuy));
                if (result.size() >= limit) break;
            }
        } while (result.size() < limit);
        return result;
    }

    private List<VideoDetail> hotVideos(@Nonnull String appKey, @Nonnull Integer limit) {
        List<Long> videoIdList = videoCacheManager.getHotKeys();
        List<VideoDetail> result = new ArrayList<>(limit);
        for (Long id : videoIdList) {
            if (--limit < 0) break;
            if (!recommendCacheManager.exist(appKey, id)) {
                VideoDetail videoDetail = videoCacheManager.getVideo(id);
                result.add(videoDetail);
                recommendCacheManager.save(appKey, id);
            }
        }
        log.info("user with appKey {} get hot videos, now total: {}",
                appKey, recommendCacheManager.total(appKey));
        return result;
    }

    @Override
    public VideoDetail getVideoDetail(@Nonnull Long videoId, Long userId) {
        VideoDetail detail = videoCacheManager.getVideo(videoId);
        if (StringUtils.isEmpty(detail.getSrc()) || !detail.getSrc().contains("xvideos")) {
            driverManager.getVideoSrc(detail);
            if (StringUtils.isEmpty(detail.getSrc())) {//这里表示视频已经被网站删除，下面同理
                return detail;
            }
        }
        if (CommonUtil.videoSrcValid(detail.getSrc())) {
            driverManager.getVideoSrc(detail);
            if (StringUtils.isEmpty(detail.getSrc())) return detail;
        }
        if (userId != null) {
            if (videoRepository.findViewed(videoId, userId) <= 0)
                videoRepository.insertViewedVideo(videoId, userId);
        }
        audienceCacheManager.increase(videoId);
        return detail;
    }

    //只获取一级相关
    @Override
    public List<VideoDetail> relatedVideos(@Nonnull Long videoId, @Nonnull Integer offset, @Nonnull Integer limit) {
        List<Long> relatedIds = getRelatedVideoIds(videoId);
        List<VideoDetail> result = new ArrayList<>(limit);
        for (long id : relatedIds) {
            if (--offset >= 0) continue;
            videoRepository.findById(id).ifPresentOrElse(result::add, () -> videoRepository.deleteFromRelated(id));
            if (result.size() >= limit) break;
        }
        return result;
    }

    //TODO:搜索建议
    @Override
    public List<String> suggestSearch(@Nonnull String query) {
        return null;
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
        comment.setApprove(0);
        comment.setDisapprove(0);
        if (parentId != null) comment.setParentId(parentId);
        comment.setTime(System.currentTimeMillis());
        comment = commentRepository.save(comment);
        comment.setApproved(false);
        comment.setDisapproved(false);
        videoDetail.setCommentNum(videoDetail.getCommentNum() + 1);
        videoCacheManager.saveVideo(videoDetail);
        videoRepository.save(videoDetail);
        return comment;
    }

    @Override
    public Comment approveComment(@Nonnull Long userId, @Nonnull Comment comment) {
        boolean disapproved = commentRepository.findDisapproved(comment.getId(), userId) > 0;
        boolean approved = commentRepository.findApproved(comment.getId(), userId) > 0;
        if (approved) { //取消赞
            commentRepository.deleteApprove(comment.getId(), userId);
            comment.setApprove(comment.getApprove() - 1);
            comment = commentRepository.save(comment);
            comment.setApproved(false);
        } else { //赞
            if (disapproved) {
                comment.setDisapprove(comment.getDisapprove() - 1);
                commentRepository.deleteDisapprove(comment.getId(), userId);
            }
            commentRepository.insertApprove(comment.getId(), userId);
            comment.setApprove(comment.getApprove() + 1);
            comment = commentRepository.save(comment);
            comment.setApproved(true);
        }
        comment.setDisapproved(false);
        return comment;
    }

    @Override
    public Comment disapproveComment(@Nonnull Long userId, @Nonnull Comment comment) {
        boolean disapproved = commentRepository.findDisapproved(comment.getId(), userId) > 0;
        boolean approved = commentRepository.findApproved(comment.getId(), userId) > 0;
        if (disapproved) { //取消踩
            commentRepository.deleteDisapprove(comment.getId(), userId);
            comment.setDisapprove(comment.getDisapprove() - 1);
            comment = commentRepository.save(comment);
            comment.setDisapproved(false);
        } else { //踩
            if (approved) {
                comment.setApprove(comment.getApprove() - 1);
                commentRepository.deleteApprove(comment.getId(), userId);
            }
            commentRepository.insertDisapprove(comment.getId(), userId);
            comment.setDisapprove(comment.getDisapprove() + 1);
            comment = commentRepository.save(comment);
            comment.setDisapproved(true);
        }
        comment.setApproved(false);
        return comment;
    }

    @Override
    public List<Comment> getComments(@Nonnull Long userId, @Nonnull Long videoId, @Nonnull Integer page, @Nonnull Integer size, @Nonnull String sort) {
        VideoDetail videoDetail = videoRepository.getOne(videoId);
        Comment comment = new Comment();
        comment.setVideo(videoDetail);
        Page<Comment> comments;
        if (sort.equals("hot"))
            comments = commentRepository.findAll(Example.of(comment), PageRequest.of(page, size, Sort.by(new Sort.Order(Sort.Direction.DESC, "approve"))));
        else
            comments = commentRepository.findAll(Example.of(comment), PageRequest.of(page, size, Sort.by(new Sort.Order(Sort.Direction.DESC, "time"))));
        List<Comment> commentList = comments.getContent();
        for (Comment c : commentList) {
            c.setApproved(commentRepository.findApproved(c.getId(), userId) > 0);
            c.setDisapproved(commentRepository.findDisapproved(c.getId(), userId) > 0);
        }
        return commentList;
    }

    @Override
    public List<Comment> getChildComments(@Nonnull Integer page, @Nonnull Integer size, @Nonnull Long commentId) {
        Page<Comment> comments = commentRepository.findAll(PageRequest.of(page, size, Sort.by(new Sort.Order(Sort.Direction.ASC, "time"))));
        return comments.getContent();
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
    public List<Pair<VideoDetail, Integer>> onlineRank(@Nonnull Integer offset, @Nonnull Integer limit) {
        List<Pair<VideoDetail, Integer>> result = new ArrayList<>();
        List<Pair<Long, Integer>> rank = audienceCacheManager.rank(limit);
        for (Pair<Long, Integer> original : rank) {
            VideoDetail videoDetail = videoCacheManager.getVideo(original.getLeft());
            result.add(Pair.of(videoDetail, original.getRight()));
        }
        return result;
    }

    /**
     * 获取视频的一级相关视频id
     */
    private List<Long> getRelatedVideoIds(long videoId) {
        List<Long> relatedIds = videoRepository.findRelatedVideoIds(videoId);
        List<Long> reverseIds = videoRepository.findRelatedVideoIds_reverse(videoId);
        for (Long id : reverseIds) {
            if (!relatedIds.contains(id)) relatedIds.add(id);
        }
        relatedIds.remove(videoId);
        return relatedIds;
    }

}
