package cn.ninanina.wushan.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "tag")
@Data
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class TagDetail {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String tag;

    @Column
    private String tagZh;

    @Column
    private Character start;

    @Column(nullable = false, columnDefinition = "bit(1) default false")
    private Boolean indexed;

    @Column(nullable = false)
    private Integer videoCount;

    @Column
    private String cover;

    @Column(nullable = false, columnDefinition = "int(11) default 0")
    private Integer searchCount;

    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VideoDetail> videos;

    @ManyToMany(mappedBy = "collectedTags", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Playlist> playlists;
}
