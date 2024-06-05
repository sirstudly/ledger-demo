package demo.ledger.api.repository;

import demo.ledger.api.model.dto.Ledger;
import demo.ledger.api.model.dto.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long> {
}
