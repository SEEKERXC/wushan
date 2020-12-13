package cn.ninanina.wushan.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Data
@Entity
@Table(name = "playlist")
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Playlist implements Serializable {
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
    private User user;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "video_collect",
            joinColumns = {@JoinColumn(name = "dir_id", referencedColumnName = "id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "video_id", referencedColumnName = "id", nullable = false)})
    private List<VideoDetail> collectedVideos;
}
