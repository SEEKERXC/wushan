package cn.ninanina.wushan.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "video")
@EqualsAndHashCode(of = "id")
public class VideoDetail {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String url;

    @Column
    private String src;

    @Column(nullable = false)
    private String title;

    @Column
    private String titleZh;

    @Column
    private String coverUrl;

    @Column(nullable = false)
    private String duration;

    @Column(nullable = false)
    private Integer viewed;

    @Column(nullable = false, columnDefinition = "int(11) default 0")
    private Integer approved;

    @Column(nullable = false, columnDefinition = "bit(1) default true")
    private Boolean valid;

    @Column(nullable = false, columnDefinition = "bit(1) default false")
    private Boolean indexed;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "video_tag",
            joinColumns = {@JoinColumn(name = "video_id", referencedColumnName = "id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "tag_id", referencedColumnName = "id", nullable = false)})
    private List<TagDetail> tags;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "video_user_viewed",
            joinColumns = {@JoinColumn(name = "video_id", referencedColumnName = "id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)})
    private List<User> viewedUsers;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "video_user_collected",
            joinColumns = {@JoinColumn(name = "video_id", referencedColumnName = "id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)})
    private List<User> collectedUsers;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "video_user_download",
            joinColumns = {@JoinColumn(name = "video_id", referencedColumnName = "id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)})
    private List<User> downloadedUsers;

    @OneToMany
    @JsonIgnore
    private List<VideoDetail> relatedVideos;

}
