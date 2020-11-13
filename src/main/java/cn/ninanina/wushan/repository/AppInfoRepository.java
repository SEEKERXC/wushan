package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.AppInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppInfoRepository extends JpaRepository<AppInfo, Long> {
    AppInfo findByAppKey(String appKey);
}
