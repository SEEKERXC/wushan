package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.LoginInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginRepository extends JpaRepository<LoginInfo, Long> {
}
