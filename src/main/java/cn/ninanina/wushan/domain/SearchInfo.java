package cn.ninanina.wushan.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@Entity
@Table(name = "video")
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class SearchInfo {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    private String appKey;

    @Column(nullable = false)
    private String ip;

    @Column(nullable = false)
    private Long time;

    @Column(nullable = false)
    private String sort;
}
