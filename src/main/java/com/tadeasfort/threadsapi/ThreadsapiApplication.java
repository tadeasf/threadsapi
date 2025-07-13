package com.tadeasfort.threadsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class ThreadsapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThreadsapiApplication.class, args);
	}

}
