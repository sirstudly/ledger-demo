package demo.ledger.api.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import demo.ledger.api.model.dto.CreateLedgerRequest;
import demo.ledger.api.model.dto.CreateLedgerResponse;
import demo.ledger.api.model.dto.RequestStatus;
import demo.ledger.api.service.LedgerService;
import demo.ledger.model.Ledger;
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

import java.time.OffsetDateTime;
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

@WebMvcTest( LedgerController.class )
public class LedgerControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private LedgerService ledgerService;

    @MockBean
    private CompletableFuture<SendResult<String, String>> sendResult;

    @MockBean
    private CreateLedgerResponse ledgerResponse;

    @MockBean
    private Ledger ledger;

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
    private static final String LEDGER_NAME = "My first ledger";
    private static final String LEDGER_DESCRIPTION = "Some dodgy transactions";
    private static final String LEDGER_CREATED_DATE = "2024-04-11T10:15:30+01:00";
    private static final String LEDGER_LAST_UPDATED_DATE = "2024-04-11T10:24:35+02:00";

    @BeforeEach
    public void setup() {
        when( ledger.getId() ).thenReturn( ID );
        when( ledger.getUuid() ).thenReturn( UUID );
        when( ledger.getName() ).thenReturn( LEDGER_NAME );
        when( ledger.getDescription() ).thenReturn( LEDGER_DESCRIPTION );
        when( ledger.getCreatedDate() ).thenReturn( OffsetDateTime.parse( LEDGER_CREATED_DATE ) );
        when( ledger.getLastUpdatedDate() ).thenReturn( OffsetDateTime.parse( LEDGER_LAST_UPDATED_DATE ) );
    }

    @Test
    public void testCreateLedger() throws Exception {

        // setup
        when( ledgerResponse.getStatus() ).thenReturn( RequestStatus.completed );
        when( ledgerResponse.getErrors() ).thenReturn( null );
        when( ledgerResponse.getLedger() ).thenReturn( ledger );
        when( ledgerService.waitForLedgerCreation( eq( UUID ), timeoutCaptor.capture() ) )
                .thenReturn( ledgerResponse );
        when( kafkaTemplate.send( stringCaptor.capture(), stringCaptor.capture(), stringCaptor.capture() ) )
                .thenReturn( sendResult );

        // execute & verify
        mvc.perform( MockMvcRequestBuilders
                        .post( "/api/ledger" )
                        .content( gson.toJson( CreateLedgerRequest.builder()
                                .uuid( UUID )
                                .name( LEDGER_NAME )
                                .description( LEDGER_DESCRIPTION )
                                .build() ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andDo( print() )
                .andExpect( status().isAccepted() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.status", is( RequestStatus.completed.name() ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.error" ).doesNotExist() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors" ).doesNotExist() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledger" ).exists() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledger.id", is( ID ), Long.class ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledger.uuid", is( UUID ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledger.name", is( LEDGER_NAME ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledger.description", is( LEDGER_DESCRIPTION ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledger.createdDate", is( LEDGER_CREATED_DATE ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledger.lastUpdatedDate", is( LEDGER_LAST_UPDATED_DATE ) ) );

        assertThat( "call to ledgerService.waitForLedgerCreation()", timeoutCaptor.getValue(), is( API_SERVICE_TIMEOUT_MS ) );
        assertThat( "kafka topic", stringCaptor.getAllValues().get( 0 ), is( LEDGER_EVENTS_TOPIC ) );
        assertThat( "kafka key", stringCaptor.getAllValues().get( 1 ), is( "USERID" ) ); // TODO: placeholder

        JsonObject kafkaPayload = gson.fromJson( stringCaptor.getAllValues().get( 2 ), JsonObject.class );
        assertThat( kafkaPayload.get( "eventType" ).getAsString(), is( EventType.CREATE_LEDGER.name() ) );
        JsonObject dataObj = kafkaPayload.get( "data" ).getAsJsonObject();
        assertThat( dataObj.get( "uuid" ).getAsString(), is( UUID ) );
        assertThat( dataObj.get( "name" ).getAsString(), is( LEDGER_NAME ) );
        assertThat( dataObj.get( "description" ).getAsString(), is( LEDGER_DESCRIPTION ) );
    }

    @Test
    public void testCreateLedgerWithInvalidUUID() throws Exception {

        // execute & verify
        mvc.perform( MockMvcRequestBuilders
                        .post( "/api/ledger" )
                        .content( gson.toJson( CreateLedgerRequest.builder()
                                .uuid( "INVALID-UUID" )
                                .name( LEDGER_NAME )
                                .description( LEDGER_DESCRIPTION )
                                .build() ) )
                        .contentType( MediaType.APPLICATION_JSON )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andDo( print() )
                .andExpect( status().isBadRequest() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.status", is( RequestStatus.failed.name() ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.error" ).doesNotExist() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors").exists() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors.uuid", is( "Invalid UUID (only lowercase characters allowed)" ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledger" ).doesNotExist() );

        verify( ledgerService, never() ).waitForLedgerCreation( anyString(), anyLong() );
        verify( kafkaTemplate, never() ).send( anyString(), anyString(), anyString() );
    }

    @Test
    public void testCreateLedgerWithEmptyRequest() throws Exception {

        // execute & verify
        mvc.perform( MockMvcRequestBuilders
                        .post( "/api/ledger" )
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
                        .post( "/api/ledger" )
                        .content( "{}" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andDo( print() )
                .andExpect( status().isBadRequest() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.status", is( RequestStatus.failed.name() ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.error" ).doesNotExist() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors" ).exists() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors.uuid", is( "must not be blank" ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors.name", is( "must not be blank" ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.ledger" ).doesNotExist() );

        verify( ledgerService, never() ).waitForLedgerCreation( anyString(), anyLong() );
        verify( kafkaTemplate, never() ).send( anyString(), anyString(), anyString() );
    }

    @Test
    public void testFindByUUID() throws Exception {
        // setup
        when( ledgerService.findLedgerByUuid( stringCaptor.capture() ) )
                .thenReturn( Optional.of( ledger ) );

        // execute & verify
        mvc.perform( MockMvcRequestBuilders.get( "/api/ledger/" + UUID )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.id", is( ID ), Long.class ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.uuid", is( UUID ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.name", is( LEDGER_NAME ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.description", is( LEDGER_DESCRIPTION ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.createdDate", is( LEDGER_CREATED_DATE ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.lastUpdatedDate", is( LEDGER_LAST_UPDATED_DATE ) ) );
    }

    @Test
    public void testFindByUUIDNotFound() throws Exception {
        // setup
        when( ledgerService.findLedgerByUuid( stringCaptor.capture() ) )
                .thenReturn( Optional.ofNullable( null ) );

        // execute & verify
        mvc.perform( MockMvcRequestBuilders.get( "/api/ledger/" + UUID )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isNotFound() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.status", is( RequestStatus.failed.name() ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.error", is( "No matching ledger found." ) ) );
    }

    @Test
    public void testFindByInvalidUUID() throws Exception {

        // execute & verify
        mvc.perform( MockMvcRequestBuilders.get( "/api/ledger/abcdefghijklmnop" )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isBadRequest() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.status", is( RequestStatus.failed.name() ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors").exists() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors.uuid", is( "Invalid UUID (only lowercase characters allowed)" ) ) );
    }
}
