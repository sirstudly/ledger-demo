package demo.ledger.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table( name = "failed_events" )
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FailedProcessingEvent {

    @Id
    @GeneratedValue
    private Long id;

    @Column( name = "kafka_topic", nullable = false )
    private String topic;

    @Column( name = "kafka_partition", nullable = false )
    private long partition;

    @Column( name = "kafka_offset", nullable = false )
    private long offset;

    @Column( name = "timestamp" )
    private OffsetDateTime timestamp;

    @Column( name = "input", columnDefinition = "text" )
    private String input;

    @Column( name = "error", columnDefinition = "text" )
    private String error;

    @Column( name = "stacktrace", columnDefinition = "text" )
    private String stacktrace;
}