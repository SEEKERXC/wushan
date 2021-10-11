package cn.ninanina.wushan.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * 这个类用于记录登录。一个app可以不同时登录多个用户，一个用户也可以在多个app上登录。
 */
@Entity
@Table(name = "user")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class LoginInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;

    @Column(nullable = false)
    private String appKey;

    @Column(nullable = false)
    private long userId;

    @Column(nullable = false)
    private long time;

    @Column(nullable = false)
    private String ip;
}
