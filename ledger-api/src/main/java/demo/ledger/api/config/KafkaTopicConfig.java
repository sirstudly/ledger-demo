package demo.ledger.api.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates our default topics if they don't exist.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String LEDGER_EVENTS_TOPIC = "ledger-events";
    public static final String LEDGER_ACCOUNT_EVENTS_TOPIC = "ledger-account-events";

    @Value( value = "${spring.kafka.bootstrap.servers}" )
    private String bootstrapAddress;

//    @Value( value = "${spring.kafka.topic.default}" )
//    private String defaultTopic;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put( AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress );
        return new KafkaAdmin( configs );
    }

    @Bean
    public NewTopic ledgerEventsTopic() {
        return TopicBuilder.name( LEDGER_EVENTS_TOPIC )
                .partitions( 1 )
                .replicas( 1 )
                .build();
    }

    @Bean
    public NewTopic ledgerAccountEventsTopic() {
        return TopicBuilder.name( LEDGER_ACCOUNT_EVENTS_TOPIC )
                .partitions( 1 )
                .replicas( 1 )
                .build();
    }
}