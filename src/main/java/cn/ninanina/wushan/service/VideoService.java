package cn.ninanina.wushan.service;

import cn.ninanina.wushan.domain.Comment;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.domain.VideoDetail;
import com.sun.istack.NotNull;
import org.apache.commons.lang3.tuple.Pair;

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
     * <p>根据用户浏览过的tag获取，如果用户未登录或者没看过视频，就返回随机热点视频
     * <p>根据appKey来保存每个app的当前获取状态。
     *
     * @param user 用户信息
     * @return 推荐视频列表，视频链接不一定有效
     * @Param appKey 用来区分app
     */
    List<VideoDetail> recommendVideos(@Nonnull User user, @Nonnull String appKey, @Nonnull int limit);

    /**
     * 获取随机热点视频列表，从缓存中获取，每次取10个。
     * 根据appKey来保存每个app的当前获取状态。
     *
     * @return 视频信息列表，链接不一定有效
     * @Param appKey 用来区分app
     */
    List<VideoDetail> randomHotVideos(@Nonnull String appKey, @Nonnull Integer limit);

    /**
     * 获取指定视频的有效信息，即更新视频链接，当客户端请求视频详情，并且视频链接失效时才调用。
     * <p>视频实际有效为3小时，我们规定超过2.5小时则失效。
     * <p>如果数据库/缓存中的视频链接还有效，则不打开页面重新请求，否则就调用selenium的接口进行更新。
     *
     * @param videoId 视频id
     * @return 视频当前有效信息
     */
    VideoDetail getVideoDetail(long videoId, User user);

    /**
     * 获取相关视频。先在数据库中查询相关视频id，如果没有则在使用webmagic进行爬取。
     * 初期设计webmagic爬虫的时候，无法考虑存相关视频，而只有当获取了足够多视频之后才可以加入相关视频。
     *
     * @param videoId 视频id
     * @param limit 相关视频数量
     * @return 相关视频列表
     */
    List<VideoDetail> relatedVideos(long videoId, int limit);

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
     * @param user 当前用户
     * @param videoId 视频id
     * @param content 评论内容
     * @param parentId 评论父id,可为空
     * @return 生成的评论信息
     */
    Comment commentOn(@Nonnull User user, @Nonnull Long videoId, @Nonnull String content, @Nullable Long parentId);

    /**
     * 收藏视频/取消收藏
     *
     * @param user 用户
     * @param videoId 视频id
     * @return true表示收藏，false表示取消收藏
     */
    Boolean collect(@Nonnull User user, @Nonnull Long videoId);

    /**
     * 获取用户收藏的所有视频列表
     *
     * @param user 用户
     */
    List<VideoDetail> collectedVideos(User user);

    /**
     * 获取用户看过的视频列表，分段获取
     *
     * @param user 用户
     * @param offset offset
     * @param limit limit
     */
    List<VideoDetail> viewedVideos(@Nonnull User user, @Nonnull Integer offset, @Nonnull Integer limit);

    /**
     * 下载视频，做个记录方便推荐。因为下载的视频一定是用户最喜欢的，权重比收藏还要高。
     *
     * @param user 下载的用户
     * @param videoId 下载的视频id
     */
    void download(@Nonnull User user, @Nonnull Long videoId);

    /**
     * 用户退出视频播放
     */
    void exitDetail(@Nonnull Long videoId);

    /**
     * 指定视频的当前观众数
     * @param videoId 视频id
     * @return 观众人数
     */
    int audiences(@Nonnull Long videoId);

    /**
     * 获取当前在线视频排行，根据观众数降序排列
     * @param limit 限制数量
     * @return Pair列表，左为video详情，右为当前观看人数。
     */
    List<Pair<VideoDetail,Integer>> onlineRank(@Nonnull Integer limit);

}
