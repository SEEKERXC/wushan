package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.VideoDetail;
import cn.ninanina.wushan.domain.VideoUserViewed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

public interface VideoRepository extends JpaRepository<VideoDetail, Long> {

    @Query(value = "select max(id) from video", nativeQuery = true)
    Long findMaxId();

    @Query(value = "select min(id) from video where titleZh is null", nativeQuery = true)
    Long findTranslateWatermark();

    /**
     * 获取没有建索引的最小videoId
     */
    @Query(value = "select min(id) from video where titleZh is not null and indexed is false", nativeQuery = true)
    Long findIndexingWatermark();

    /**
     * 获取相关视频的id，需要和下面的联合调用
     *
     * @param videoId 主视频id
     */
    @Query(value = "select relatedVideos_id from video_relatedVideos where VideoDetail_id = ?1", nativeQuery = true)
    List<Long> findRelatedVideoIds(long videoId);

    /**
     * 反向寻找相关视频，需要和上面的联合调用
     *
     * @param videoId 主视频id
     */
    @Query(value = "select VideoDetail_id from video_relatedVideos where relatedVideos_id = ?1", nativeQuery = true)
    List<Long> findRelatedVideoIds_reverse(long videoId);

    @Modifying
    @Transactional
    @Query(value = "delete from video_relatedVideos where VideoDetail_id = ?1 or relatedVideos_id = ?1", nativeQuery = true)
    void deleteFromRelated(long videoId);

    /**
     * 对给定的id集合进行排序并抽取一部分，返回子id集合。不能返回video集合，因为会使offset变得很慢。
     */
    @Query(value = "select video.id from video where video.id in (:ids) order by " + "video." + ":order desc limit :offset, :limit", nativeQuery = true)
    List<Long> findLimitedInIdsWithOrder(@Param("ids") List<Long> ids, @Param("order") String order, @Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 对于视频数量较少的tag，直接采用sql获取
     */
    @Query(value = "select * from video where id in ( select video_id from video_tag where tag_id = ?1 ) order by ?2 desc limit ?3, ?4", nativeQuery = true)
    List<VideoDetail> findLimitedWithOrder(long tagId, String sort, int offset, int limit);

    //获取某标签的某一个视频的封面
    @Query(value = "select coverUrl from video where id = (select video_id from video_tag where tag_id = ?1 limit 1)", nativeQuery = true)
    String findCoverForTag(long tagId);

}
