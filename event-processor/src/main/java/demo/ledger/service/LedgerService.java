package demo.ledger.service;

import demo.ledger.model.Ledger;
import demo.ledger.repository.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class LedgerService {

    private static final Logger LOGGER = LoggerFactory.getLogger( LedgerService.class );
    private final LedgerRepository repository;

    public LedgerService( LedgerRepository repository ) {
        this.repository = repository;
    }

    public Ledger createLedger( String uuid, String name, String description ) {
        LOGGER.info( "Creating ledger: uuid={}, name={}, description={}", uuid, name, description );
        Ledger obj = repository.save( Ledger.builder()
                .uuid( uuid.toLowerCase() )
                .name( name )
                .description( description )
                .createdDate( OffsetDateTime.now() )
                .lastUpdatedDate( OffsetDateTime.now() )
                .build() );
        LOGGER.info( "Created ledger: id={}", obj.getId() );
        return obj;
    }
}
