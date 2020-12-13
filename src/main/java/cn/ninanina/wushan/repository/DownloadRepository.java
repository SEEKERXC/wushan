package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.VideoUserDownload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DownloadRepository extends JpaRepository<VideoUserDownload, Long> {
    VideoUserDownload findByUserIdAndVideoId(long userId, long videoId);

    @Query(value = "select videoId from video_user_download where userId = ?1", nativeQuery = true)
    List<Long> findVideoIdsByUserId(long userId);
}
