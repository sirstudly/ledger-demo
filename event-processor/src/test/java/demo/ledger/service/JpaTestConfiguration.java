package demo.ledger.service;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableAutoConfiguration
@EnableJpaRepositories({"demo.ledger.repository", "demo.ledger.model"})
@EntityScan("demo.ledger.model")
public class JpaTestConfiguration {
    // Used for integration testing of repository/entity classes against a real database
}
