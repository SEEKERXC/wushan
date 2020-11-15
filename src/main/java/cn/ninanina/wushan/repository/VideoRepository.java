package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.VideoDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

public interface VideoRepository extends JpaRepository<VideoDetail, Long> {

    /**
     * 获取播放数最高的n个视频，只在程序初始化时调用。保证视频有封面
     */
    @Query(value = "select * from video where coverUrl is not null and valid is true order by viewed desc limit 0, ?1", nativeQuery = true)
    List<VideoDetail> findHottest(int n);

    @Query(value = "select min(id) from video where titleZh is null", nativeQuery = true)
    Long findTranslateWatermark();

    /**
     * 获取没有建索引的最小videoId
     */
    @Query(value = "select min(id) from video where titleZh is not null and indexed is false", nativeQuery = true)
    Long findIndexingWatermark();

    /**
     * 根据视频url获取视频。
     */
    VideoDetail findByUrl(String url);

    /**
     * 获取相关视频的id，需要和下面的联合调用
     *
     * @param videoId 主视频id
     */
    @Query(value = "select relatedVideos_id from video_relatedVideos where VideoDetail_id = ?1", nativeQuery = true)
    Set<Long> findRelatedVideoIds(long videoId);

    /**
     * 反向寻找相关视频，需要和上面的联合调用
     * @param videoId 主视频id
     */
    @Query(value = "select VideoDetail_id from video_relatedVideos where relatedVideos_id = ?1", nativeQuery = true)
    Set<Long> findRelatedVideoIds_reverse(long videoId);

    @Modifying
    @Transactional
    @Query(value = "insert into video_relatedVideos values( ?1 , ?2 )", nativeQuery = true)
    void insertRelated(long id1, long id2);

    @Query(value = "select count(1) from video_relatedVideos where VideoDetail_id = ?1 and relatedVideos_id = ?2", nativeQuery = true)
    int existRelation(Long id1, Long id2);


}
