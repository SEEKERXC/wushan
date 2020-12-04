package cn.ninanina.wushan.service;

import cn.ninanina.wushan.domain.Comment;
import cn.ninanina.wushan.domain.TagDetail;
import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.service.cache.VideoCacheManager;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;

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
    List<VideoDetail> recommendVideos(Long userId, @Nonnull String appKey, @Nonnull String type, @Nonnull Integer offset, @Nonnull Integer limit);

    /**
     * 获取指定视频的有效信息，即更新视频链接，当客户端请求视频详情，并且视频链接失效时才调用。
     * <p>视频实际有效为3小时，我们规定超过2.5小时则失效。
     * <p>如果数据库/缓存中的视频链接还有效，则不打开页面重新请求，否则就调用selenium的接口进行更新。
     */
    VideoDetail getVideoDetail(@Nonnull Long videoId, Long userId);

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
    List<VideoDetail> search(@Nonnull String query, @Nonnull Integer offset, @Nonnull Integer limit);

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
     */
    List<VideoDetail> viewedVideos(@Nonnull Long userId, @Nonnull Integer offset, @Nonnull Integer limit);

    /**
     * 下载视频，做个记录方便推荐。因为下载的视频一定是用户最喜欢的，权重比收藏还要高。
     */
    void download(@Nonnull Long userId, @Nonnull Long videoId);

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
     * 获取当前在线视频排行，根据观众数降序排列
     *
     * @param limit 限制数量
     * @return Pair列表，左为video详情，右为当前观看人数。
     */
    List<Pair<VideoDetail, Integer>> onlineRank(@Nonnull Integer offset, @Nonnull Integer limit);

}
