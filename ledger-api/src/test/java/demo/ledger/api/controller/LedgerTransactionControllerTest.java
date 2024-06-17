package demo.ledger.api.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import demo.ledger.api.model.dto.CreateLedgerTransactionRequest;
import demo.ledger.api.model.dto.CreateLedgerTransactionResponse;
import demo.ledger.api.model.dto.LedgerEntryAccount;
import demo.ledger.api.model.dto.LedgerEntryRequest;
import demo.ledger.api.model.dto.RequestStatus;
import demo.ledger.api.service.LedgerService;
import demo.ledger.model.LedgerTransaction;
import demo.ledger.model.dto.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static demo.ledger.api.config.KafkaTopicConfig.LEDGER_EVENTS_TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest( LedgerTransactionController.class )
public class LedgerTransactionControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private LedgerService ledgerService;

    @MockBean
    private CompletableFuture<SendResult<String, String>> sendResult;

    @MockBean
    private CreateLedgerTransactionResponse ledgerTransactionResponse;

    @MockBean
    private LedgerTransaction ledgerTransaction;

    @Captor
    private ArgumentCaptor<Long> timeoutCaptor;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Autowired
    private Gson gson;

    @Value( value = "${ledger.api.service.timeout.ms}" )
    private long API_SERVICE_TIMEOUT_MS;

    private static final Long ID = 123456L;
    private static final String UUID = "a1d968c1-86fc-4864-a146-f7f8e601fa3f";
    private static final String LEDGER_DESCRIPTION = "Some dodgy transactions";
    private static final String LEDGER_TXN_CREATED_DATE = "2024-04-11T10:15:30+01:00";

    @BeforeEach
    public void setup() {
        when( ledgerTransaction.getId() ).thenReturn( ID );
        when( ledgerTransaction.getUuid() ).thenReturn( UUID );
        when( ledgerTransaction.getDescription() ).thenReturn( LEDGER_DESCRIPTION );
        when( ledgerTransaction.getCreatedDate() ).thenReturn( OffsetDateTime.parse( LEDGER_TXN_CREATED_DATE ) );
    }

    @Test
    public void testCreateLedgerTransaction() throws Exception {

        // setup
        when( ledgerTransactionResponse.getStatus() ).thenReturn( RequestStatus.completed );
        when( ledgerTransactionResponse.getErrors() ).thenReturn( null );
        when( ledgerTransactionResponse.getLedgerTransaction() ).thenReturn( ledgerTransaction );
        when( ledgerService.waitForLedgerTransactionCreation( eq( UUID ), timeoutCaptor.capture() ) )
                .thenReturn( ledgerTransactionResponse );
        when( kafkaTemplate.send( stringCaptor.capture(), stringCaptor.capture(), stringCaptor.capture() ) )
                .thenReturn( sendResult );

        String json = gson.toJson( CreateLedgerTransactionRequest.builder()
                .uuid( UUID )
                .description( LEDGER_DESCRIPTION )
                .ledgerEntries( Arrays.asList(
                        LedgerEntryRequest.builder()
                                .ledgerAccount( LedgerEntryAccount.builder()
                                        .uuid( UUID )
                                        .lockVersion( 21L )
                                        .build() )
                                .direction( "debit" )
                                .amount( new BigInteger( "100" ) )
                                .build(),
                        LedgerEntryRequest.builder()
                                .ledgerAccount( LedgerEntryAccount.builder()
                                        .uuid( UUID )
                                        .lockVersion( 32L )
                                        .build() )
                                .direction( "credit" )
                                .amount( new BigInteger( "100" ) )
                                .build() ) )
                .build() );

        // execute & verify
        mvc.perform( MockMvcRequestBuilders
                        .post( "/api/ledger_transaction" )
                        .content( json )
                        .contentType( MediaType.APPLICATION_JSON )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andDo( print() )
                .andExpect( status().isAccepted() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.status", is( RequestStatus.completed.name() ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.error" ).doesNotExist() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors" ).doesNotExist() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledgerTransaction" ).exists() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledgerTransaction.id", is( ID ), Long.class ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledgerTransaction.uuid", is( UUID ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledgerTransaction.description", is( LEDGER_DESCRIPTION ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledgerTransaction.createdDate", is( LEDGER_TXN_CREATED_DATE ) ) );

        assertThat( "call to ledgerService.waitForLedgerTransactionCreation()", timeoutCaptor.getValue(), is( API_SERVICE_TIMEOUT_MS ) );
        assertThat( "kafka topic", stringCaptor.getAllValues().get( 0 ), is( LEDGER_EVENTS_TOPIC ) );
        assertThat( "kafka key", stringCaptor.getAllValues().get( 1 ), is( "USERID" ) ); // TODO: placeholder

        JsonObject kafkaPayload = gson.fromJson( stringCaptor.getAllValues().get( 2 ), JsonObject.class );
        assertThat( kafkaPayload.get( "eventType" ).getAsString(), is( EventType.CREATE_LEDGER_TRANSACTION.name() ) );
        JsonObject dataObj = kafkaPayload.get( "data" ).getAsJsonObject();
        assertThat( dataObj.get( "uuid" ).getAsString(), is( UUID ) );
        assertThat( dataObj.get( "description" ).getAsString(), is( LEDGER_DESCRIPTION ) );
    }

    @Test
    public void testCreateLedgerTransactionMissingLockVersions() throws Exception {

        // setup
        String json = gson.toJson( CreateLedgerTransactionRequest.builder()
                .uuid( UUID )
                .description( LEDGER_DESCRIPTION )
                .ledgerEntries( Arrays.asList(
                        LedgerEntryRequest.builder()
                                .ledgerAccount( LedgerEntryAccount.builder()
                                        .uuid( UUID )
                                        .build() )
                                .direction( "debit" )
                                .amount( new BigInteger( "100" ) )
                                .build(),
                        LedgerEntryRequest.builder()
                                .ledgerAccount( LedgerEntryAccount.builder()
                                        .uuid( UUID )
                                        .build() )
                                .direction( "credit" )
                                .amount( new BigInteger( "100" ) )
                                .build() ) )
                .build() );

        // execute & verify
        mvc.perform( MockMvcRequestBuilders
                        .post( "/api/ledger_transaction" )
                        .content( json )
                        .contentType( MediaType.APPLICATION_JSON )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andDo( print() )
                .andExpect( status().isBadRequest() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.status", is( RequestStatus.failed.name() ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.error" ).doesNotExist() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors" ).exists() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors.['ledgerEntries[0].ledgerAccount.lockVersion']", is( "must not be null" ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors.['ledgerEntries[1].ledgerAccount.lockVersion']", is( "must not be null" ) ) );

        verify( ledgerService, never() ).waitForLedgerTransactionCreation( anyString(), anyLong() );
        verify( kafkaTemplate, never() ).send( anyString(), anyString(), anyString() );
    }

    @Test
    public void testCreateLedgerWithInvalidUUID() throws Exception {

        // execute & verify
        mvc.perform( MockMvcRequestBuilders
                        .post( "/api/ledger_transaction" )
                        .content( gson.toJson( CreateLedgerTransactionRequest.builder()
                                .uuid( "INVALID-UUID" )
                                .description( LEDGER_DESCRIPTION )
                                .build() ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andDo( print() )
                .andExpect( status().isBadRequest() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.status", is( RequestStatus.failed.name() ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.error" ).doesNotExist() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors" ).exists() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors.uuid", is( "Invalid UUID (only lowercase characters allowed)" ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledger" ).doesNotExist() );

        verify( ledgerService, never() ).waitForLedgerCreation( anyString(), anyLong() );
        verify( kafkaTemplate, never() ).send( anyString(), anyString(), anyString() );
    }

    @Test
    public void testCreateLedgerWithEmptyRequest() throws Exception {

        // execute & verify
        mvc.perform( MockMvcRequestBuilders
                        .post( "/api/ledger_transaction" )
                        .content( "" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andDo( print() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.status", is( RequestStatus.failed.name() ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors" ).doesNotExist() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.error", is( "Request unreadable" ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledger" ).doesNotExist() );

        verify( ledgerService, never() ).waitForLedgerCreation( anyString(), anyLong() );
        verify( kafkaTemplate, never() ).send( anyString(), anyString(), anyString() );
    }

    @Test
    public void testCreateLedgerWithEmptyObject() throws Exception {

        // execute & verify
        mvc.perform( MockMvcRequestBuilders
                        .post( "/api/ledger_transaction" )
                        .content( "{}" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andDo( print() )
                .andExpect( status().isBadRequest() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.status", is( RequestStatus.failed.name() ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.error" ).doesNotExist() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors" ).exists() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors.uuid", is( "must not be blank" ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors.ledgerEntries", is( "must not be empty" ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledgerTransaction" ).doesNotExist() );

        verify( ledgerService, never() ).waitForLedgerCreation( anyString(), anyLong() );
        verify( kafkaTemplate, never() ).send( anyString(), anyString(), anyString() );
    }

    @Test
    public void testCreateLedgerMissingLedgerAccount() throws Exception {

        // execute & verify
        mvc.perform( MockMvcRequestBuilders
                        .post( "/api/ledger_transaction" )
                        .content( gson.toJson( CreateLedgerTransactionRequest.builder()
                                .uuid( UUID )
                                .description( LEDGER_DESCRIPTION )
                                .ledgerEntries( Arrays.asList(
                                        LedgerEntryRequest.builder()
                                                // missing ledgerAccount
                                                .direction( "debit" )
                                                .amount( new BigInteger( "100" ) )
                                                .build() ) )
                                .build() ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andDo( print() )
                .andExpect( status().isBadRequest() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.status", is( RequestStatus.failed.name() ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.error" ).doesNotExist() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors" ).exists() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors.['ledgerEntries[0].ledgerAccount']", is( "must not be null" ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledger" ).doesNotExist() );

        verify( ledgerService, never() ).waitForLedgerCreation( anyString(), anyLong() );
        verify( kafkaTemplate, never() ).send( anyString(), anyString(), anyString() );
    }

    @Test
    public void testCreateLedgerInvalidLedgerAccountUUID() throws Exception {

        // execute & verify
        mvc.perform( MockMvcRequestBuilders
                        .post( "/api/ledger_transaction" )
                        .content( gson.toJson( CreateLedgerTransactionRequest.builder()
                                .uuid( UUID )
                                .description( LEDGER_DESCRIPTION )
                                .ledgerEntries( Arrays.asList(
                                        LedgerEntryRequest.builder()
                                                .ledgerAccount( LedgerEntryAccount.builder()
                                                        .uuid( "ABCDEFGHIJKLMNOP" ).build() )
                                                .direction( "debit" )
                                                .amount( new BigInteger( "100" ) )
                                                .build() ) )
                                .build() ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andDo( print() )
                .andExpect( status().isBadRequest() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.status", is( RequestStatus.failed.name() ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.error" ).doesNotExist() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors" ).exists() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors.['ledgerEntries[0].ledgerAccount.uuid']",
                        is( "Invalid UUID (only lowercase characters allowed)" ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledger" ).doesNotExist() );

        verify( ledgerService, never() ).waitForLedgerCreation( anyString(), anyLong() );
        verify( kafkaTemplate, never() ).send( anyString(), anyString(), anyString() );
    }

    @Test
    public void testFindByUUID() throws Exception {
        // setup
        when( ledgerService.findLedgerTransactionByUuid( stringCaptor.capture() ) )
                .thenReturn( Optional.of( ledgerTransaction ) );

        // execute & verify
        mvc.perform( MockMvcRequestBuilders.get( "/api/ledger_transaction/" + UUID )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.id", is( ID ), Long.class ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.uuid", is( UUID ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.description", is( LEDGER_DESCRIPTION ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.createdDate", is( LEDGER_TXN_CREATED_DATE ) ) );
    }

    @Test
    public void testFindByUUIDNotFound() throws Exception {
        // setup
        when( ledgerService.findLedgerByUuid( stringCaptor.capture() ) )
                .thenReturn( Optional.ofNullable( null ) );

        // execute & verify
        mvc.perform( MockMvcRequestBuilders.get( "/api/ledger_transaction/" + UUID )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isNotFound() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.status", is( RequestStatus.failed.name() ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.error", is( "No matching ledger transaction found." ) ) );
    }

    @Test
    public void testFindByInvalidUUID() throws Exception {

        // execute & verify
        mvc.perform( MockMvcRequestBuilders.get( "/api/ledger_transaction/abcdefghijklmnop" )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isBadRequest() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.status", is( RequestStatus.failed.name() ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors" ).exists() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors.uuid", is( "Invalid UUID (only lowercase characters allowed)" ) ) );
    }
}
