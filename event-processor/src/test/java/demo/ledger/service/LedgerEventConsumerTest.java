package demo.ledger.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import demo.ledger.config.OffsetDateTimeConverter;
import demo.ledger.model.Ledger;
import demo.ledger.model.dto.EventType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

import static demo.ledger.config.KafkaProducerConfig.FAILED_PROCESSING_TOPIC;
import static demo.ledger.config.KafkaProducerConfig.LEDGER_EVENTS_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith( SpringRunner.class )
public class LedgerEventConsumerTest {

    @TestConfiguration
    static class EmployeeServiceImplTestContextConfiguration {
        @Bean
        public LedgerEventConsumer getLedgerEventConsumer( LedgerService ledgerService, Gson gson, KafkaTemplate<String, String> kafkaTemplate ) {
            return new LedgerEventConsumer( ledgerService, gson, kafkaTemplate );
        }

        @Bean
        public Gson getGson() {
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter( OffsetDateTime.class, new OffsetDateTimeConverter() );
            builder.setPrettyPrinting();
            return builder.create();
        }
    }

    @Autowired
    private LedgerEventConsumer ledgerEventConsumer; // object under test

    @Autowired
    private Gson gson;

    @MockBean
    private LedgerService ledgerService;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private CompletableFuture<SendResult<String, String>> sendResult;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    private static final String UUID = "a1d968c1-86fc-4864-a146-f7f8e601fa3f";
    private static final String LEDGER_NAME = "My first ledger";
    private static final String LEDGER_DESCRIPTION = "Some dodgy transactions";

    @Before
    public void setup() {
        when( ledgerService.createLedger( UUID, LEDGER_NAME, LEDGER_DESCRIPTION ) )
                .thenReturn( Ledger.builder()
                        .uuid( UUID )
                        .name( LEDGER_NAME )
                        .description( LEDGER_DESCRIPTION )
                        .build() );

        when( kafkaTemplate.send( anyString(), anyString() ) ).thenReturn( sendResult );
    }

    @Test
    public void testConsumeLedgerOperationSuccessful() {
        JsonObject event = new JsonObject();
        event.addProperty( "eventType", EventType.CREATE_LEDGER.name() );
        JsonObject data = new JsonObject();
        data.addProperty( "uuid", UUID );
        data.addProperty( "name", LEDGER_NAME );
        data.addProperty( "description", LEDGER_DESCRIPTION );
        event.add( "data", data );
        ledgerEventConsumer.consumeLedgerOperation( gson.toJson( event ), "test-topic", 0, 0, System.currentTimeMillis() );

        verify( ledgerService ).createLedger( UUID, LEDGER_NAME, LEDGER_DESCRIPTION );
        verify( kafkaTemplate ).send( eq( LEDGER_EVENTS_TOPIC ), stringCaptor.capture() );

        event.addProperty( "eventType", EventType.LEDGER_CREATED.name() );
        assertThat( stringCaptor.getValue() ).isEqualTo( gson.toJson( event ) );
    }

    @Test
    public void testConsumeLedgerOperationWithUnsupportedEventType() {
        JsonObject event = new JsonObject();
        event.addProperty( "eventType", "FOO-EVENT" );
        ledgerEventConsumer.consumeLedgerOperation( gson.toJson( event ), "test-topic", 0, 0, System.currentTimeMillis() );

        verify( ledgerService, never() ).createLedger( anyString(), anyString(), anyString() );
        verify( kafkaTemplate, never() ).send( anyString(), anyString() );
    }

    @Test
    public void testConsumeLedgerOperationFailureWithInvalidJson() {
        ledgerEventConsumer.consumeLedgerOperation( "not a JSON string", "test-topic", 1, 2, 1717902931964L );

        verify( kafkaTemplate ).send( eq( FAILED_PROCESSING_TOPIC ), stringCaptor.capture() );
        JsonObject response = gson.fromJson( stringCaptor.getValue(), JsonObject.class );

        assertThat( response.get( "topic" ).getAsString() ).isEqualTo( "test-topic" );
        assertThat( response.get( "partition" ).getAsInt() ).isEqualTo( 1 );
        assertThat( response.get( "offset" ).getAsInt() ).isEqualTo( 2 );
        assertThat( response.get( "input" ).getAsString() ).isEqualTo( "not a JSON string" );
        assertThat( response.get( "timestamp" ).getAsString() ).isEqualTo( "2024-06-09T03:15:31Z" );
        assertThat( response.get( "error" ).getAsString() ).asString().startsWith( "Expected a com.google.gson.JsonObject but was com.google.gson.JsonPrimitive;" );
        assertThat( response.get( "stacktrace" ).getAsString() ).asString().startsWith( "com.google.gson.JsonSyntaxException: Expected a com.google.gson.JsonObject but was com.google.gson.JsonPrimitive;" );
    }
}
