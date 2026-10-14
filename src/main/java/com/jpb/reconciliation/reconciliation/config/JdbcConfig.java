package com.jpb.reconciliation.reconciliation.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

@Configuration
public class JdbcConfig {


	@Bean
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean
	public SimpleJdbcCall simpleJdbcCall(JdbcTemplate jdbcTemplate) {
		return new SimpleJdbcCall(jdbcTemplate);
	}
}