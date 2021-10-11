package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.SearchInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchRepository extends JpaRepository<SearchInfo, Long> {
}
