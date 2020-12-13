package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    @Query(value = "select count(1) from video_collect where video_id = ?1 and dir_id = ?2", nativeQuery = true)
    int findCollected(long videoId, long playlistId);

    @Modifying
    @Transactional
    @Query(value = "insert into video_collect values ( ?1 , ?2 )", nativeQuery = true)
    void insertCollect(long videoId, long playlistId);

    @Modifying
    @Transactional
    @Query(value = "delete from video_collect where video_id = ?1 and dir_id = ?2", nativeQuery = true)
    void deleteCollect(long videoId, long playlistId);

    @Modifying
    @Transactional
    @Query(value = "delete from video_collect where dir_id = ?1", nativeQuery = true)
    void deleteCollect(long playlistId);

    /**
     * 获取用户所有的收藏夹的id
     */
    @Query(value = "select id from playlist where user_id = ?1", nativeQuery = true)
    List<Long> findAllPlaylistIds(long userId);

    /**
     * 获取收藏夹的所有视频id
     */
    @Query(value = "select video_id from video_collect where dir_id = ?1", nativeQuery = true)
    List<Long> findAllVideoIds(long playlistId);

    @Modifying
    @Transactional
    @Query(value = "delete from playlist where id = ?1", nativeQuery = true)
    void remove(long playlistId);
}
