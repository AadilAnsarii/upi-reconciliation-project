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
//import org.springframework.boot.jdbc.DataSourceBuilder;
//import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//
////PrimaryDataSourceConfig.java
//@Configuration
//@EnableTransactionManagement
//@EnableJpaRepositories(basePackages = "com.jpb.reconciliation.reconciliation.repository", entityManagerFactoryRef = "primaryEntityManagerFactory", transactionManagerRef = "primaryTransactionManager")
//public class PrimaryDataSourceConfig {
//
//	@Bean(name ="primaryDataSourceProperties")
//	@Primary
//	@ConfigurationProperties("spring.datasource")
//	public DataSourceProperties primaryDataSourceProperties() {
//		return new DataSourceProperties();
//	}
//
//	@Bean(name="primaryDataSource")
//	@Primary
//	public DataSource primaryDataSource() {
//		return primaryDataSourceProperties().initializeDataSourceBuilder().build();
//	}
//
//	@Bean(name="primaryEntityManagerFactory")
//	@Primary
//	public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(EntityManagerFactoryBuilder builder) {
//		
//		Map<String, Object> properties = new HashMap<>();
//		properties.put("hibernate.hbm2ddl.auto", "update");
//		properties.put("hibernate.dialect", "org.hibernate.dialect.Oracle10gDialect");
//		return builder.dataSource(primaryDataSource()).packages("com.jpb.reconciliation.reconciliation.entity")
//				.properties(properties)
//				.persistenceUnit("primary").build();
//	}
//
//	@Bean(name="primaryTransactionManager")
//	@Primary
//	public PlatformTransactionManager primaryTransactionManager(
//			@Qualifier("primaryEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
//		return new JpaTransactionManager(entityManagerFactory);
//	}
//}
