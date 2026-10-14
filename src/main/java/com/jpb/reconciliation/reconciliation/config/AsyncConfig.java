package com.jpb.reconciliation.reconciliation.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "extractionExecutor")
	public Executor extractionExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(20);
		executor.setMaxPoolSize(200);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("Extraction-");
		executor.initialize();
		return executor;
	}
}
