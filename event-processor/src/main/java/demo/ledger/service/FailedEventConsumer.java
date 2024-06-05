package demo.ledger.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import demo.ledger.model.FailedProcessingEvent;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static demo.ledger.config.KafkaProducerConfig.FAILED_PROCESSING_TOPIC;

@Service
public class FailedEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger( FailedEventConsumer.class );

    private final EventService eventService;
    private final Gson gson;

    @Autowired
    public FailedEventConsumer( EventService eventService, Gson gson) {
        this.eventService = eventService;
        this.gson = gson;
    }

    @KafkaListener( groupId = "${spring.kafka.groupid}", topics = {FAILED_PROCESSING_TOPIC},
            containerFactory = "kafkaListenerContainerFactory" )
    public void consumeFailedProcessingOperation( @Payload String in,
                                        @Header( KafkaHeaders.RECEIVED_TOPIC ) String topic,
                                        @Header( KafkaHeaders.RECEIVED_PARTITION ) long partition,
                                        @Header( KafkaHeaders.OFFSET ) long offset,
                                        @Header( KafkaHeaders.RECEIVED_TIMESTAMP ) long ts ) {
        OffsetDateTime timestamp = OffsetDateTime.of( LocalDateTime.ofEpochSecond( ts / 1000, 0, ZoneOffset.UTC ), ZoneOffset.UTC );
        LOGGER.info( "Received topic={}, message=[{}], partition={}, offset={}, timestamp={}", topic, in, partition, offset, timestamp );
        FailedProcessingEvent event = gson.fromJson( in, FailedProcessingEvent.class );
        eventService.saveFailedProcessingEvent( event );
    }
}
