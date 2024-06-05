package demo.ledger.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
//@TestPropertySource(properties = "spring.config.additional-location=classpath:application-test.properties")
//@TestPropertySource(locations="classpath:application-test.properties")
class ApplicationTests {

    @Test
    void contextLoads() {
    }

}
