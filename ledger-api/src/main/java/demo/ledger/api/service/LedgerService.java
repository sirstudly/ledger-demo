package demo.ledger.api.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import demo.ledger.api.model.dto.CreateLedgerResponse;
import demo.ledger.model.LedgerAccount;
import demo.ledger.api.model.dto.RequestStatus;
import demo.ledger.api.repository.LedgerAccountRepository;
import demo.ledger.api.repository.LedgerRepository;
import demo.ledger.model.Ledger;
import demo.ledger.model.dto.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class LedgerService {
    private static final Logger LOGGER = LoggerFactory.getLogger( LedgerService.class );
    private final LedgerRepository ledgerRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final Gson gson;
    private final ConcurrentMap<String, CompletableFuture<CreateLedgerResponse>> pendingLedgers = new ConcurrentHashMap<>();

    @Autowired
    public LedgerService( LedgerRepository ledgerRepository, LedgerAccountRepository ledgerAccountRepository, Gson gson ) {
        this.ledgerRepository = ledgerRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.gson = gson;
    }

    @KafkaListener( groupId = "${spring.kafka.groupid}", topics = {"ledger-events"},
            containerFactory = "kafkaListenerContainerFactory" )
    public void listenForCompletionEvents( @Payload String in,
                                        @Header( KafkaHeaders.RECEIVED_TOPIC ) String topic,
                                        @Header( KafkaHeaders.RECEIVED_PARTITION ) long partition,
                                        @Header( KafkaHeaders.OFFSET ) long offset,
                                        @Header( KafkaHeaders.RECEIVED_TIMESTAMP ) long ts ) {
        OffsetDateTime timestamp = OffsetDateTime.of( LocalDateTime.ofEpochSecond( ts / 1000, 0, ZoneOffset.UTC ), ZoneOffset.UTC );
        LOGGER.info( "Received topic={}, message=[{}], partition={}, offset={}, timestamp={}", topic, in, partition, offset, timestamp );
        JsonObject obj = gson.fromJson( in, JsonObject.class );
        if ( obj.has( "eventType" ) ) {
            String eventType = obj.get( "eventType" ).getAsString();

            // notify any requests that are waiting that the ledger has been created successfully
            if ( EventType.LEDGER_CREATED.name().equals( eventType ) ) {
                JsonObject req = obj.get( "data" ).getAsJsonObject();
                Ledger ledger = gson.fromJson( gson.toJson( req ), Ledger.class );
                completeLedgerCreation( new CreateLedgerResponse( RequestStatus.completed ).withLedger( ledger ) );
            }
        }
    }

    /**
     * Wait for the ledger identified by the given UUID to be created for a given amount of time.
     * If the request does not come back within the allotted time, a status response of "pending" is returned.
     * @param ledgerUuid the unique identifier for the ledger
     * @param timeoutMillis the amount of time to wait before returning a "pending" response
     * @return non-null response
     */
    public CreateLedgerResponse waitForLedgerCreation( String ledgerUuid, long timeoutMillis ) {
        CompletableFuture<CreateLedgerResponse> future = new CompletableFuture<>();
        pendingLedgers.put( ledgerUuid, future );

        try {
            return future.get( timeoutMillis, TimeUnit.MILLISECONDS );
        }
        catch ( TimeoutException | InterruptedException e ) {
            // Return pending status if timeout occurs
            return new CreateLedgerResponse( RequestStatus.pending );
        }
        catch( ExecutionException e ) {
            return new CreateLedgerResponse( RequestStatus.failed ).withError( e.getMessage() );
        }
    }

    /**
     * Notify any pending requests that the given ledger has completed.
     * @param response completed ledger response
     */
    private void completeLedgerCreation( CreateLedgerResponse response ) {
        CompletableFuture<CreateLedgerResponse> future = pendingLedgers.remove( response.getLedger().getUuid() );
        if ( future != null ) {
            future.complete( response );
        }
    }

    public Optional<Ledger> findLedgerByUuid( String uuid ) {
        // https://docs.spring.io/spring-data/jpa/reference/repositories/query-by-example.html
        return ledgerRepository.findOne( Example.of( Ledger.builder().uuid( uuid.toLowerCase() ).build() ) );
    }

    public Optional<LedgerAccount> findLedgerAccountByUuid( String uuid ) {
        return ledgerAccountRepository.findOne( Example.of( new LedgerAccount().withUuid( uuid.toLowerCase() ) ) );
    }
}
