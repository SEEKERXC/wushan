package cn.ninanina.wushan.web;

import cn.ninanina.wushan.common.util.EncodeUtil;
import cn.ninanina.wushan.domain.Comment;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.domain.VideoDir;
import cn.ninanina.wushan.repository.TagRepository;
import cn.ninanina.wushan.repository.VideoRepository;
import cn.ninanina.wushan.service.CommonService;
import cn.ninanina.wushan.service.TagService;
import cn.ninanina.wushan.service.VideoService;
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
    private TagService tagService;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private CommonService commonService;

    @GetMapping("/recommend")
    public Response recommendVideos(@RequestParam("appKey") String appKey,
                                    @RequestParam("limit") Integer limit) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        User user = getUser();
        if (user == null) {
            List<VideoDetail> result = videoService.randomHotVideos(appKey, limit);
            return result(result);
        }
        log.info("user: {} get recommendVideos, appKey: {}", user.getId(), appKey);
        return result(videoService.recommendVideos(user, appKey, limit));
    }

    /**
     * 获取最新的有效的视频详情。调用场景：
     * <p>用户播放视频时
     *
     * @param id 视频id
     */
    @GetMapping("/detail")
    public Response videoDetail(@RequestParam("appKey") String appKey,
                                @RequestParam("id") Long id) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (videoRepository.findById(id).isEmpty()) {
            return result(ResultMsg.INVALID_VIDEO_ID);
        }
        User user = getUser();
        VideoDetail videoDetail;
        log.info("appKey {} get video detail: {}", appKey, id);
        videoDetail = videoService.getVideoDetail(id, user);
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
                                  @RequestParam("id") Long id) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        if (videoRepository.findById(id).isEmpty()) {
            return result(ResultMsg.INVALID_VIDEO_ID);
        }
        List<VideoDetail> result = videoService.relatedVideos(id);
        log.info("get related videos, appKey: {}, result size: {}", appKey, result.size());
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
        User user = getUser();
        List<VideoDetail> result = videoService.search(query, offset, limit);
        if (user != null) {
            log.info("user {} search with query {}, offset: {}, limit: {}", user.getId(), query, offset, limit);
        } else {
            log.info("ip {} search with query {}, offset: {}, limit: {}", getIp(), query, offset, limit);
        }
        return result(result);
    }

    /**
     * 发表视频评论
     *
     * @param id       视频ID
     * @param content  评论内容
     * @param parentId 评论父ID，可以为空
     * @return 评论结果
     */
    @PostMapping("/comment")
    public Response comment(@RequestParam("id") Long id,
                            @RequestParam("content") String content,
                            Long parentId) {
        User user = getUser();
        if (user == null) return result(ResultMsg.NOT_LOGIN);
        if (StringUtils.isEmpty(content.trim())) return result(ResultMsg.EMPTY_CONTENT);
        Comment comment = videoService.commentOn(user, id, content, parentId);
        if (comment == null) return result(ResultMsg.INVALID_VIDEO_ID);
        log.info("user {} commented on video {}, content: {}, commentId: {}", user.getId(), id, content, comment.getId());
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
                                 @RequestParam("dirId") Long dirId) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        User user = getUser();
        if (user == null) return result(ResultMsg.NOT_LOGIN);
        if (videoRepository.findById(videoId).isEmpty()) {
            return result(ResultMsg.INVALID_VIDEO_ID);
        }
        if (!videoService.possessDir(user, dirId)) return result(ResultMsg.COLLECT_WRONG_DIR);
        if (videoService.collect(videoId, dirId)) {
            return result(ResultMsg.COLLECT_SUCCESS);
        } else {
            result(ResultMsg.COLLECT_ALREADY);
        }
        return result();
    }

    @PostMapping("/collect/cancel")
    public Response cancelCollect(@RequestParam("appKey") String appKey,
                                  @RequestParam("videoId") Long videoId,
                                  @RequestParam("dirId") Long dirId) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        User user = getUser();
        if (user == null) return result(ResultMsg.NOT_LOGIN);
        if (videoRepository.findById(videoId).isEmpty()) {
            return result(ResultMsg.INVALID_VIDEO_ID);
        }
        if (!videoService.possessDir(user, dirId)) return result(ResultMsg.COLLECT_WRONG_DIR);
        if (videoService.cancelCollect(videoId, dirId)) {
            return result();
        } else return result(ResultMsg.COLLECT_WRONG_DIR);

    }

    /**
     * 获取收藏夹列表
     *
     * @return 收藏的视频列表
     */
    @GetMapping("/collect")
    public Response collectedVideos(@RequestParam("appKey") String appKey) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        User user = getUser();
        if (user == null) return result(ResultMsg.NOT_LOGIN);
        List<VideoDir> result = videoService.collectedDirs(user);
        log.info("user get collected dirs, userid: {}, dir list size: {}", user.getId(), result.size());
        return result(result);
    }

    /**
     * 创建收藏夹
     *
     * @param appKey appKey
     * @param name   收藏夹名字
     * @return 收藏夹
     */
    @PostMapping("/collect/create")
    public Response createCollectDir(@RequestParam("appKey") String appKey,
                                     @RequestParam("name") String name) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        User user = getUser();
        if (user == null) return result(ResultMsg.NOT_LOGIN);
        VideoDir videoDir = videoService.createDir(user, name);
        return result(videoDir);
    }

    @PostMapping("/collect/delete")
    public Response deleteCollectDir(@RequestParam("appKey") String appKey,
                                     @RequestParam("dirId") Long dirId) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        User user = getUser();
        if (user == null) return result(ResultMsg.NOT_LOGIN);
        if (!videoService.possessDir(user, dirId)) return result(ResultMsg.COLLECT_WRONG_DIR);
        videoService.removeDir(dirId);
        return result();
    }

    @PostMapping("/collect/rename")
    public Response renameCollectDir(@RequestParam("appKey") String appKey,
                                     @RequestParam("dirId") Long dirId,
                                     @RequestParam("name") String name) {
        if (commonService.appKeyValid(appKey)) return result(ResultMsg.APPKEY_INVALID);
        User user = getUser();
        if (user == null) return result(ResultMsg.NOT_LOGIN);
        if (!videoService.possessDir(user, dirId)) return result(ResultMsg.COLLECT_WRONG_DIR);
        VideoDir dir = videoService.renameDir(dirId, name);
        return result(dir);
    }

    /**
     * 下载视频
     *
     * @param id 视频id
     */
    @PostMapping("/download")
    public Response downloadVideo(@RequestParam("id") Long id) {
        User user = getUser();
        if (user == null) return result(ResultMsg.NOT_LOGIN);
        if (!videoRepository.findById(id).isPresent()) {
            return result(ResultMsg.INVALID_VIDEO_ID);
        }
        videoService.download(user, id);
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
