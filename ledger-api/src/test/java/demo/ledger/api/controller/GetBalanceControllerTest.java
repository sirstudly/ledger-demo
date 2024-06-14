package demo.ledger.api.controller;

import demo.ledger.api.model.dto.GetBalanceResponse;
import demo.ledger.api.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigInteger;
import java.time.OffsetDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest( GetBalanceController.class )
public class GetBalanceControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private LedgerService ledgerService;

    @MockBean
    private GetBalanceResponse response;

    @Captor
    private ArgumentCaptor<OffsetDateTime> dateTimeCaptor;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    private static final String UUID = "a1d968c1-86fc-4864-a146-f7f8e601fa3f";
    private static final String ACCOUNT_NAME = "My first ledger";
    private static final String ACCOUNT_DESCRIPTION = "Some dodgy transactions";
    private static final String QUERY_DATE = "2024-04-11T10:24:35+02:00";
    private static final BigInteger TOTAL_CREDITS = new BigInteger( "12345" );
    private static final BigInteger TOTAL_DEBITS = new BigInteger( "54321" );

    @BeforeEach
    public void setup() throws Exception {
        when( ledgerService.fetchLedgerAccountBalance( stringCaptor.capture(), dateTimeCaptor.capture() ) ).thenReturn( response );
        when( response.getUuid() ).thenReturn( UUID );
        when( response.getName() ).thenReturn( ACCOUNT_NAME );
        when( response.getDescription() ).thenReturn( ACCOUNT_DESCRIPTION );
        when( response.getTotalCredits() ).thenReturn( TOTAL_CREDITS );
        when( response.getTotalDebits() ).thenReturn( TOTAL_DEBITS );
        when( response.getTimestamp() ).thenReturn( OffsetDateTime.parse( QUERY_DATE ) );
    }

    @Test
    public void testGetBalance() throws Exception {

        // execute & verify
        mvc.perform( MockMvcRequestBuilders.get( "/api/get_balance" )
                        .param( "uuid", UUID )
                        .param( "timestamp", QUERY_DATE )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.uuid", is( UUID ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.name", is( ACCOUNT_NAME ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.description", is( ACCOUNT_DESCRIPTION ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.timestamp", is( QUERY_DATE ) ) );
        assertThat( stringCaptor.getValue(), is( UUID ) );
        assertThat( dateTimeCaptor.getValue(), is( OffsetDateTime.parse( QUERY_DATE ) ) );
    }

    @Test
    public void testGetBalanceWithoutTimestamp() throws Exception {

        // execute & verify
        mvc.perform( MockMvcRequestBuilders.get( "/api/get_balance" )
                        .param( "uuid", UUID )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.uuid", is( UUID ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.name", is( ACCOUNT_NAME ) ) )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.description", is( ACCOUNT_DESCRIPTION ) ) );
        assertThat( stringCaptor.getValue(), is( UUID ) );
    }

    @Test
    public void testGetBalanceWithoutUuid() throws Exception {

        // execute & verify
        mvc.perform( MockMvcRequestBuilders.get( "/api/get_balance" )
                        .param( "timestamp", QUERY_DATE )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isBadRequest() )
                .andExpect( MockMvcResultMatchers.jsonPath( "$.errors.uuid",
                        is( "Required request parameter 'uuid' for method parameter type String is not present" ) ) );
    }
}
