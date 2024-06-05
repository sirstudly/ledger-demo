package demo.ledgerx;

import demo.ledger.service.LedgerEventConsumer;
import demo.ledger.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

/**
 * @link https://www.baeldung.com/spring-boot-kafka-testing
 */
@SpringBootTest(properties = "spring.profiles.active=test")
@DirtiesContext
//@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@EmbeddedKafka( partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers" )
@ContextConfiguration(classes = {IntegrationTestConfiguration.class, LedgerEventConsumer.class, KafkaTestProducer.class})
class EmbeddedKafkaIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedKafkaIntegrationTest.class);

    // object under test
    @Autowired
    @InjectMocks
    private LedgerEventConsumer consumer;

    @Autowired
    private KafkaTestProducer producer;

    @MockBean
    private LedgerService mockService;

    @Captor
    private ArgumentCaptor<String> stringCaptor;

    @Value("${spring.kafka.topic.default}")
    private String topic;

    @Test
    public void givenEmbeddedKafkaBroker_whenSendingWithSimpleProducer_thenMessageReceived()
            throws Exception {

        LOGGER.info("This is the topic: {}", topic);

        producer.send("ledger-operations", "{\n" +
                "  \"command\": \"CREATE_LEDGER\",\n" +
                "  \"id\": \"a42f714c-ce90-4a9c-a06e-9a1b8842d2de\",\n" +
                "  \"ledgerRequest\": {\n" +
                "    \"name\": \"roger one wilcox\",\n" +
                "    \"description\": \"my first ledger event\"\n" +
                "  }\n" +
                "}");

        try {
            synchronized ( this ) {
                this.wait( 10000 );
            }
        }
        catch ( InterruptedException ex ) {
            // and resume...
        }
        verify( mockService ).createLedger( stringCaptor.capture(), stringCaptor.capture(), stringCaptor.capture() );

        assertThat(stringCaptor.getAllValues().get(0), equals("a42f714c-ce90-4a9c-a06e-9a1b8842d2de"));
        assertThat(stringCaptor.getAllValues().get(1), equals("roger one wilcox"));
        assertThat(stringCaptor.getAllValues().get(2), equals("my first ledger event"));
    }
}