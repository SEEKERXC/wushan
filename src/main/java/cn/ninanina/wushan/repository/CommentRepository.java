package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
