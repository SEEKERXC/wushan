package cn.ninanina.wushan.service;

import cn.ninanina.wushan.domain.Playlist;
import cn.ninanina.wushan.domain.VideoDetail;

import javax.annotation.Nonnull;
import java.util.List;

public interface PlaylistService {

    /**
     * 新建收藏夹
     *
     * @param user 用户
     * @param name 收藏夹名字
     * @return 创建结果
     */
    Playlist create(@Nonnull Long userId, @Nonnull String name);

    /**
     * 查看收藏夹是否属于用户
     *
     * @param user  用户
     * @param dirId 收藏夹id
     * @return 属于返回videoDir，不属于返回null
     */
    Playlist possess(@Nonnull Long userId, @Nonnull Long dirId);

    /**
     * 删除收藏夹
     *
     * @param user 用户
     * @param id   收藏夹id
     */
    void delete(@Nonnull Long id);

    /**
     * 重命名收藏夹
     *
     * @param id   收藏夹id
     * @param name 新名字
     */
    Playlist rename(@Nonnull Long id, @Nonnull String name);

    /**
     * 获取用户的收藏文件夹列表
     */
    List<Playlist> listAll(@Nonnull Long userId);

    /**
     * 收藏视频
     *
     * @return true表示收藏成功，false表示已收藏过
     */
    Boolean collect(@Nonnull Playlist playlist, @Nonnull VideoDetail videoDetail);

    /**
     * 取消收藏
     *
     * @return true表示取消成功，false表示收藏夹为空或者收藏夹不包含给定video
     */
    Boolean cancelCollect(@Nonnull Playlist playlist, @Nonnull VideoDetail videoDetail);

    /**
     * 获取收藏夹所有视频
     *
     * @param id 收藏夹id
     */
    List<VideoDetail> listVideos(@Nonnull Long id);
}
