package demo.ledger.api.repository;

import demo.ledger.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    @Query( "SELECT " +
            "e.ledgerAccount.uuid AS ledgerAccountUuid, " +
            "MAX(e.ledgerAccount.lockVersion) AS ledgerAccountLockVersion, " +
            "SUM(CASE WHEN e.direction = 'credit' THEN e.amount ELSE 0 END) AS totalCredits, " +
            "SUM(CASE WHEN e.direction = 'debit' THEN e.amount ELSE 0 END) AS totalDebits " +
            "FROM LedgerEntry e " +
            "WHERE e.ledgerAccount.uuid = :ledgerAccountUuid " +
            "AND e.createdDate <= :queryDate " +
            "GROUP BY e.ledgerAccount.uuid" )
    List<LedgerAccountBalance> getBalances( @Param( "ledgerAccountUuid" ) String ledgerAccountId,
                                @Param( "queryDate" ) OffsetDateTime createdDate );

    @Query( "SELECT " +
            "e.ledgerAccount.uuid AS ledgerAccountUuid, " +
            "MAX(e.ledgerAccount.lockVersion) AS ledgerAccountLockVersion, " +
            "SUM(CASE WHEN e.direction = 'credit' THEN e.amount ELSE 0 END) AS totalCredits, " +
            "SUM(CASE WHEN e.direction = 'debit' THEN e.amount ELSE 0 END) AS totalDebits " +
            "FROM LedgerEntry e " +
            "WHERE e.ledgerAccount.uuid = :ledgerAccountUuid " +
            "GROUP BY e.ledgerAccount.uuid" )
    List<LedgerAccountBalance> getBalances( @Param( "ledgerAccountUuid" ) String ledgerAccountId );

}

