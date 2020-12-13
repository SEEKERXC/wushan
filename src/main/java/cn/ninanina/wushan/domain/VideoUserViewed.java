package cn.ninanina.wushan.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@Entity
@Table(name = "video_user_viewed")
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class VideoUserViewed {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, columnDefinition = "bigint(20) default 0")
    private Long videoId;

    @Column(nullable = false, columnDefinition = "bigint(20) default 0")
    private Long userId;

    @Column(nullable = false, columnDefinition = "bigint(20) default 0")
    private Long time;

    @Column(nullable = false, columnDefinition = "int(11) default 0")
    private Integer viewCount;

    @Column(nullable = false, columnDefinition = "bigint(20) default 0")
    private Long watchTime; //TODO:用户停留在视频页面上面的总时间
}
