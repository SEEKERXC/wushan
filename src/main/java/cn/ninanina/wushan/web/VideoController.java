package cn.ninanina.wushan.web;

import cn.ninanina.wushan.domain.*;
import cn.ninanina.wushan.repository.*;
import cn.ninanina.wushan.service.CommonService;
import cn.ninanina.wushan.service.PlaylistService;
import cn.ninanina.wushan.service.TagService;
import cn.ninanina.wushan.service.VideoService;
import cn.ninanina.wushan.service.cache.VideoCacheManager;
import cn.ninanina.wushan.web.result.Response;
import cn.ninanina.wushan.web.result.ResultMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/video")
public class VideoController extends BaseController {
    @Autowired
    private VideoService videoService;
    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private VideoCacheManager videoCacheManager;
    @Autowired
    private TagService tagService;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlaylistRepository playlistRepository;
    @Autowired
    private CommonService commonService;
    @Autowired
    private PlaylistService playlistService;
    @Autowired
    private SearchRepository searchRepository;

    /**
     * 首页推荐视频
     *
     * @param appKey appKey
     * @param type   包含hot、recommend、asian、west、lesbian
     * @param limit  限制数量
     * @param token  登录依据，可为空
     */
    @GetMapping("/recommend")
    public Response recommendVideos(@RequestParam("appKey") String appKey,
                                    @RequestParam("type") String type,
                                    @RequestParam("limit") Integer limit,
                                    String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (limit <= 0 || limit > 50) return result(ResultMsg.ParamError);
        Long userId = getUserId(token);
        log.info("get recommendVideos, appKey: {}, type: {} ,limit: {}", appKey, type, limit);
        return result(videoService.recommendVideos(userId, appKey, type, limit));
    }

    /**
     * 获取当前可以直接看的video
     */
    @GetMapping("/instant")
    public Response instantVideos(@RequestParam("appKey") String appKey,
                                  @RequestParam("limit") Integer limit,
                                  String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (limit <= 0 || limit > 50) return result(ResultMsg.ParamError);
        Long userId = getUserId(token);
        log.info("get instant videos, appKey: {}, limit: {}", appKey, limit);
        return result(videoService.instantVideos(appKey, userId, limit));
    }

