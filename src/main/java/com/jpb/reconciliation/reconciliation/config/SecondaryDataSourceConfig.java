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
////SecondaryDataSourceConfig.java
//@Configuration
//@EnableTransactionManagement
//@EnableJpaRepositories(basePackages = "com.jpb.reconciliation.reconciliation.secondary.repository", entityManagerFactoryRef = "secondaryEntityManagerFactory", transactionManagerRef = "secondaryTransactionManager")
//public class SecondaryDataSourceConfig {
//
//	@Bean(name = "secondaryDataSourceProperties")
//	@ConfigurationProperties("spring.datasource.secondary")
//	public DataSourceProperties secondaryDataSourceProperties() {
//		return new DataSourceProperties();
//	}
//
//	@Bean(name = "secondaryDataSource")
//	public DataSource secondaryDataSource() {
//		return secondaryDataSourceProperties().initializeDataSourceBuilder().build();
//	}
//
//	@Bean(name = "secondaryJdbcTemplate")
//	public JdbcTemplate secondaryJdbcTemplate(@Qualifier("secondaryDataSource") DataSource dataSource) {
//		return new JdbcTemplate(dataSource);
//	}
//
//	@Bean(name = "secondaryEntityManagerFactory")
//	public LocalContainerEntityManagerFactoryBean secondaryEntityManagerFactory(EntityManagerFactoryBuilder builder) {
//		Map<String, Object> properties = new HashMap<>();
//		properties.put("hibernate.hbm2ddl.auto", "update");
//		properties.put("hibernate.dialect", "org.hibernate.dialect.Oracle10gDialect");
//		return builder.dataSource(secondaryDataSource())
//				.packages("com.jpb.reconciliation.reconciliation.secondary.entity").properties(properties)
//				.persistenceUnit("secondary").build();
//	}
//
//	@Bean(name = "secondaryTransactionManager")
//	public PlatformTransactionManager secondaryTransactionManager(
//			@Qualifier("secondaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
//		return new JpaTransactionManager(entityManagerFactory);
//	}
//
//	@Bean(name = "secondaryJdbcTransactionManager")
//	public PlatformTransactionManager secondaryJdbcTransactionManager(
//			@Qualifier("secondaryDataSource") DataSource secondaryDataSource) {
//		return new DataSourceTransactionManager(secondaryDataSource);
//	}
//}