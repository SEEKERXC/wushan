package cn.ninanina.wushan.web;

import cn.ninanina.wushan.domain.Comment;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.domain.Playlist;
import cn.ninanina.wushan.repository.TagRepository;
import cn.ninanina.wushan.repository.VideoRepository;
import cn.ninanina.wushan.service.CommonService;
import cn.ninanina.wushan.service.PlaylistService;
import cn.ninanina.wushan.service.TagService;
import cn.ninanina.wushan.service.VideoService;
import cn.ninanina.wushan.service.cache.VideoCacheManager;
import cn.ninanina.wushan.web.result.Response;
import cn.ninanina.wushan.web.result.ResultMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    private CommonService commonService;
    @Autowired
    private PlaylistService playlistService;

    @GetMapping("/recommend")
    public Response recommendVideos(@RequestParam("appKey") String appKey,
                                    @RequestParam("type") String type,
                                    @RequestParam("limit") Integer limit,
                                    String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (limit <= 0 || limit >= 50) return result(ResultMsg.ParamError);
        Long userId = getUserId(token);
        log.info("get recommendVideos, appKey: {}, type: {} ,limit: {}", appKey, type, limit);
        return result(videoService.recommendVideos(userId, appKey, type, limit));
    }

    /**
     * 获取最新的有效的视频详情。调用场景：
     * <p>用户播放视频时
     *
     * @param id 视频id
     */
    @GetMapping("/detail")
    public Response videoDetail(@RequestParam("appKey") String appKey,
                                @RequestParam("id") Long id,
                                String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (videoRepository.findById(id).isEmpty()) return result(ResultMsg.INVALID_VIDEO_ID);
        Long userId = getUserId(token);
        VideoDetail videoDetail;
        log.info("appKey {} get video detail: {}", appKey, id);
        videoDetail = videoService.getVideoDetail(id, userId);
        return result(videoDetail);
    }

    /**
     * 用户退出视频播放页调用
     *
     * @param id 视频id
     */
    @PostMapping("/exit")
    public Response exitVideoDetail(@RequestParam("id") Long id) {
        if (videoRepository.findById(id).isEmpty()) {
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
    public Response currentAudience(@RequestParam("id") Long id) {
        if (!videoRepository.findById(id).isPresent()) {
            return result(ResultMsg.INVALID_VIDEO_ID);
        }
        int audienceCount = videoService.audiences(id);
        log.info("video {} now audience count: {}", id, audienceCount);
        return result(audienceCount);
    }

    /**
     * 获取当前在线视频排行，即按当前观看人数排序的videoList
     *
     * @return
     */
    @GetMapping("/rank/online")
    public Response onlineRank(@RequestParam("limit") Integer limit) {
        List<Pair<VideoDetail, Integer>> rank = videoService.onlineRank(limit);
        log.info("ip {} get online rank, rank size: {}", getIp(), rank.size());
        return result(rank);
    }

    /**
     * 获取视频数量最多的若干个tag
     *
     * @param limit tag数量，最好小于1000
     */
    @GetMapping("/tag/hot")
    public Response hotTags(@RequestParam("limit") Integer limit) {
        if (limit <= 0 || limit > tagService.tagCount()) return result(ResultMsg.ParamError, "limit参数错误");
        log.info("get hot tags, limit:{}", limit);
        return result(tagService.hotTags(limit));
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
        if (videoRepository.findById(id).isEmpty()) {
            return result(ResultMsg.INVALID_VIDEO_ID);
        }
        if (offset < 0 || limit <= 0 || limit >= 50) return result(ResultMsg.ParamError);
        List<VideoDetail> result = videoService.relatedVideos(id, offset, limit);
        log.info("get related videos, videoId: {}, offset: {}, limit: {}", id, offset, limit);
        return result(result);
    }

    /**
     * 根据tag获取随机热门视频
     *
     * @param appKey 每个app独有的key
     */
    @GetMapping("/tag")
    public Response tagVideos(@RequestParam("appKey") String appKey) {
        return result();
    }

    /**
     * 搜索视频
     *
     * @param query  关键词
     * @param offset offset
     * @param limit  limit
     * @return 视频列表
     */
    @GetMapping("/search")
    public Response search(@RequestParam("appKey") String appKey,
                           @RequestParam("query") String query,
                           @RequestParam("offset") Integer offset,
                           @RequestParam("limit") Integer limit) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        List<VideoDetail> result = videoService.search(query, offset, limit);
        log.info("ip {} search with query {}, offset: {}, limit: {}", getIp(), query, offset, limit);
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
        if (videoRepository.findById(id).isEmpty()) return result(ResultMsg.INVALID_VIDEO_ID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        if (StringUtils.isEmpty(content.trim())) return result(ResultMsg.EMPTY_CONTENT);
        Comment comment = videoService.commentOn(userId, id, content, parentId);
        if (comment == null) return result(ResultMsg.INVALID_VIDEO_ID);
        log.info("user {} commented on video {}, content: {}, commentId: {}", userId, id, content, comment.getId());
        return result(comment);
    }

    /**
     * 收藏视频
     *
     * @param videoId 视频id
     * @param dirId   收藏夹id
     * @return 收藏的结果
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

    @GetMapping("/playlist/videos")
    public Response getPlayListVideos(@RequestParam("appKey") String appKey,
                                      @RequestParam("id") Long id) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        log.info("get videos of playlist {}", id);
        return result(playlistService.listVideos(id));
    }

    /**
     * 创建收藏夹
     *
     * @param appKey appKey
     * @param name   收藏夹名字
     * @return 收藏夹
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

    @PostMapping("/playlist/rename")
    public Response renameCollectDir(@RequestParam("appKey") String appKey,
                                     @RequestParam("dirId") Long dirId,
                                     @RequestParam("name") String name,
                                     @RequestParam("token") String token) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        Long userId = getUserId(token);
        if (userId == null) return result(ResultMsg.NOT_LOGIN);
        if (playlistService.possess(userId, dirId) == null) return result(ResultMsg.COLLECT_WRONG_DIR);
        Playlist dir = playlistService.rename(dirId, name);
        return result(dir);
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
        if (videoRepository.findById(id).isEmpty()) return result(ResultMsg.INVALID_VIDEO_ID);
        videoService.download(userId, id);
        return result();
    }

    @PostMapping("/updateTag")
    public Response updateTag(@RequestParam("id") Long id,
                              @RequestParam("name") String name) {
        tagRepository.findById(id).ifPresent(tagDetail -> {
            tagDetail.setTagZh(name);
            tagRepository.save(tagDetail);
        });
        return result();
    }
}
