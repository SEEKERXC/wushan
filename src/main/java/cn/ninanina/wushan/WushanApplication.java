package cn.ninanina.wushan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan()
public class WushanApplication {

	public static void main(String[] args) {
		SpringApplication.run(WushanApplication.class, args);
	}

}
