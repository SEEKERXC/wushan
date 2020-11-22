package cn.ninanina.wushan.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "collect")
@EqualsAndHashCode(of = "id")
public class VideoDir {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String cover;

    @Column(nullable = false)
    private Long createTime;

    @Column(nullable = false)
    private Long updateTime;

    @Column(nullable = false, columnDefinition = "int(11) default 0")
    private Integer count;

    @Column(nullable = false, columnDefinition = "bit(1) default true")
    private Boolean isPublic;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    private User user;

    @ManyToMany(mappedBy = "videoDirs", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<VideoDetail> collectedVideos;
}
