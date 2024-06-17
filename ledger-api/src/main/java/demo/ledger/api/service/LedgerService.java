package demo.ledger.api.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import demo.ledger.api.model.dto.CreateLedgerAccountResponse;
import demo.ledger.api.model.dto.CreateLedgerResponse;
import demo.ledger.api.model.dto.CreateLedgerTransactionResponse;
import demo.ledger.api.model.dto.GetBalanceResponse;
import demo.ledger.api.model.dto.RequestStatus;
import demo.ledger.api.repository.LedgerAccountBalance;
import demo.ledger.api.repository.LedgerAccountRepository;
import demo.ledger.api.repository.LedgerEntryRepository;
import demo.ledger.api.repository.LedgerRepository;
import demo.ledger.api.repository.LedgerTransactionRepository;
import demo.ledger.model.Ledger;
import demo.ledger.model.LedgerAccount;
import demo.ledger.model.LedgerTransaction;
import demo.ledger.model.dto.EventType;
import demo.ledger.model.dto.FailedResponse;
import demo.ledger.model.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final Gson gson;
    private final ConcurrentMap<String, CompletableFuture<CreateLedgerResponse>> pendingLedgers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CompletableFuture<CreateLedgerAccountResponse>> pendingLedgerAccounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CompletableFuture<CreateLedgerTransactionResponse>> pendingLedgerTransactions = new ConcurrentHashMap<>();

    @Autowired
    public LedgerService( LedgerRepository ledgerRepository, LedgerAccountRepository ledgerAccountRepository,
                          LedgerTransactionRepository ledgerTransactionRepository, LedgerEntryRepository ledgerEntryRepository, Gson gson ) {
        this.ledgerRepository = ledgerRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.ledgerTransactionRepository = ledgerTransactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
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
            else if ( EventType.LEDGER_CREATION_FAILED.name().equals( eventType ) ) {
                JsonObject req = obj.get( "data" ).getAsJsonObject();
                FailedResponse failedResponse = gson.fromJson( gson.toJson( req ), FailedResponse.class );
                completeLedgerCreation( new CreateLedgerResponse( RequestStatus.failed )
                        .withLedger( Ledger.builder().uuid( failedResponse.getUuid() ).build() )
                        .withError( failedResponse.getError() ) );
            }
            else if ( EventType.LEDGER_ACCOUNT_CREATED.name().equals( eventType ) ) {
                JsonObject req = obj.get( "data" ).getAsJsonObject();
                LedgerAccount ledgerAccount = gson.fromJson( gson.toJson( req ), LedgerAccount.class );
                completeLedgerAccountCreation( new CreateLedgerAccountResponse( RequestStatus.completed ).withLedgerAccount( ledgerAccount ) );
            }
            else if ( EventType.LEDGER_ACCOUNT_CREATION_FAILED.name().equals( eventType ) ) {
                JsonObject req = obj.get( "data" ).getAsJsonObject();
                FailedResponse failedResponse = gson.fromJson( gson.toJson( req ), FailedResponse.class );
                completeLedgerAccountCreation( new CreateLedgerAccountResponse( RequestStatus.failed )
                        .withError( failedResponse.getError() )
                        .withLedgerAccount( LedgerAccount.builder().uuid( failedResponse.getUuid() ).build() ) );
            }
            else if ( EventType.LEDGER_TRANSACTION_CREATED.name().equals( eventType ) ) {
                JsonObject req = obj.get( "data" ).getAsJsonObject();
                LedgerTransaction ledgerTransaction = gson.fromJson( gson.toJson( req ), LedgerTransaction.class );
                completeLedgerTransactionCreation( new CreateLedgerTransactionResponse( RequestStatus.completed ).withLedgerTransaction( ledgerTransaction ) );
            }
            else if ( EventType.LEDGER_TRANSACTION_CREATION_FAILED.name().equals( eventType ) ) {
                JsonObject req = obj.get( "data" ).getAsJsonObject();
                FailedResponse failedResponse = gson.fromJson( gson.toJson( req ), FailedResponse.class );
                completeLedgerTransactionCreation( new CreateLedgerTransactionResponse( RequestStatus.failed )
                        .withError( failedResponse.getError() )
                        .withLedgerTransaction( LedgerTransaction.builder().uuid( failedResponse.getUuid() ).build() ) );
            }
        }
    }

    /**
     * Wait for the ledger identified by the given UUID to be created for a given amount of time.
     * If the request does not come back within the allotted time, a status response of "pending" is returned.
     *
     * @param ledgerUuid    the unique identifier for the ledger
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
        catch ( ExecutionException e ) {
            return new CreateLedgerResponse( RequestStatus.failed ).withError( e.getMessage() );
        }
    }

    /**
     * Wait for the ledger account identified by the given UUID to be created for a given amount of time.
     * If the request does not come back within the allotted time, a status response of "pending" is returned.
     *
     * @param ledgerAccountUuid the unique identifier for the ledger account
     * @param timeoutMillis     the amount of time to wait before returning a "pending" response
     * @return non-null response
     */
    public CreateLedgerAccountResponse waitForLedgerAccountCreation( String ledgerAccountUuid, long timeoutMillis ) {
        CompletableFuture<CreateLedgerAccountResponse> future = new CompletableFuture<>();
        pendingLedgerAccounts.put( ledgerAccountUuid, future );

        try {
            return future.get( timeoutMillis, TimeUnit.MILLISECONDS );
        }
        catch ( TimeoutException | InterruptedException e ) {
            // Return pending status if timeout occurs
            return new CreateLedgerAccountResponse( RequestStatus.pending );
        }
        catch ( ExecutionException e ) {
            return new CreateLedgerAccountResponse( RequestStatus.failed ).withError( e.getMessage() );
        }
    }

    /**
     * Wait for the ledger transaction identified by the given UUID to be created for a given amount of time.
     * If the request does not come back within the allotted time, a status response of "pending" is returned.
     *
     * @param ledgerTransactionUuid the unique identifier for the ledger account
     * @param timeoutMillis         the amount of time to wait before returning a "pending" response
     * @return non-null response
     */
    public CreateLedgerTransactionResponse waitForLedgerTransactionCreation( String ledgerTransactionUuid, long timeoutMillis ) {
        CompletableFuture<CreateLedgerTransactionResponse> future = new CompletableFuture<>();
        pendingLedgerTransactions.put( ledgerTransactionUuid, future );

        try {
            return future.get( timeoutMillis, TimeUnit.MILLISECONDS );
        }
        catch ( TimeoutException | InterruptedException e ) {
            // Return pending status if timeout occurs
            return new CreateLedgerTransactionResponse( RequestStatus.pending );
        }
        catch ( ExecutionException e ) {
            return new CreateLedgerTransactionResponse( RequestStatus.failed ).withError( e.getMessage() );
        }
    }

    /**
     * Notify any pending requests that the given ledger has completed.
     *
     * @param response completed ledger response
     */
    private void completeLedgerCreation( CreateLedgerResponse response ) {
        CompletableFuture<CreateLedgerResponse> future = pendingLedgers.remove( response.getLedger().getUuid() );
        if ( future != null ) {
            future.complete( response );
        }
    }

    /**
     * Notify any pending requests that the given ledger account has completed.
     *
     * @param response completed ledger account response
     */
    private void completeLedgerAccountCreation( CreateLedgerAccountResponse response ) {
        CompletableFuture<CreateLedgerAccountResponse> future = pendingLedgerAccounts.remove( response.getLedgerAccount().getUuid() );
        if ( future != null ) {
            future.complete( response );
        }
    }

    /**
     * Notify any pending requests that the given ledger transaction has completed.
     *
     * @param response completed ledger transaction response
     */
    private void completeLedgerTransactionCreation( CreateLedgerTransactionResponse response ) {
        CompletableFuture<CreateLedgerTransactionResponse> future = pendingLedgerTransactions.remove( response.getLedgerTransaction().getUuid() );
        if ( future != null ) {
            future.complete( response );
        }
    }

    // https://docs.spring.io/spring-data/jpa/reference/repositories/query-by-example.html
    public Optional<Ledger> findLedgerByUuid( String uuid ) {
        return ledgerRepository.findOne( Example.of( Ledger.builder().uuid( uuid ).build() ) );
    }

    public Optional<LedgerAccount> findLedgerAccountByUuid( String uuid ) {
        return ledgerAccountRepository.findOne( Example.of( LedgerAccount.builder().uuid( uuid ).build() ) );
    }

    public Optional<LedgerTransaction> findLedgerTransactionByUuid( String uuid ) {
        return ledgerTransactionRepository.findOne( Example.of( LedgerTransaction.builder().uuid( uuid ).build() ) );
    }

    /**
     * Fetches a user's account balance up to a particular date/time.
     *
     * @param uuid          unique ID of the ledger account to query
     * @param untilDateTime find all transactions up until this time (optional)
     * @return balance response
     * @throws NotFoundException if UUID does not match any account
     */
    public GetBalanceResponse fetchLedgerAccountBalance( String uuid, OffsetDateTime untilDateTime ) throws NotFoundException {

        LedgerAccount ledgerAccount = findLedgerAccountByUuid( uuid )
                .orElseThrow( () -> new NotFoundException( "No matching ledger account found" ) );
        List<LedgerAccountBalance> results = untilDateTime == null ?
                ledgerEntryRepository.getBalances( uuid ) : ledgerEntryRepository.getBalances( uuid, untilDateTime );

        LedgerAccountBalance result = results.get( 0 );
        return GetBalanceResponse.builder()
                .uuid( uuid )
                .lockVersion( result.getLedgerAccountLockVersion() )
                .name( ledgerAccount.getName() )
                .description( ledgerAccount.getDescription() )
                .totalCredits( result.getTotalCredits() )
                .totalDebits( result.getTotalDebits() )
                .timestamp( untilDateTime )
                .build();
    }
}
