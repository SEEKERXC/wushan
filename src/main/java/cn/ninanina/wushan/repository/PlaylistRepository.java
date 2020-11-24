package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
}
