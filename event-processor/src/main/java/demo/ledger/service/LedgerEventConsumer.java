package demo.ledger.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import demo.ledger.model.FailedProcessingEvent;
import demo.ledger.model.Ledger;
import demo.ledger.model.dto.ApiOperation;
import demo.ledger.model.dto.EventType;
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
import static demo.ledger.config.KafkaProducerConfig.LEDGER_EVENTS_TOPIC;

@Service
public class LedgerEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger( LedgerEventConsumer.class );

    private final LedgerService ledgerService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Gson gson;

    @Autowired
    public LedgerEventConsumer( LedgerService ledgerService, Gson gson, KafkaTemplate<String, String> kafkaTemplate ) {
        this.ledgerService = ledgerService;
        this.gson = gson;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener( groupId = "${spring.kafka.groupid}", topics = {"ledger-events"},
            containerFactory = "kafkaListenerContainerFactory" )
    public void consumeLedgerOperation( @Payload String in,
                                        @Header( KafkaHeaders.RECEIVED_TOPIC ) String topic,
                                        @Header( KafkaHeaders.RECEIVED_PARTITION ) long partition,
                                        @Header( KafkaHeaders.OFFSET ) long offset,
                                        @Header( KafkaHeaders.RECEIVED_TIMESTAMP ) long ts ) {
        OffsetDateTime timestamp = OffsetDateTime.of( LocalDateTime.ofEpochSecond( ts / 1000, 0, ZoneOffset.UTC ), ZoneOffset.UTC );
        LOGGER.info( "Received topic={}, message=[{}], offset={}, timestamp={}", topic, in, offset, timestamp );
        JsonObject obj = gson.fromJson( in, JsonObject.class );
        if ( obj.has( "eventType" ) ) {
            if ( EventType.CREATE_LEDGER.name().equals( obj.get( "eventType" ).getAsString() ) ) {
                JsonObject req = obj.get( "data" ).getAsJsonObject();
                // FIXME: deserialise to DTOs from common package
                try {
                    Ledger ledger = ledgerService.createLedger(
                            req.get( "uuid" ).getAsString(),
                            req.get( "name" ).getAsString(),
                            req.get( "description" ).getAsString() );

                    sendMessage( LEDGER_EVENTS_TOPIC, gson.toJson(
                            new ApiOperation<Ledger>()
                                    .withEventType( EventType.LEDGER_CREATED )
                                    .withData( ledger ) ) );
                }
                catch ( Exception ex ) {
                    LOGGER.error( "Failed to process offset=" + offset, ex );
                    sendMessage( FAILED_PROCESSING_TOPIC, gson.toJson( FailedProcessingEvent.builder()
                            .topic( topic )
                            .partition( partition )
                            .offset( offset )
                            .input( in )
                            .timestamp( timestamp )
                            .error( ex.getMessage() )
                            .stacktrace( ExceptionUtils.getStackTrace( ex ) )
                            .build() )
                    );
                }
            }
        }
        else {
            sendMessage( FAILED_PROCESSING_TOPIC, gson.toJson( FailedProcessingEvent.builder()
                    .topic( topic )
                    .partition( partition )
                    .offset( offset )
                    .input( in )
                    .timestamp( timestamp )
                    .error( "Missing event type" )
                    .build() ) );
            LOGGER.warn( "Missing eventType on event offset={}", offset );
        }
    }

    protected void sendMessage( String topic, String payload ) {
        kafkaTemplate.send( topic, payload ).whenComplete( ( result, ex ) -> {
            if ( ex == null ) {
                LOGGER.info( "Sent message=[payload={}] to topic={} with offset={}",
                        payload, topic, result.getRecordMetadata().offset() );
            }
            else {
                LOGGER.info( "Unable to send message=[payload={}] to topic={} due to : {}",
                        payload, topic, ex.getMessage() );
            }
        } );
    }
}
