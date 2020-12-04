package cn.ninanina.wushan.domain;

import cn.ninanina.wushan.common.Gender;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import javax.persistence.*;

@Entity
@Table(name = "user")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column
    @Enumerated(value = EnumType.ORDINAL)
    private Gender gender;

    @Column
    private int age;

    @Column
    private String photo;

    @Column(nullable = false)
    private Long registerTime;

    @Column(nullable = false)
    private Long lastLoginTime;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "video_user_viewed",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "video_id", referencedColumnName = "id", nullable = false)})
    private List<VideoDetail> viewedVideos;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "video_user_download",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "video_id", referencedColumnName = "id", nullable = false)})
    private List<VideoDetail> downloadedVideos;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "video_user_dislike",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "video_id", referencedColumnName = "id", nullable = false)})
    private List<VideoDetail> dislikedVideos;

    @ManyToMany(mappedBy = "approvedUsers", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Comment> approvedComments;

    @ManyToMany(mappedBy = "disapprovedUsers", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Comment> disapprovedComments;

    @OneToMany(mappedBy = "user", targetEntity = Playlist.class, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("updateTime desc")
    @JsonIgnore
    private List<Playlist> playlists;

}
