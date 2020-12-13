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
@Table(name = "video")
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class VideoDetail implements Serializable { //todo:添加视频评分,根据点赞数、踩数、收藏数、下载数以及推送数计算
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

    @Column
    private Integer durationSeconds;

    @Column(nullable = false)
    private Integer viewed; //这是xvideo网站上面的播放数

    @Column(nullable = false, columnDefinition = "int(11) default 0")
    private Integer audience; //这是我们这里的播放数

    @Column(nullable = false, columnDefinition = "int(11) default 0")
    private Integer collected;

    @Column(nullable = false, columnDefinition = "int(11) default 0")
    private Integer downloaded;

    @Column(nullable = false, columnDefinition = "int(11) default 0")
    private Integer liked;

    @Column(nullable = false, columnDefinition = "int(11) default 0")
    private Integer disliked;

    @Column(nullable = false, columnDefinition = "int(11) default 0")
    private Integer commentNum;

    @Column(nullable = false, columnDefinition = "bit(1) default true")
    private Boolean valid;

    @Column(nullable = false, columnDefinition = "bit(1) default false")
    private Boolean indexed;

    @Column(nullable = false, columnDefinition = "bigint(20) default 0")
    private Long updateTime;

    @Transient
    private List<TagDetail> tags;

    @ManyToMany(mappedBy = "collectedVideos", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Playlist> playlists;
}
