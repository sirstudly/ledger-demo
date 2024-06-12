package demo.ledger.service;

import demo.ledger.model.Ledger;
import demo.ledger.model.LedgerAccount;
import demo.ledger.model.LedgerEntry;
import demo.ledger.model.LedgerTransaction;
import demo.ledger.model.LedgerTransactionDirection;
import demo.ledger.model.exception.NotFoundException;
import demo.ledger.repository.LedgerTransactionRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith( SpringRunner.class )
@ContextConfiguration( classes = {JpaTestConfiguration.class, LedgerService.class} )
@DataJpaTest
@Transactional( propagation = Propagation.NOT_SUPPORTED ) // disable rollback so you can query the db after the test
@ActiveProfiles( value = "test" )
@AutoConfigureTestDatabase( replace = AutoConfigureTestDatabase.Replace.NONE )
public class LedgerServiceIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger( LedgerServiceIntegrationTest.class );

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private LedgerTransactionRepository transactionRepository;

    @Test
    public void testCreateLedger() throws Exception {
        Ledger originalLedger = ledgerService.createLedger( UUID.randomUUID().toString(), "My first ledger", "A bunch of accounts" );
        LOGGER.info( "CREATED ledger id={}, uuid={}", originalLedger.getId(), originalLedger.getUuid() );

        Ledger fetchedLedger = ledgerService.getLedger( originalLedger.getUuid() )
                .orElseThrow( () -> new NotFoundException( "ledger not found!" ) );
        LOGGER.info( "FOUND ledger id={}, uuid={}", fetchedLedger.getId(), fetchedLedger.getUuid() );

        assertThat( fetchedLedger.getUuid() ).isEqualTo( originalLedger.getUuid() );
        assertThat( fetchedLedger.getId() ).isEqualTo( originalLedger.getId() );
        assertThat( fetchedLedger.getName() ).isEqualTo( originalLedger.getName() );
        assertThat( fetchedLedger.getDescription() ).isEqualTo( originalLedger.getDescription() );
    }

    @Test
    public void testCreateTransaction() throws Exception {
        Ledger ledgerBob = ledgerService.createLedger( UUID.randomUUID().toString(), "Bob's ledger", "Bob's accounts" );
        Ledger ledgerAnn = ledgerService.createLedger( UUID.randomUUID().toString(), "Ann's ledger", "Ann's accounts" );

        LedgerAccount acctBob = ledgerService.createLedgerAccount( ledgerBob, UUID.randomUUID().toString(), "Bob's checking account", "Bob's everyday transactions", "USD" );
        LedgerAccount acctAnn = ledgerService.createLedgerAccount( ledgerAnn, UUID.randomUUID().toString(), "Ann's checking account", "Ann's everyday transactions", "USD" );

        LedgerTransaction txn = ledgerService.createLedgerTransaction( UUID.randomUUID().toString(),
                "Transferring $50 from Bob to Ann", Arrays.asList(
                        LedgerEntry.builder()
                                .ledgerAccount( acctBob )
                                .amount( new BigInteger( "5000" ) )
                                .direction( LedgerTransactionDirection.debit )
                                .createdDate( OffsetDateTime.now() )
                                .build(),
                        LedgerEntry.builder()
                                .ledgerAccount( acctAnn )
                                .amount( new BigInteger( "5000" ) )
                                .direction( LedgerTransactionDirection.credit )
                                .createdDate( OffsetDateTime.now() )
                                .build()
                ) );
        LOGGER.info("CREATED transaction id={}, uuid={}", txn.getId(), txn.getUuid());

        LedgerTransaction fetchedTxn = ledgerService.getLedgerTransaction( txn.getUuid() )
                .orElseThrow( () -> new NotFoundException( "ledger transaction not found!" ) );
        LOGGER.info("FOUND transaction id={}, uuid={}", fetchedTxn.getId(), fetchedTxn.getUuid());
    }
}
