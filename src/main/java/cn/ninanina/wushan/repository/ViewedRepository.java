package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.VideoUserViewed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.List;

public interface ViewedRepository extends JpaRepository<VideoUserViewed, Long> {
    VideoUserViewed findByVideoIdAndUserId(long videoId, long userId);

    List<VideoUserViewed> findByUserId(long userId);

    @Query(value = "select * from video_user_viewed where userId = ?1 order by time desc limit ?2, ?3", nativeQuery = true)
    List<VideoUserViewed> findByLimit(long userId, int offset, int limit);

    @Query(value = "select * from video_user_viewed where userId = ?1 and time between ?2 and ?3 order by time desc", nativeQuery = true)
    List<VideoUserViewed> findByPeriod(long userId, long startTime, long endTime);

    @Modifying
    @Transactional
    @Query(value = "delete from video_user_viewed where id in ?1", nativeQuery = true)
    void deleteAllByIds(List<Long> ids);

    /**
     * 找出所有用户看过的视频id
     */
    @Query(value = "select videoId from video_user_viewed where userId = ?1", nativeQuery = true)
    List<Long> findViewedIds(long userId);
}
