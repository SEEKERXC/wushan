package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.VideoUserDislike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DislikeRepository extends JpaRepository<VideoUserDislike, Long> {
    VideoUserDislike findByUserIdAndVideoId(long userId, long videoId);

    List<VideoUserDislike> findByUserId(long userId);

    @Query(value = "select videoId from video_user_dislike where userId = ?1", nativeQuery = true)
    List<Long> findVideoIdsByUserId(long userId);
}
