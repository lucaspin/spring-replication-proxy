package io.github.lucaspin.replicatingproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ReplicatingProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReplicatingProxyApplication.class, args);
	}

}
