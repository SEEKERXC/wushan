package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.VideoDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

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

}
