package demo.ledger.api.repository;

import java.math.BigInteger;

public interface LedgerAccountBalance {
    String getLedgerAccountUuid();
    Long getLedgerAccountLockVersion();
    BigInteger getTotalDebits();
    BigInteger getTotalCredits();
}
