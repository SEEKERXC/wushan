package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.VersionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VersionRepository extends JpaRepository<VersionInfo, Long> {
    @Query(value = "select * from version order by updateTime desc limit 1", nativeQuery = true)
    VersionInfo getLatestVersion();
}
