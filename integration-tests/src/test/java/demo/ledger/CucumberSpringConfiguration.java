package demo.ledger;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@CucumberContextConfiguration
@SpringBootTest( classes = TestConfiguration.class )
public class CucumberSpringConfiguration {
}
