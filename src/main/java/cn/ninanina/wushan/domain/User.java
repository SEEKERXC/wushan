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
    @GeneratedValue
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

    @ManyToMany(mappedBy = "viewedUsers", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VideoDetail> viewedVideos;

    @ManyToMany(mappedBy = "downloadedUsers", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VideoDetail> downloadedVideos;

    @OneToMany(mappedBy = "user", targetEntity = Playlist.class, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("updateTime desc")
    @JsonIgnore
    private List<Playlist> playlists;

}
