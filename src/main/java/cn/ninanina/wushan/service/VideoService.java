package cn.ninanina.wushan.service;

import cn.ninanina.wushan.domain.Comment;
import cn.ninanina.wushan.domain.ToWatch;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.domain.VideoUserViewed;
import org.springframework.data.util.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 提供所有视频相关API。
 * <p>所有返回的视频详情都包含真实可用的封面链接,但列表信息不包含视频实时链接。
 * <p>获取视频的实时链接需要单独请求。
 * <p>重点：xvideos的视频链接有效时长为3小时。所以，我们规定超过2.5小时的链接为失效。
 */
public interface VideoService {
    /**
     * 获取推荐视频列表，返回10个。目前采用最简单的实现方式：
     * <p>首先取validVideoCache里面的值，即当前有效视频。有效视频取完后，
     * <p>然后根据用户收藏、下载、浏览过的视频推荐，
     * <p>如果没有任何记录或者记录很少，则从选定的精华视频中推荐。
     */
    List<VideoDetail> recommendVideos(Long userId, @Nonnull String appKey, @Nonnull String type, @Nonnull Integer limit);

    /**
     * 获取指定视频的有效信息，即更新视频链接，当客户端请求视频详情，并且视频链接失效时才调用。
     * <p>视频实际有效为3小时，我们规定超过2.5小时则失效。
     * <p>如果数据库/缓存中的视频链接还有效，则不打开页面重新请求，否则就调用selenium的接口进行更新。
     */
    VideoDetail getVideoDetail(@Nonnull Long videoId, Long userId, Boolean withoutSrc, Boolean record);

    /**
     * 增量记录播放时长
     *
     * @param userId  用户id
     * @param videoId 视频id
     * @param time    增加的播放时长，以秒为单位
     */
    void recordWatch(long userId, long videoId, int time);

    /**
     * 首先获取一级相关视频，一般有20-50个，获取完了之后获取二级相关，一般有1000个左右。
     *
     * @param videoId 视频id
     * @param limit   相关视频数量
     * @return 相关视频列表
     */
    List<VideoDetail> relatedVideos(@Nonnull Long videoId, @Nonnull Integer offset, @Nonnull Integer limit);

    /**
     * 搜索建议
     */
    List<String> suggestSearch(@Nonnull String word);

    /**
     * 根据关键词搜索视频。一般用户都是用中文搜索，根据标题和标签进行匹配，并且同时进行中英文匹配。后期会考虑根据视频评论来匹配
     * 视频标题和标签翻译都来根据有道翻译。英译汉能力有道>谷歌>百度>others
     *
     * @param query 关键词，可以中文可以英文
     * @return 匹配的视频列表
     */
    List<VideoDetail> search(@Nonnull String query, @Nonnull Integer offset, @Nonnull Integer limit, @Nonnull String sort);

    /**
     * 发表视频评论
     *
     * @param userId   当前用户id
     * @param videoId  视频id
     * @param content  评论内容
     * @param parentId 评论父id,可为空
     * @return 生成的评论信息
     */
    Comment commentOn(@Nonnull Long userId, @Nonnull Long videoId, @Nonnull String content, @Nullable Long parentId);

    /**
     * 点赞评论/取消点赞
     *
     * @param userId  用户id
     * @param comment 视频评论
     * @return 点赞返回true，取消返回false
     */
    Comment approveComment(@Nonnull Long userId, @Nonnull Comment comment);

    /**
     * 踩评论/取消踩
     *
     * @param userId  用户id
     * @param comment 视频评论
     * @return 踩返回true，取消返回false
     */
    Comment disapproveComment(@Nonnull Long userId, @Nonnull Comment comment);

    /**
     * 分页获取评论
     */
    List<Comment> getComments(@Nonnull Long userId, @Nonnull Long videoId, @Nonnull Integer page, @Nonnull Integer size, @Nonnull String sort);

    /**
     * 分页获取子评论
     */
    List<Comment> getChildComments(@Nonnull Integer page, @Nonnull Integer size, @Nonnull Long commentId);

    /**
     * 获取用户看过的视频列表，分段获取
     * 如果指定了startOfDay就获取当天的所有记录，否则按照offset和limit获取
     */
    List<Pair<VideoUserViewed, VideoDetail>> viewedVideos(@Nonnull Long userId, Integer offset, Integer limit, Long startOfDay);

    /**
     * 返回用户所有观看记录，不需要排序
     */
    List<VideoUserViewed> allViewed(@Nonnull Long userId);

    /**
     * 删除观看记录
     */
    void deleteViewed(@Nonnull List<Long> viewedIds);

    /**
     * 下载视频，做个记录方便推荐。因为下载的视频一定是用户最喜欢的，权重比收藏还要高。
     */
    void download(@Nonnull Long userId, @Nonnull VideoDetail video);

    /**
     * 用户退出视频播放
     */
    void exitDetail(@Nonnull Long videoId);

    /**
     * 指定视频的当前观众数
     *
     * @param videoId 视频id
     * @return 观众人数
     */
    int audiences(@Nonnull Long videoId);

    /**
     * 获取当前链接有效的视频，并且提供当前观看人数
     */
    List<VideoDetail> instantVideos(@Nonnull String appKey, Long userId, @Nonnull Integer limit);

    /**
     * 喜欢/取消喜欢video
     */
    VideoDetail likeVideo(@Nonnull Long userId, @Nonnull VideoDetail video);

    List<Long> likedVideos(@Nonnull Long userId);

    List<VideoDetail> likedVideos(long userId, int offset, int limit);

    /**
     * 不喜欢/取消不喜欢video
     */
    VideoDetail dislikeVideo(@Nonnull Long userId, @Nonnull VideoDetail video);

    List<Long> dislikedVideos(@Nonnull Long userId);

    /**
     * 新增稍后观看
     */
    ToWatch newToWatch(long userId, long videoId);

    /**
     * 批量删除稍后观看
     */
    void deleteToWatch(List<Long> toWatchIds);

    /**
     * 获取稍后观看列表，根据添加时间降序排列
     */
    List<Pair<ToWatch, VideoDetail>> listToWatches(long userId, int offset, int limit);

}
