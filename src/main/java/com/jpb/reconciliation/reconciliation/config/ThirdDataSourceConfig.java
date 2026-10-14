//package com.jpb.reconciliation.reconciliation.config;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import javax.persistence.EntityManagerFactory;
//import javax.sql.DataSource;
//
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.datasource.DataSourceTransactionManager;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//
//@Configuration
//@EnableTransactionManagement
//@EnableJpaRepositories(basePackages = "com.jpb.reconciliation.reconciliation.tertiary.repository", entityManagerFactoryRef = "tertiaryEntityManagerFactory", transactionManagerRef = "tertiaryTransactionManager")
//public class ThirdDataSourceConfig {
//	@Bean(name = "tertiaryDataSourceProperties")
//	@ConfigurationProperties("spring.datasource.tertiary")
//	public DataSourceProperties tertiaryDataSourceProperties() {
//		return new DataSourceProperties();
//	}
//
//	@Bean(name = "tertiaryDataSource")
//	public DataSource tertiaryDataSource() {
//		return tertiaryDataSourceProperties().initializeDataSourceBuilder().build();
//	}
//
//	@Bean(name = "tertiaryJdbcTemplate")
//	public JdbcTemplate tertiaryJdbcTemplate(@Qualifier("tertiaryDataSource") DataSource dataSource) {
//		return new JdbcTemplate(dataSource);
//	}
//
//	@Bean(name = "tertiaryEntityManagerFactory")
//	public LocalContainerEntityManagerFactoryBean tertiaryEntityManagerFactory(EntityManagerFactoryBuilder builder) {
//		Map<String, Object> properties = new HashMap<>();
//		properties.put("hibernate.hbm2ddl.auto", "update");
//		properties.put("hibernate.dialect", "org.hibernate.dialect.Oracle10gDialect");
//		return builder.dataSource(tertiaryDataSource())
//				.packages("com.jpb.reconciliation.reconciliation.tertiary.entity").properties(properties)
//				.persistenceUnit("tertiary").build();
//	}
//
//	@Bean(name = "tertiaryTransactionManager")
//	public PlatformTransactionManager tertiaryTransactionManager(
//			@Qualifier("tertiaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
//		return new JpaTransactionManager(entityManagerFactory);
//	}
//
//	@Bean(name = "tertiaryJdbcTransactionManager")
//	public PlatformTransactionManager tertiaryJdbcTransactionManager(
//			@Qualifier("tertiaryDataSource") DataSource tertiaryDataSource) {
//		return new DataSourceTransactionManager(tertiaryDataSource);
//	}
//}
