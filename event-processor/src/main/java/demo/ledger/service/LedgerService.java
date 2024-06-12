package demo.ledger.service;

import demo.ledger.model.Ledger;
import demo.ledger.model.LedgerAccount;
import demo.ledger.model.LedgerEntry;
import demo.ledger.model.LedgerTransaction;
import demo.ledger.repository.LedgerAccountRepository;
import demo.ledger.repository.LedgerEntryRepository;
import demo.ledger.repository.LedgerRepository;
import demo.ledger.repository.LedgerTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LedgerService {

    private static final Logger LOGGER = LoggerFactory.getLogger( LedgerService.class );
    private final LedgerRepository ledgerRepository;
    private final LedgerAccountRepository ledgerAccountRepository;
    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService( LedgerRepository ledgerRepository, LedgerAccountRepository ledgerAccountRepository,
                          LedgerTransactionRepository ledgerTransactionRepository, LedgerEntryRepository ledgerEntryRepository ) {
        this.ledgerRepository = ledgerRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.ledgerTransactionRepository = ledgerTransactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public Ledger createLedger( String uuid, String name, String description ) {
        LOGGER.info( "Creating ledger: uuid={}, name={}, description={}", uuid, name, description );
        Ledger obj = ledgerRepository.save( Ledger.builder()
                .uuid( uuid )
                .name( name )
                .description( description )
                .createdDate( OffsetDateTime.now() )
                .lastUpdatedDate( OffsetDateTime.now() )
                .build() );
        LOGGER.info( "Created ledger: id={}", obj.getId() );
        return obj;
    }

    public Optional<Ledger> getLedger( String uuid ) {
        return ledgerRepository.findOne( Example.of( Ledger.builder().uuid( uuid ).build() ) );
    }

    public LedgerAccount createLedgerAccount( Ledger ledger, String uuid, String name, String description, String currency ) {
        LOGGER.info( "Creating ledger account: ledgerId={}, uuid={}, name={}, description={}", ledger.getId(), uuid, name, description, currency );

        LedgerAccount obj = ledgerAccountRepository.save( LedgerAccount.builder()
                .uuid( uuid )
                .ledger( ledger )
                .name( name )
                .description( description )
                .currency( currency )
                .createdDate( OffsetDateTime.now() )
                .lastUpdatedDate( OffsetDateTime.now() )
                .build() );
        LOGGER.info( "Created ledger account: id={}", obj.getId() );
        return obj;
    }

    public Optional<LedgerAccount> getLedgerAccount( String uuid ) {
        return ledgerAccountRepository.findOne( Example.of( LedgerAccount.builder().uuid( uuid ).build() ) );
    }

    public LedgerTransaction createLedgerTransaction( String uuid, String description, List<LedgerEntry> ledgerEntries ) {
        LOGGER.info( "Creating ledger transaction: uuid={}, description={}", uuid, description );

        LOGGER.info( "Saving all {} ledger entries first...", ledgerEntries.size() );
        List<LedgerEntry> savedLedgerEntries = ledgerEntryRepository.saveAll( ledgerEntries );

        LedgerTransaction obj = ledgerTransactionRepository.save( LedgerTransaction.builder()
                .uuid( uuid )
                .description( description )
                .ledgerEntries( savedLedgerEntries )
                .createdDate( OffsetDateTime.now() )
                .build() );
        LOGGER.info( "Created ledger transaction: id={}", obj.getId() );
        return obj;
    }

    public Optional<LedgerTransaction> getLedgerTransaction( String uuid ) {
        return ledgerTransactionRepository.findOne( Example.of( LedgerTransaction.builder().uuid( uuid ).build() ) );
    }
}
