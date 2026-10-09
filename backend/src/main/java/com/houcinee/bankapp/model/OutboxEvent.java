package com.houcinee.bankapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "outbox_events")
public class OutboxEvent extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator(style = UuidGenerator.Style.VERSION_7)
	private UUID id;

	@NotNull
	@Column(name = "aggregate_type", nullable = false, length = 50)
	private String aggregateType;

	@NotNull
	@Column(name = "aggregate_id", nullable = false)
	private UUID aggregateId;

	@NotNull
	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@NotNull
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String payload;

	@NotNull
	@Column(nullable = false)
	private Boolean processed = false;

	public OutboxEvent(String aggregateType, UUID aggregateId, String eventType, String payload) {
		this.aggregateType = aggregateType;
		this.aggregateId = aggregateId;
		this.eventType = eventType;
		this.payload = payload;
		this.processed = false;
	}

	public void markProcessed() {
		this.processed = true;
	}
}
