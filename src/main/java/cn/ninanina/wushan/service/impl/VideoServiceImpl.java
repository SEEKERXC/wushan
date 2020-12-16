package cn.ninanina.wushan.service.impl;

import cn.ninanina.wushan.common.util.CommonUtil;
import cn.ninanina.wushan.common.util.LuceneUtil;
import cn.ninanina.wushan.domain.*;
import cn.ninanina.wushan.repository.*;
import cn.ninanina.wushan.service.VideoService;
import cn.ninanina.wushan.service.cache.DownloadManager;
import cn.ninanina.wushan.service.cache.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

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
    private AudienceManager audienceManager;
    @Autowired
    private TagCacheManager tagCacheManager;
    @Autowired
    private DownloadManager downloadManager;
    @Autowired
    private SearchCacheManager searchCacheManager;
    @Autowired
    private UserDataCacheManager userDataCacheManager;
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private VideoRepositoryImpl videoRepositoryImpl;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private PlaylistRepository playlistRepository;
    @Autowired
    private ViewedRepository viewedRepository;
    @Autowired
    private LikeRepository likeRepository;
    @Autowired
    private DislikeRepository dislikeRepository;
    @Autowired
    private DownloadRepository downloadRepository;
    @Autowired
    private ToWatchRepository toWatchRepository;

    @Override
    public List<VideoDetail> recommendVideos(Long userId, @Nonnull String appKey, @Nonnull String type, @Nonnull Integer limit) {
        List<TagDetail> tags = null;
        switch (type) {
            case "hot": //最新更新链接的视频
                return jingxuanVideos(appKey, limit);
            case "recommend": //根据用户的观看、收藏、下载与不喜欢等行为进行推荐
                List<VideoDetail> recommendResult = new ArrayList<>();
                if (userId == null) return jingxuanVideos(appKey, limit);
                List<Long> viewedVideoIds = userDataCacheManager.getViewedIds(userId);
                List<Long> downloadedIds = userDataCacheManager.getDownloadedIds(userId);
                List<Long> likedIds = userDataCacheManager.getLikedIds(userId);
                List<Long> dislikedIds = userDataCacheManager.getDislikedIds(userId);
                List<Long> collectedVideoIds = userDataCacheManager.getCollectedIds(userId);
                if (CollectionUtils.isEmpty(viewedVideoIds)
                        && CollectionUtils.isEmpty(collectedVideoIds)
                        && CollectionUtils.isEmpty(downloadedIds)
                        && CollectionUtils.isEmpty(likedIds)) {
                    return jingxuanVideos(appKey, limit);
                }
                List<Long> ids = new ArrayList<>();
                ids.addAll(collectedVideoIds);
                ids.addAll(downloadedIds); //下载的和收藏的应具有相同比重
                Collections.shuffle(ids);
                Collections.shuffle(viewedVideoIds);
                Collections.shuffle(likedIds);
                ids.addAll(likedIds); //喜欢的视频比重稍微低一点
                ids.addAll(viewedVideoIds);
                for (long id : ids) {
                    List<Long> relatedIds = getRelatedVideoIds(id);
                    for (long relatedId : relatedIds) {
                        if (!recommendCacheManager.exist(appKey, relatedId)
                                && !collectedVideoIds.contains(relatedId)
                                && !downloadedIds.contains(relatedId)
                                && !viewedVideoIds.contains(relatedId)
                                && !dislikedIds.contains(relatedId)) {
                            VideoDetail video = videoCacheManager.getIfPresent(relatedId);
                            if (video == null)
                                videoRepository.findById(relatedId).ifPresent(recommendResult::add);
                            else recommendResult.add(video);
                            recommendCacheManager.save(appKey, relatedId);
                        }
                        if (recommendResult.size() >= limit) break;
                    }
                    if (recommendResult.size() >= limit) break;
                }
                if (recommendResult.size() < limit) {
                    int left = limit - recommendResult.size();
                    recommendResult.addAll(jingxuanVideos(appKey, left));
                }
                for (VideoDetail videoDetail : recommendResult)
                    videoCacheManager.loadTagsForVideo(videoDetail);
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
        for (VideoDetail videoDetail : result)
            videoCacheManager.loadTagsForVideo(videoDetail);
        return result;
    }

    /**
     * 获取若干个精选video，如果推荐完了，那么在数据库中随机取
     */
    private List<VideoDetail> jingxuanVideos(@Nonnull String appKey, @Nonnull Integer limit) {
        List<Long> videoIdList = videoCacheManager.getBestIds();
        List<VideoDetail> result = new ArrayList<>(limit);
        for (Long id : videoIdList) {
            if (!recommendCacheManager.exist(appKey, id)) {
                VideoDetail videoDetail = videoCacheManager.getVideo(id);
                if (videoDetail != null) result.add(videoDetail);
                recommendCacheManager.save(appKey, id);
            }
            if (result.size() >= limit) break;
        }
        Long maxId = videoRepository.findMaxId();
        Random random = new Random();
        while (result.size() < limit) {
            videoRepository.findById((long) random.nextInt(maxId.intValue())).ifPresent(result::add);
        }
        log.info("user with appKey {} get hot videos, now total: {}",
                appKey, recommendCacheManager.total(appKey));
        for (VideoDetail videoDetail : result)
            videoCacheManager.loadTagsForVideo(videoDetail);
        return result;
    }

    @Override
    public VideoDetail getVideoDetail(@Nonnull Long videoId, Long userId, Boolean withoutSrc, Boolean record) {
        VideoDetail detail = videoCacheManager.getVideo(videoId);
        if (!withoutSrc && !CommonUtil.videoSrcValid(detail.getSrc())) {
            downloadManager.getVideoSrc(detail);
            if (!CommonUtil.videoSrcValid(detail.getSrc()))
                return detail; //有两种情况会到这里，视频被删除或者cookie用完，尽量避免走到这，因为特别影响用户体验
        }
        if (record && userId != null) {
            VideoUserViewed viewed = viewedRepository.findByVideoIdAndUserId(videoId, userId);
            if (viewed == null) {
                viewed = new VideoUserViewed();
                viewed.setTime(System.currentTimeMillis());
                viewed.setUserId(userId);
                viewed.setVideoId(videoId);
                viewed.setViewCount(1);
                viewed.setWatchTime(0L);
            } else {
                viewed.setViewCount(viewed.getViewCount() + 1);
                viewed.setTime(System.currentTimeMillis());
            }
            viewedRepository.save(viewed);
        }
        if (record) detail.setAudience(detail.getAudience() + 1);
        if (!withoutSrc) detail.setUpdateTime(System.currentTimeMillis());
        videoCacheManager.saveVideo(detail); //这里会一起存入缓存和数据库
        if (record) audienceManager.increase(detail.getId());
        return detail;
    }

    //只获取一级相关
    @Override
    public List<VideoDetail> relatedVideos(@Nonnull Long videoId, @Nonnull Integer offset, @Nonnull Integer limit) {
        List<Long> relatedIds = getRelatedVideoIds(videoId);
        List<VideoDetail> result = new ArrayList<>(limit);
        for (long id : relatedIds) {
            if (--offset >= 0) continue;
            VideoDetail videoDetail = videoCacheManager.getVideo(id);
            if (videoDetail != null) result.add(videoDetail);
            else
                videoRepository.deleteFromRelated(id);
            if (result.size() >= limit) break;
        }
        for (VideoDetail videoDetail : result)
            videoCacheManager.loadTagsForVideo(videoDetail);
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
    public List<VideoDetail> search(@Nonnull String word, @Nonnull Integer offset, @Nonnull Integer limit, @Nonnull String sort) {
        List<VideoDetail> result = new ArrayList<>();
        Map<String, List<Long>> map = searchCacheManager.get(word);
        List<Long> ids = new ArrayList<>();
        if (CollectionUtils.isEmpty(map)) { //近期未搜索过
            map = new HashMap<>();
            IndexSearcher indexSearcher = LuceneUtil.get().getIndexSearcher();
            String[] fields = {"title", "titleZh", "tagZh"};
            QueryParser parser = new MultiFieldQueryParser(Version.LUCENE_4_10_4, fields, LuceneUtil.get().getAnalyzer());
            Query query = parser.parse(word);
            TopDocs topDocs = indexSearcher.search(query, 500);
            for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                Document document = indexSearcher.doc(scoreDoc.doc);
                long id = Long.parseLong(document.get("id"));
                ids.add(id);
            }
            if (!sort.equals("default"))
                ids = videoRepositoryImpl.findLimitedInIdsWithOrder(ids, sort, 0, ids.size());
            map.put(sort, ids);
            searchCacheManager.save(word, map);
        } else { //近期搜索过
            if (!map.containsKey(sort)) { //但是之前未按照sort排过序
                for (String key : map.keySet()) {
                    ids = map.get(key);
                    if (!CollectionUtils.isEmpty(ids)) break;
                }
                if (!sort.equals("default")) {
                    ids = videoRepositoryImpl.findLimitedInIdsWithOrder(ids, sort, 0, ids.size());
                    log.info("search sorted by {}", sort);
                }
                map.put(sort, ids);
            } else ids = map.get(sort); //近期对word按照sort搜索过
        }
        for (long id : ids) {
            if (--offset >= 0) continue;
            VideoDetail videoDetail = videoCacheManager.getVideo(id);
            if (videoDetail != null) result.add(videoDetail);
            else {
                //TODO：删除被删除视频的索引
            }
            if (result.size() >= limit) break;
        }
        for (VideoDetail videoDetail : result)
            videoCacheManager.loadTagsForVideo(videoDetail);
        return result;
    }

    @Override
    public Comment commentOn(@Nonnull Long userId, @Nonnull Long videoId, @Nonnull String content, @Nullable Long parentId) {
        User user = userRepository.getOne(userId);
        Comment comment = new Comment();
        comment.setContent(content);
        VideoDetail videoDetail = videoCacheManager.getVideo(videoId);
        if (videoDetail == null) return null;
        comment.setVideo(videoDetail);
        comment.setUser(user);
        comment.setApprove(0);
        comment.setDisapprove(0);
        Comment parent = commentRepository.findById(parentId).orElse(null);
        if (parent != null) {
            comment.setParentId(parentId);
            comment.setParent(parent);
        }
        comment.setTime(System.currentTimeMillis());
        comment = commentRepository.save(comment);
        comment.setApproved(false);
        comment.setDisapproved(false);
        videoDetail.setCommentNum(videoDetail.getCommentNum() + 1);
        videoCacheManager.saveVideo(videoDetail);
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
        VideoDetail videoDetail = videoCacheManager.getVideo(videoId);
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
            if (c.getParentId() != null && c.getParentId() > 0) {
                c.setParent(commentRepository.findById(c.getParentId()).orElse(null));
            }
        }
        return commentList;
    }

    @Override
    public List<Comment> getChildComments(@Nonnull Integer page, @Nonnull Integer size, @Nonnull Long commentId) {
        Page<Comment> comments = commentRepository.findAll(PageRequest.of(page, size, Sort.by(new Sort.Order(Sort.Direction.ASC, "time"))));
        return comments.getContent();
    }

    @Override
    public List<Pair<VideoUserViewed, VideoDetail>> viewedVideos(@Nonnull Long userId, Integer offset, Integer limit, Long startOfDay) {
        List<Pair<VideoUserViewed, VideoDetail>> result = new ArrayList<>();
        List<VideoUserViewed> vieweds;
        VideoUserViewed videoUserViewed = new VideoUserViewed();
        videoUserViewed.setUserId(userId);
        if (startOfDay != null && startOfDay != 0) {
            long endOfDay = startOfDay + 86400000; //一天的毫秒数 = 3600 * 24 * 1000 = 86,400,000
            vieweds = viewedRepository.findByPeriod(userId, startOfDay, endOfDay);
        } else {
            vieweds = viewedRepository.findByLimit(userId, offset, limit);
        }
        for (VideoUserViewed viewed : vieweds) {
            VideoDetail v = videoCacheManager.getIfPresent(viewed.getVideoId());
            if (v != null) result.add(Pair.of(viewed, v));
            else
                videoRepository.findById(viewed.getVideoId()).ifPresentOrElse(videoDetail ->
                {
                    videoCacheManager.loadTagsForVideo(videoDetail);
                    result.add(Pair.of(viewed, videoDetail));
                }, () ->
                        viewedRepository.delete(viewed));
        }

        return result;
    }

    @Override
    public List<VideoUserViewed> allViewed(@Nonnull Long userId) {
        return viewedRepository.findByUserId(userId);
    }

    @Override
    public void deleteViewed(@Nonnull List<Long> viewedIds) {
        viewedRepository.deleteAllByIds(viewedIds);
        log.info("deleted views: {}", viewedIds.toArray());
    }

    @Override
    public void download(@Nonnull Long userId, @Nonnull VideoDetail video) {
        VideoUserDownload download = downloadRepository.findByUserIdAndVideoId(userId, video.getId());
        if (download == null) {
            download = new VideoUserDownload();
            download.setUserId(userId);
            download.setVideoId(video.getId());
            video.setDownloaded(video.getDownloaded() + 1);
            downloadRepository.save(download);
            videoCacheManager.saveVideo(video);
        }
    }

    @Override
    public void exitDetail(@Nonnull Long videoId) {
        audienceManager.decrease(videoId);
    }

    @Override
    public int audiences(@Nonnull Long videoId) {
        return audienceManager.get(videoId);
    }

    @Override
    public List<VideoDetail> instantVideos(@Nonnull String appKey, Long userId, @Nonnull Integer limit) {
        List<VideoDetail> result = new ArrayList<>();
        Set<Long> validIds = videoCacheManager.getValidIds();
        List<Long> viewedVideoIds = new ArrayList<>();
        List<Long> collectedVideoIds = new ArrayList<>();
        List<Long> dislikedVideoIds = new ArrayList<>();
        List<Long> downloadedIds = new ArrayList<>();
        if (userId != null) {
            viewedVideoIds.addAll(viewedRepository.findViewedIds(userId));
            List<Long> playlistIds = playlistRepository.findAllPlaylistIds(userId);
            for (long playlistId : playlistIds) {
                collectedVideoIds.addAll(playlistRepository.findAllVideoIds(playlistId));
            }
            dislikedVideoIds.addAll(dislikeRepository.findVideoIdsByUserId(userId));
            downloadedIds.addAll(downloadRepository.findVideoIdsByUserId(userId));
        }
        for (Long id : validIds) {
            if (!recommendCacheManager.exist(appKey, id)
                    && !viewedVideoIds.contains(id)
                    && !collectedVideoIds.contains(id)
                    && !dislikedVideoIds.contains(id)
                    && !downloadedIds.contains(id)) {
                VideoDetail videoDetail = videoCacheManager.getVideo(id);
                if (videoDetail != null) {
                    if (CommonUtil.videoSrcValid(videoDetail.getSrc())) {
                        result.add(videoDetail);
                        recommendCacheManager.save(appKey, id);
                    } else {
                        videoCacheManager.deleteValidId(id);
                        log.info("remove invalid video {} from cache.", id);
                    }
                }
            }
            if (result.size() >= limit) break;
        }
        if (result.size() < limit) {
            result.addAll(jingxuanVideos(appKey, limit - result.size()));
        }
        return result;
    }

    @Override
    public VideoDetail likeVideo(@Nonnull Long userId, @Nonnull VideoDetail video) {
        VideoUserLike like = likeRepository.findByUserIdAndVideoId(userId, video.getId());
        if (like == null) {
            VideoUserDislike dislike = dislikeRepository.findByUserIdAndVideoId(userId, video.getId());
            if (dislike != null) {
                dislikeRepository.delete(dislike);
                video.setDisliked(video.getDisliked() - 1);
            }
            like = new VideoUserLike();
            like.setUserId(userId);
            like.setVideoId(video.getId());
            like.setTime(System.currentTimeMillis());
            video.setLiked(video.getLiked() + 1);
            video.setLike(true);
            likeRepository.save(like);
            videoRepository.save(video);
            log.info("user {} like video {}", userId, video.getId());
        } else {
            likeRepository.delete(like);
            video.setLiked(video.getLiked() - 1);
            video.setLike(false);
            videoRepository.save(video);
            log.info("user {} canceled liking video {}", userId, video.getId());
        }
        return video;
    }

    @Override
    public List<Long> likedVideos(@Nonnull Long userId) {
        return likeRepository.findByUserId(userId);
    }

    @Override
    public List<VideoDetail> likedVideos(long userId, int offset, int limit) {
        List<VideoDetail> result = new ArrayList<>();
        List<Long> ids = likeRepository.findByUserId(userId, offset, limit);
        for (long id : ids) {
            VideoDetail videoDetail = videoCacheManager.getVideo(id);
            if (videoDetail != null) result.add(videoDetail);
        }
        return result;
    }

    @Override
    public VideoDetail dislikeVideo(@Nonnull Long userId, @Nonnull VideoDetail video) {
        VideoUserDislike dislike = dislikeRepository.findByUserIdAndVideoId(userId, video.getId());
        if (dislike == null) {
            VideoUserLike like = likeRepository.findByUserIdAndVideoId(userId, video.getId());
            if (like != null) {
                likeRepository.delete(like);
                video.setLiked(video.getLiked() - 1);
            }
            dislike = new VideoUserDislike();
            dislike.setUserId(userId);
            dislike.setVideoId(video.getId());
            video.setDisliked(video.getDisliked() + 1);
            video.setDislike(true);
            dislikeRepository.save(dislike);
            videoRepository.save(video);
            log.info("user {} dislike video {}", userId, video.getId());
        } else {
            dislikeRepository.delete(dislike);
            video.setDisliked(video.getDisliked() - 1);
            video.setDislike(false);
            videoRepository.save(video);
            log.info("user {} canceled disliking video {}", userId, video.getId());
        }
        return video;
    }

    @Override
    public List<Long> dislikedVideos(@Nonnull Long userId) {
        List<Long> result = new ArrayList<>();
        List<VideoUserDislike> dislikes = dislikeRepository.findByUserId(userId);
        for (VideoUserDislike dislike : dislikes) result.add(dislike.getVideoId());
        return result;
    }

    @Override
    public ToWatch newToWatch(long userId, long videoId) {
        ToWatch toWatch = toWatchRepository.findByUserIdAndVideoId(userId, videoId);
        if (toWatch == null) {
            toWatch = new ToWatch();
            toWatch.setUserId(userId);
            toWatch.setVideoId(videoId);
        }
        toWatch.setAddTime(System.currentTimeMillis());
        toWatch = toWatchRepository.save(toWatch);
        return toWatch;
    }

    @Override
    public void deleteToWatch(List<Long> toWatchIds) {
        toWatchRepository.deleteAllByIds(toWatchIds);
    }

    @Override
    public List<Pair<ToWatch, VideoDetail>> listToWatches(long userId, int offset, int limit) {
        List<Pair<ToWatch, VideoDetail>> result = new ArrayList<>();
        List<ToWatch> toWatches = toWatchRepository.findByUserId(userId, offset, limit);
        for (ToWatch toWatch : toWatches) {
            VideoDetail videoDetail = videoCacheManager.getVideo(toWatch.getVideoId());
            if (videoDetail != null) result.add(Pair.of(toWatch, videoDetail));
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
