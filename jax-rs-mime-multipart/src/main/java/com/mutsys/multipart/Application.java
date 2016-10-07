package com.mutsys.multipart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@EnableAutoConfiguration
public class Application {
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);
    }
	
	@Bean
	public MultipartResourceConfig multipartResourceConfig(MultipartResource multipartResource) {
		MultipartResourceConfig multipartResourceConfig = new MultipartResourceConfig();
		multipartResourceConfig.setMultipartResource(multipartResource);
		return multipartResourceConfig;
	}
	
	@Bean
	public MultipartResource multipartResource() {
		return new MultipartResource();
	}
	

}