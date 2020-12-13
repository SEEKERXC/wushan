package cn.ninanina.wushan.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@Entity
@Table(name = "toWatch")
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class ToWatch {
    @Id
    @GeneratedValue
    private Long id;

    @Column
    private Long videoId;

    @Column
    private Long userId;

    @Column
    private Long addTime;
}
