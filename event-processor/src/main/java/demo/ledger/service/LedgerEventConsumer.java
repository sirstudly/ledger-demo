package demo.ledger.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import demo.ledger.model.FailedProcessingEvent;
import demo.ledger.model.Ledger;
import demo.ledger.model.LedgerAccount;
import demo.ledger.model.LedgerEntry;
import demo.ledger.model.LedgerTransaction;
import demo.ledger.model.dto.ApiOperation;
import demo.ledger.model.dto.EventType;
import demo.ledger.model.dto.FailedResponse;
import demo.ledger.model.exception.DuplicateKeyException;
import demo.ledger.model.exception.NotFoundException;
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
        try {
            JsonObject obj = gson.fromJson( in, JsonObject.class );
            if ( EventType.CREATE_LEDGER.name().equals( obj.get( "eventType" ).getAsString() ) ) {
                handleCreateLedgerEvent( obj );
            }
            else if ( EventType.CREATE_LEDGER_ACCOUNT.name().equals( obj.get( "eventType" ).getAsString() ) ) {
                handleCreateLedgerAccountEvent( obj );
            }
            else if ( EventType.CREATE_LEDGER_TRANSACTION.name().equals( obj.get( "eventType" ).getAsString() ) ) {
                handleCreateLedgerTransactionEvent( obj );
            }
            else {
                LOGGER.warn( "Unsupported event type: {}", obj.get( "eventType" ).getAsString() );
            }
        }
        catch ( DuplicateKeyException ex ) {
            sendMessage( LEDGER_EVENTS_TOPIC, gson.toJson(
                    new ApiOperation<FailedResponse>()
                            .withEventType( ex.getEventType() )
                            .withData( new FailedResponse( ex.getUuid(), ex.getMessage() ) ) ) );
        }
        catch ( NotFoundException ex ) {
            sendMessage( LEDGER_EVENTS_TOPIC, gson.toJson(
                    new ApiOperation<FailedResponse>()
                            .withEventType( ex.getEventType() )
                            .withData( new FailedResponse( ex.getUuid(), ex.getMessage() ) ) ) );
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

    /**
     * Saves the new ledger in the datastore and emits a LEDGER_CREATED event on successful completion.
     *
     * @param obj deserialized event payload
     * @throws DuplicateKeyException if UUID already exists for ledger
     */
    private void handleCreateLedgerEvent( JsonObject obj ) throws DuplicateKeyException {
        JsonObject req = obj.get( "data" ).getAsJsonObject();
        Ledger ledger = gson.fromJson( gson.toJson( req ), Ledger.class );

        // first check that another ledger doesn't already exist with the same UUID
        if ( ledgerService.getLedger( ledger.getUuid() ).isPresent() ) {
            throw new DuplicateKeyException( EventType.LEDGER_CREATION_FAILED, ledger.getUuid(), "Ledger already exists with this UUID" );
        }

        ledger = ledgerService.createLedger(
                ledger.getUuid(),
                ledger.getName(),
                ledger.getDescription() );

        sendMessage( LEDGER_EVENTS_TOPIC, gson.toJson(
                new ApiOperation<Ledger>()
                        .withEventType( EventType.LEDGER_CREATED )
                        .withData( ledger ) ) );
    }

    /**
     * Saves the new ledger account in the datastore and emits a LEDGER_ACCOUNT_CREATED event on successful completion.
     *
     * @param obj deserialized event payload
     * @throws DuplicateKeyException if UUID already exists for ledger
     * @throws NotFoundException     if parent ledger could not be found
     */
    private void handleCreateLedgerAccountEvent( JsonObject obj ) throws DuplicateKeyException, NotFoundException {
        JsonObject req = obj.get( "data" ).getAsJsonObject();
        LedgerAccount ledgerAccount = gson.fromJson( gson.toJson( req ), LedgerAccount.class );
        final String ledgerAccountUuid = ledgerAccount.getUuid();

        // first check that a ledger doesn't already exist with the same UUID
        if ( ledgerService.getLedgerAccount( ledgerAccountUuid ).isPresent() ) {
            throw new DuplicateKeyException( EventType.LEDGER_ACCOUNT_CREATION_FAILED, ledgerAccountUuid, "Ledger account already exists with this UUID" );
        }

        Ledger parentLedger = ledgerService.getLedger( ledgerAccount.getLedger().getUuid() )
                .orElseThrow( () -> new NotFoundException( EventType.LEDGER_ACCOUNT_CREATION_FAILED, ledgerAccountUuid, "No matching ledger found." ) );

        ledgerAccount = ledgerService.createLedgerAccount(
                parentLedger,
                ledgerAccount.getUuid(),
                ledgerAccount.getName(),
                ledgerAccount.getDescription(),
                ledgerAccount.getCurrency() );

        sendMessage( LEDGER_EVENTS_TOPIC, gson.toJson(
                new ApiOperation<LedgerAccount>()
                        .withEventType( EventType.LEDGER_ACCOUNT_CREATED )
                        .withData( ledgerAccount ) ) );
    }

    /**
     * Saves the new ledger transaction in the datastore and emits a LEDGER_TRANSACTION_CREATED event on successful completion.
     *
     * @param obj deserialized event payload
     * @throws DuplicateKeyException if UUID already exists for ledger transaction
     * @throws NotFoundException     if parent ledger account(s) could not be found
     */
    private void handleCreateLedgerTransactionEvent( JsonObject obj ) throws DuplicateKeyException, NotFoundException {
        JsonObject req = obj.get( "data" ).getAsJsonObject();
        LedgerTransaction ledgerTxn = gson.fromJson( gson.toJson( req ), LedgerTransaction.class );
        final String ledgerTxnUuid = ledgerTxn.getUuid();

        // first load all the ledger accounts from the ledger entries
        for ( int i = 0; i < ledgerTxn.getLedgerEntries().size(); i++ ) {
            LedgerEntry entry = ledgerTxn.getLedgerEntries().get( i );
            LedgerAccount acct = ledgerService.getLedgerAccount( entry.getLedgerAccount().getUuid() )
                    .orElseThrow( () -> new NotFoundException( EventType.LEDGER_TRANSACTION_CREATION_FAILED, ledgerTxnUuid,
                            "No matching ledger account found for UUID " + entry.getLedgerAccount().getUuid() ) );
            entry.setLedgerAccount( acct ); // replace with "live" ledger account
            entry.setCreatedDate( OffsetDateTime.now() );
        }

        ledgerTxn = ledgerService.createLedgerTransaction(
                ledgerTxn.getUuid(),
                ledgerTxn.getDescription(),
                ledgerTxn.getLedgerEntries() );

        sendMessage( LEDGER_EVENTS_TOPIC, gson.toJson(
                new ApiOperation<LedgerTransaction>()
                        .withEventType( EventType.LEDGER_TRANSACTION_CREATED )
                        // re-serialize to a normal POJO otherwise we get an exception attempting serialize HibernateProxy
                        .withData( LedgerTransaction.builder()
                                .id( ledgerTxn.getId() )
                                .uuid( ledgerTxn.getUuid() )
                                .description( ledgerTxn.getDescription() )
                                .ledgerEntries( ledgerTxn.getLedgerEntries().stream().map( entry ->
                                        LedgerEntry.builder()
                                                .id( entry.getId() )
                                                .amount( entry.getAmount() )
                                                .direction( entry.getDirection() )
                                                .ledgerAccount( LedgerAccount.builder()
                                                        .id( entry.getLedgerAccount().getId() )
                                                        .uuid( entry.getLedgerAccount().getUuid() )
                                                        .build() )
                                                .createdDate( entry.getCreatedDate() )
                                                .build()
                                ).toList() )
                                .createdDate( ledgerTxn.getCreatedDate() )
                                .build() ) ) );
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
