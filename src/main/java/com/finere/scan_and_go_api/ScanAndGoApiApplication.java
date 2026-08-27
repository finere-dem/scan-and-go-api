package com.finere.scan_and_go_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class ScanAndGoApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ScanAndGoApiApplication.class, args);
	}

}
