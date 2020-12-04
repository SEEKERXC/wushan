package cn.ninanina.wushan.repository;

import cn.ninanina.wushan.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query(value = "select count(1) from comment_user_approve where comment_id = ?1 and user_id = ?2", nativeQuery = true)
    Integer findApproved(long commentId, long userId);

    @Modifying
    @Transactional
    @Query(value = "insert into comment_user_approve values (?1, ?2)", nativeQuery = true)
    void insertApprove(long commendId, long userId);

    @Modifying
    @Transactional
    @Query(value = "delete from comment_user_approve where comment_id = ?1 and user_id = ?2", nativeQuery = true)
    void deleteApprove(long commentId, long userId);

    @Query(value = "select count(1) from comment_user_disapprove where comment_id = ?1 and user_id = ?2", nativeQuery = true)
    Integer findDisapproved(long commentId, long userId);

    @Modifying
    @Transactional
    @Query(value = "insert into comment_user_disapprove values (?1, ?2)", nativeQuery = true)
    void insertDisapprove(long commendId, long userId);

    @Modifying
    @Transactional
    @Query(value = "delete from comment_user_disapprove where comment_id = ?1 and user_id = ?2", nativeQuery = true)
    void deleteDisapprove(long commentId, long userId);
}
