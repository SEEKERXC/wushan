package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.ToWatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.List;

public interface ToWatchRepository extends JpaRepository<ToWatch, Long> {
    @Query(value = "delete from toWatch where id in ?1", nativeQuery = true)
    @Transactional
    @Modifying
    void deleteAllByIds(List<Long> ids);

    ToWatch findByUserIdAndVideoId(long userId, long videoId);

    @Query(value = "select * from toWatch where userId = ?1 order by addTime desc limit ?2, ?3", nativeQuery = true)
    List<ToWatch> findByUserId(long userId, int offset, int limit);
}
