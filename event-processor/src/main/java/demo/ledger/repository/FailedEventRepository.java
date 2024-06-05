package demo.ledger.repository;

import demo.ledger.model.FailedProcessingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedEventRepository extends JpaRepository<FailedProcessingEvent, Long> {
}
