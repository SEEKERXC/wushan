package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.TagDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TagRepository extends JpaRepository<TagDetail, Long> {

    @Query(value = "select * from tag order by videoCount desc limit 0, ?1", nativeQuery = true)
    List<TagDetail> findByVideoCountDesc(int count);

    @Query(value = "select min(id) from tag where tagZh is null", nativeQuery = true)
    Long findTranslateWaterMark();
}
