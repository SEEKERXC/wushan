package cn.ninanina.wushan.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "comment")
@Data
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Comment {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, columnDefinition = "varchar(1024)")
    private String content;

    @Column(nullable = false)
    private Long time;

    @Column(nullable = false, columnDefinition = "int(11) default 0")
    private Integer approve;

    @Column(nullable = false, columnDefinition = "int(11) default 0")
    private Integer disapprove;

    @Transient
    private Boolean approved = false;

    @Transient
    private Boolean disapproved = false;

    @Column
    private Long parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private VideoDetail video;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "comment_user_approve",
            joinColumns = {@JoinColumn(name = "comment_id", referencedColumnName = "id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)})
    private List<User> approvedUsers;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "comment_user_disapprove",
            joinColumns = {@JoinColumn(name = "comment_id", referencedColumnName = "id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)})
    private List<User> disapprovedUsers;
}