    /**
     * 获取最新的有效的视频详情。调用场景：
     * <p>用户播放视频时
     *
     * @param id         视频id
     * @param withoutSrc 指定不需要实时链接
     * @param record     指定是否记录观看
     */
    @GetMapping("/detail")
    public Response videoDetail(@RequestParam("appKey") String appKey,
                                @RequestParam("id") Long id,
                                String token,
                                Boolean withoutSrc,
                                Boolean record) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (videoCacheManager.getVideo(id) == null) return result(ResultMsg.INVALID_VIDEO_ID);
        Long userId = getUserId(token);
        VideoDetail videoDetail;
        log.info("appKey {} get video detail: {}", appKey, id);
        videoDetail = videoService.getVideoDetail(id, userId, withoutSrc, record);
        return result(videoDetail);
    }

    /**
     * 增量记录观看视频时长，客户端在播放视频时，每隔几秒都需要记录一次播放总时长，以秒为单位
     */
    @PostMapping("/record")
    public Response recordWatchTime(@RequestParam("appKey") String appKey,
                                    @RequestParam("token") String token,
                                    @RequestParam("videoId") Long videoId,
                                    @RequestParam("time") Integer time) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (videoCacheManager.getVideo(videoId) == null) return result(ResultMsg.INVALID_VIDEO_ID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        videoService.recordWatch(userId, videoId, time);
        return result();
    }

    /**
     * 用户退出视频播放页调用
     *
     * @param id 视频id
     */
    @PostMapping("/exit")
    public Response exitVideoDetail(@RequestParam("appKey") String appKey,
                                    @RequestParam("id") Long id) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (videoCacheManager.getVideo(id) == null) {
            return result(ResultMsg.INVALID_VIDEO_ID);
        }
        videoService.exitDetail(id);
        return result();
    }

    /**
     * 获取当前视频正在观看的人数
     *
     * @param id 视频id
     */
    @GetMapping("/audience")
    public Response currentAudience(@RequestParam("appKey") String appKey,
                                    @RequestParam("id") Long id) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (videoCacheManager.getVideo(id) == null) {
            return result(ResultMsg.INVALID_VIDEO_ID);
        }
        int audienceCount = videoService.audiences(id);
        log.info("video {} now audience count: {}", id, audienceCount);
        return result(audienceCount);
    }

    /**
     * 获取视频id相关的视频
     *
     * @param id 视频id
     * @return 相关视频列表
     */
    @GetMapping("/related")
    public Response relatedVideos(@RequestParam("appKey") String appKey,
                                  @RequestParam("id") Long id,
                                  @RequestParam("offset") Integer offset,
                                  @RequestParam("limit") Integer limit) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (videoCacheManager.getVideo(id) == null) {
            return result(ResultMsg.INVALID_VIDEO_ID);
        }
        if (offset < 0 || limit <= 0 || limit > 50) return result(ResultMsg.ParamError);
        List<VideoDetail> result = videoService.relatedVideos(id, offset, limit);
        log.info("get related videos, videoId: {}, offset: {}, limit: {}", id, offset, limit);
        return result(result);
    }

    /**
     * 搜索视频
     *
     * @param query  关键词
     * @param offset offset
     * @param limit  limit
     * @param sort   排序依据，random：默认排序，viewed：播放量降序，
     *               collected：收藏量降序，downloaded：下载量降序，commentNum：评论数降序
     * @return 视频列表
     */
    @GetMapping("/search")
    public Response search(@RequestParam("appKey") String appKey,
                           @RequestParam("query") String query,
                           @RequestParam("offset") Integer offset,
                           @RequestParam("limit") Integer limit,
                           @RequestParam("sort") String sort) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (StringUtils.isEmpty(query) || offset < 0 || limit <= 0 || limit > 50)
            return result(ResultMsg.ParamError);
        List<VideoDetail> result = videoService.search(query, offset, limit, sort);
        SearchInfo searchInfo = new SearchInfo();
        searchInfo.setAppKey(appKey);
        searchInfo.setIp(getIp());
        searchInfo.setSort(sort);
        searchInfo.setWord(query);
        searchInfo.setTime(System.currentTimeMillis());
        searchRepository.save(searchInfo);
        log.info("ip {} search with query {}, offset: {}, limit: {}, result size: {}", getIp(), query, offset, limit, result.size());
        return result(result);
    }

    /**
     * 发表视频评论
     *
     * @param id       视频ID
     * @param content  评论内容
     * @param token    表示已登录的token
     * @param parentId 评论父ID，可以为空
     * @return 评论结果
     */
    @PostMapping("/comment")
    public Response comment(@RequestParam("appKey") String appKey,
                            @RequestParam("id") Long id,
                            @RequestParam("content") String content,
                            @RequestParam("token") String token,
                            Long parentId) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (videoCacheManager.getVideo(id) == null) return result(ResultMsg.INVALID_VIDEO_ID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        if (StringUtils.isEmpty(content.trim())) return result(ResultMsg.EMPTY_CONTENT);
        Comment comment = videoService.commentOn(userId, id, content, parentId);
        if (comment == null) return result(ResultMsg.INVALID_VIDEO_ID);
        log.info("user {} commented on video {}, content: {}, commentId: {}, parentId: {}", userId, id, content, comment.getId(), parentId);
        return result(comment);
    }

    /**
     * 删除评论
     */
    @PostMapping("/comment/delete")
    public Response deleteComment(@RequestParam("appKey") String appKey,
                                  @RequestParam("token") String token,
                                  @RequestParam("commentId") Long commentId) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) return result(ResultMsg.INVALID_COMMENT_ID);
        if (!comment.getUser().getId().equals(userId)) return result(ResultMsg.FAILED);
        commentRepository.delete(comment);
        return result();
    }

    /**
     * 点赞/取消点赞评论
     */
    @PostMapping("/comment/approve")
    public Response approveComment(@RequestParam("appKey") String appKey,
                                   @RequestParam("commentId") Long commentId,
                                   @RequestParam("token") String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) return result(ResultMsg.INVALID_COMMENT_ID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        comment = videoService.approveComment(userId, comment);
        if (comment.getApproved()) log.info("user {} approved comment {}.", userId, commentId);
        else log.info("user {} canceled approving comment {}", userId, commentId);
        return result(comment);
    }

    /**
     * 踩/取消踩评论
     */
    @PostMapping("/comment/disapprove")
    public Response disapproveComment(@RequestParam("appKey") String appKey,
                                      @RequestParam("commentId") Long commentId,
                                      @RequestParam("token") String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) return result(ResultMsg.INVALID_COMMENT_ID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        comment = videoService.disapproveComment(userId, comment);
        if (comment.getDisapproved()) log.info("user {} disapproved comment {}.", userId, commentId);
        else log.info("user {} canceled disapproving comment {}", userId, commentId);
        return result(comment);
    }

    /**
     * 分页获取视频的评论
     *
     * @param appKey  appkey
     * @param videoId 视频id
     * @param page    页数，从0开始
     * @param size    一页总数，[1,50]
     * @param sort    排序依据，热度=hot, 时间=time
     * @return 评论列表
     */
    @GetMapping("/comments")
    public Response getComments(@RequestParam("appKey") String appKey,
                                @RequestParam("token") String token,
                                @RequestParam("videoId") Long videoId,
                                @RequestParam("page") Integer page,
                                @RequestParam("size") Integer size,
                                @RequestParam("sort") String sort) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        VideoDetail videoDetail = videoCacheManager.getVideo(videoId);
        if (videoDetail == null) return result(ResultMsg.INVALID_VIDEO_ID);
        if (page < 0 || size <= 0 || size > 50) return result(ResultMsg.ParamError);
        List<Comment> result = videoService.getComments(userId, videoId, page, size, sort);
        log.info("get comments of video {}, page: {}, size: {}, sort: {} ip: {}, result size: {}", videoId, page, size, sort, getIp(), result.size());
        return result(result);
    }

    /**
     * 分页获取子评论，按照时间升序
     *
     * @param appKey    appkey
     * @param page      页数，从0开始
     * @param size      一页总数，[0,50]
     * @param commentId 父评论id
     */
    @GetMapping("/childComments")
    public Response getChildComments(@RequestParam("appKey") String appKey,
                                     @RequestParam("page") Integer page,
                                     @RequestParam("size") Integer size,
                                     @RequestParam("commentId") Long commentId) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) return result(ResultMsg.INVALID_COMMENT_ID);
        if (page < 0 || size <= 0 || size > 50) return result(ResultMsg.ParamError);
        List<Comment> result = videoService.getChildComments(page, size, commentId);
        log.info("get child comments of comment {}, ip: {}", commentId, getIp());
        return result(result);
    }

    /**
     * 收藏视频
     */
    @PostMapping("/collect")
    public Response collectVideo(@RequestParam("appKey") String appKey,
                                 @RequestParam("videoId") Long videoId,
                                 @RequestParam("dirId") Long dirId,
                                 @RequestParam("token") String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        VideoDetail videoDetail = videoCacheManager.getVideo(videoId);
        if (videoDetail == null) return result(ResultMsg.INVALID_VIDEO_ID);
        Playlist playlist = playlistService.possess(userId, dirId);
        if (playlist == null) return result(ResultMsg.COLLECT_WRONG_DIR);
        if (playlistService.collect(playlist, videoDetail)) {
            return result();
        } else {
            return result(ResultMsg.COLLECT_ALREADY);
        }
    }

    /**
     * 取消收藏视频
     */
    @PostMapping("/collect/cancel")
    public Response cancelCollect(@RequestParam("appKey") String appKey,
                                  @RequestParam("videoId") Long videoId,
                                  @RequestParam("dirId") Long dirId,
                                  @RequestParam("token") String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        Playlist playlist = playlistService.possess(userId, dirId);
        VideoDetail videoDetail = videoRepository.findById(videoId).orElse(null);
        if (videoDetail == null) return result(ResultMsg.INVALID_VIDEO_ID);
        if (playlist == null) return result(ResultMsg.COLLECT_WRONG_DIR);
        if (playlistService.cancelCollect(playlist, videoDetail)) {
            return result();
        } else return result(ResultMsg.COLLECT_WRONG_DIR);
    }

    /**
     * 获取收藏夹列表
     *
     * @return 收藏的视频列表
     */
    @GetMapping("/playlist")
    public Response getPlaylists(@RequestParam("appKey") String appKey,
                                 @RequestParam("token") String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        List<Playlist> result = playlistService.listAll(userId);
        log.info("user get collected dirs, userid: {}, dir list size: {}", userId, result.size());
        return result(result);
    }

    /**
     * 获取收藏夹的视频列表
     */
    @GetMapping("/playlist/videos")
    public Response getPlayListVideos(@RequestParam("appKey") String appKey,
                                      @RequestParam("id") Long id) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        log.info("get videos of playlist {}", id);
        return result(playlistService.listVideos(id));
    }

    /**
     * 创建收藏夹
     */
    @PostMapping("/playlist/create")
    public Response createCollectDir(@RequestParam("appKey") String appKey,
                                     @RequestParam("name") String name,
                                     @RequestParam("token") String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        Playlist playlist = playlistService.create(userId, name);
        return result(playlist);
    }

    /**
     * 删除收藏夹
     */
    @PostMapping("/playlist/delete")
    public Response deleteCollectDir(@RequestParam("appKey") String appKey,
                                     @RequestParam("dirId") Long dirId,
                                     @RequestParam("token") String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        if (playlistService.possess(userId, dirId) == null) return result(ResultMsg.COLLECT_WRONG_DIR);
        playlistService.delete(dirId);
        return result();
    }

    /**
     * 更新收藏夹
     */
    @PostMapping("/playlist/update")
    public Response updatePlaylist(@RequestParam("appKey") String appKey,
                                   @RequestParam("token") String token,
                                   @RequestParam("dirId") Long dirId,
                                   @RequestParam(value = "coverUrl", required = false) String coverUrl,
                                   @RequestParam("name") String name,
                                   @RequestParam("isPublic") Boolean isPublic) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        Playlist playlist = playlistService.possess(userId, dirId);
        if (playlist == null) return result(ResultMsg.COLLECT_WRONG_DIR);
        if (!StringUtils.isEmpty(coverUrl) && !playlist.getCover().equals(coverUrl)) {
            playlist.setUserSetCover(true);
            playlist.setCover(coverUrl);
        }
        playlist.setName(name);
        playlist.setIsPublic(isPublic);
        playlist.setUpdateTime(System.currentTimeMillis());
        playlist = playlistRepository.save(playlist);
        log.info("user {} updated playlist {}", userId, dirId);
        return result(playlist);
    }

    /**
     * 下载视频
     *
     * @param id 视频id
     */
    @PostMapping("/download")
    public Response downloadVideo(@RequestParam("appKey") String appKey,
                                  @RequestParam("id") Long id,
                                  @RequestParam("token") String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        VideoDetail videoDetail = videoCacheManager.getVideo(id);
        if (videoDetail == null) return result(ResultMsg.INVALID_VIDEO_ID);
        videoService.download(userId, videoDetail);
        return result();
    }

    /**
     * 修改标签中文，用于调试，需要严格保密
     *
     * @param id   标签id
     * @param name 标签中文
     */
    @PostMapping("/updateTag")
    public Response updateTag(@RequestParam("id") Long id,
                              @RequestParam("name") String name) {
        tagRepository.findById(id).ifPresent(tagDetail -> {
            tagDetail.setTagZh(name);
            tagRepository.save(tagDetail);
        });
        return result();
    }

    /**
     * 获取以c为首字符的标签列表，按照视频数量排序，热门标签标记为~，其他字符标记为#
     */
    @GetMapping("/tags")
    public Response getTags(@RequestParam("appKey") String appKey,
                            @RequestParam("c") Character c,
                            @RequestParam("page") Integer page,
                            @RequestParam("size") Integer size) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        List<TagDetail> tagDetails = tagService.getTagsStartWith(c, page, size);
        log.info("ip {} get tags starting with {}, page {} ,size {}", getIp(), c, page, size);
        return result(tagDetails);
    }

    /**
     * 标签搜索建议
     */
    @GetMapping("/tag/suggest")
    public Response suggestTag(@RequestParam("appKey") String appKey,
                               @RequestParam("query") String query) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (StringUtils.isEmpty(query))
            return result(ResultMsg.ParamError);
        List<TagDetail> result = tagService.suggestTag(query);
        log.info("ip {} suggest for tag with query {}", getIp(), query);
        return result(result);
    }

    /**
     * 搜索标签
     */
    @GetMapping("/tag/search")
    public Response searchTag(@RequestParam("appKey") String appKey,
                              @RequestParam("query") String query,
                              @RequestParam("offset") Integer offset,
                              @RequestParam("limit") Integer limit) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (StringUtils.isEmpty(query) || offset < 0 || limit <= 0 || limit > 50)
            return result(ResultMsg.ParamError);
        List<TagDetail> result = tagService.searchForTag(query, offset, limit);
        log.info("ip {} search for tag with query {}, offset: {}, limit: {}", getIp(), query, offset, limit);
        return result(result);
    }

    /**
     * 获取标签下的视频
     *
     * @param sort 排序依据，random=随机；hot=播放数倒序；comment=评论数倒序；duration=时长倒序。默认随机
     */
    @GetMapping("/tag/videos")
    public Response getTagVideos(@RequestParam("appKey") String appKey,
                                 @RequestParam("tagId") Long tagId,
                                 @RequestParam("offset") Integer offset,
                                 @RequestParam("limit") Integer limit,
                                 @RequestParam("sort") String sort) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (offset < 0 || limit <= 0 || limit > 50)
            return result(ResultMsg.ParamError);
        TagDetail tagDetail = tagRepository.findById(tagId).orElse(null);
        if (tagDetail == null) return result(ResultMsg.INVALID_TAG_ID);
        List<VideoDetail> result = tagService.getVideosForTag(tagDetail, offset, limit, sort);
        log.info("ip {} get videos from tag {}", getIp(), tagId);
        return result(result);
    }

    /**
     * 获取搜索最多的若干个tag
     *
     * @param limit tag数量，最好不超过500
     */
    @GetMapping("/tag/hot")
    public Response hotTags(@RequestParam("appKey") String appKey,
                            @RequestParam("limit") Integer limit) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (limit <= 0 || limit > tagService.tagCount())
            return result(ResultMsg.ParamError);
        log.info("get hot tags, limit:{}", limit);
        return result(tagService.hotTags(limit));
    }

    /**
     * 返回用户所有的观看记录，不包含视频详情
     */
    @GetMapping("/viewed/all")
    public Response allViewed(@RequestParam("appKey") String appKey,
                              @RequestParam("token") String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        List<VideoUserViewed> result = videoService.allViewed(userId);
        log.info("user {} get all viewed, size {}", userId, result.size());
        return result(result);
    }

    /**
     * 返回用户的部分观看记录，包含观看信息与视频详情
     */
    @GetMapping("/viewed")
    public Response viewedVideos(@RequestParam("appKey") String appKey,
                                 @RequestParam("token") String token,
                                 @RequestParam(value = "offset", required = false) Integer offset,
                                 @RequestParam(value = "limit", required = false) Integer limit,
                                 @RequestParam(value = "startOfDay", required = false) Long startOfDay) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (offset != null && limit != null && (offset < 0 || limit < 0 || limit > 50))
            return result(ResultMsg.ParamError);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        User user = userRepository.getOne(userId);
        if (startOfDay != null && (startOfDay > System.currentTimeMillis() || startOfDay < user.getRegisterTime()))
            return result(ResultMsg.ParamError);
        List<Pair<VideoUserViewed, VideoDetail>> result = videoService.viewedVideos(userId, offset, limit, startOfDay);
        log.info("user {} get viewed videos. offset {}, limit {}, result size {}", userId, offset, limit, result.size());
        return result(result);
    }

    /**
     * 删除观看记录
     *
     * @param ids 观看记录id
     */
    @PostMapping("/viewed/delete")
    public Response deleteViewed(@RequestParam("appKey") String appKey,
                                 @RequestParam("token") String token,
                                 @RequestParam("ids") List<Long> ids) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        videoService.deleteViewed(ids);
        return result();
    }

    /**
     * 喜欢/取消喜欢视频
     */
    @PostMapping("/like")
    public Response likeVideo(@RequestParam("appKey") String appKey,
                              @RequestParam("token") String token,
                              @RequestParam("videoId") Long videoId) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        VideoDetail videoDetail = videoCacheManager.getVideo(videoId);
        if (videoDetail == null) return result(ResultMsg.INVALID_VIDEO_ID);
        return result(videoService.likeVideo(userId, videoDetail));
    }

    /**
     * 分页获取喜欢的视频
     */
    @GetMapping("/like")
    public Response likeVideos(@RequestParam("appKey") String appKey,
                               @RequestParam("token") String token,
                               @RequestParam("offset") Integer offset,
                               @RequestParam("limit") Integer limit) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        List<VideoDetail> videoDetails = videoService.likedVideos(userId, offset, limit);
        log.info("user {} get liked videos, result size {}", userId, videoDetails.size());
        return result(videoDetails);
    }

    /**
     * 获取用户所有喜欢的视频id
     */
    @GetMapping("/liked")
    public Response likedVideo(@RequestParam("appKey") String appKey,
                               @RequestParam("token") String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        return result(videoService.likedVideos(userId));
    }

    /**
     * 不喜欢/取消不喜欢视频
     */
    @PostMapping("/dislike")
    public Response dislikeVideo(@RequestParam("appKey") String appKey,
                                 @RequestParam("token") String token,
                                 @RequestParam("videoId") Long videoId) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        VideoDetail videoDetail = videoRepository.findById(videoId).orElse(null);
        if (videoDetail == null) return result(ResultMsg.INVALID_VIDEO_ID);
        return result(videoService.dislikeVideo(userId, videoDetail));
    }

    /**
     * 获取用户所有不喜欢的视频id
     */
    @GetMapping("/disliked")
    public Response dislikedVideo(@RequestParam("appKey") String appKey,
                                  @RequestParam("token") String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        return result(videoService.dislikedVideos(userId));
    }

    /**
     * 新增稍后观看
     */
    @PostMapping("/toWatch")
    public Response addToWatch(@RequestParam("appKey") String appKey,
                               @RequestParam("token") String token,
                               @RequestParam("videoId") Long videoId) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        ToWatch toWatch = videoService.newToWatch(userId, videoId);
        log.info("use {} add a new toWatch, id: {}, video {}", userId, toWatch.getId(), videoId);
        return result(toWatch);
    }

    /**
     * 删除稍后看
     */
    @PostMapping("/toWatch/delete")
    public Response deleteToWatch(@RequestParam("appKey") String appKey,
                                  @RequestParam("token") String token,
                                  @RequestParam("ids") List<Long> ids) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        videoService.deleteToWatch(ids);
        log.info("user {} deleted toWatches {}", userId, ids.toArray());
        return result();
    }

    /**
     * 获取稍后看
     */
    @GetMapping("/toWatch")
    public Response getToWatches(@RequestParam("appKey") String appKey,
                                 @RequestParam("token") String token,
                                 @RequestParam("offset") Integer offset,
                                 @RequestParam("limit") Integer limit) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        List<Pair<ToWatch, VideoDetail>> toWatches = videoService.listToWatches(userId, offset, limit);
        log.info("user {} get toWatches, result size {}", userId, toWatches.size());
        return result(toWatches);
    }
}
