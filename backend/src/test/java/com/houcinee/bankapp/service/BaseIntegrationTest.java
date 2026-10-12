package com.houcinee.bankapp.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

	private static final DockerImageName POSTGRES_IMAGE = DockerImageName
		.parse("public.ecr.aws/docker/library/postgres:17-alpine")
		.asCompatibleSubstituteFor("postgres");

	@ServiceConnection
	protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE)
		.withCommand("postgres", "-c", "max_connections=200");

	static {
		postgres.start();
	}

	@MockitoBean
	protected KafkaTemplate<String, String> kafkaTemplate;

	@MockitoBean
	protected StringRedisTemplate stringRedisTemplate;

}
