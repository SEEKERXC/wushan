package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.VideoUserLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LikeRepository extends JpaRepository<VideoUserLike, Long> {
    VideoUserLike findByUserIdAndVideoId(long userId, long videoId);

    List<VideoUserLike> findByUserId(long userId);
}
