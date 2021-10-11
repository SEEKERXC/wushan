package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
