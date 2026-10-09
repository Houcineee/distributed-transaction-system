package com.houcinee.bankapp.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
public abstract class BaseIntegrationTest {

	private static final DockerImageName POSTGRES_IMAGE = DockerImageName
		.parse("public.ecr.aws/docker/library/postgres:17-alpine")
		.asCompatibleSubstituteFor("postgres");

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

	@MockitoBean
	protected KafkaTemplate<String, String> kafkaTemplate;

	@MockitoBean
	protected StringRedisTemplate stringRedisTemplate;

}
