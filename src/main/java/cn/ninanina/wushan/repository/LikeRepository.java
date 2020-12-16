package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.VideoUserLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LikeRepository extends JpaRepository<VideoUserLike, Long> {
    VideoUserLike findByUserIdAndVideoId(long userId, long videoId);

    @Query(value = "select videoId from video_user_like where userId = ?1", nativeQuery = true)
    List<Long> findByUserId(long userId);

    @Query(value = "select videoId from video_user_like where userId = ?1 order by time desc limit ?2 , ?3", nativeQuery = true)
    List<Long> findByUserId(long userId, int offset, int limit);
}
