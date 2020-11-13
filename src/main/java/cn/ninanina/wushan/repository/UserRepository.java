package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUUID(Integer UUID);

    User findByUUIDAndPassword(Integer UUID, String password);
}
