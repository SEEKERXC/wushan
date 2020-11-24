package cn.ninanina.wushan.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Entity
@Table(name = "appInfo")
@Data
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class AppInfo {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String appKey;

    @Column(nullable = false)
    private Long installTime;

    @Column
    private Long uninstallTime;

    @Column
    private Long userId;
}
