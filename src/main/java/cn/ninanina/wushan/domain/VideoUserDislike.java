package cn.ninanina.wushan.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@Entity
@Table(name = "video_user_dislike")
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class VideoUserDislike {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, columnDefinition = "bigint(20) default 0")
    private Long videoId;

    @Column(nullable = false, columnDefinition = "bigint(20) default 0")
    private Long userId;
}
