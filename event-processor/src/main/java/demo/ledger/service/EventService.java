package demo.ledger.service;

import demo.ledger.model.FailedProcessingEvent;
import demo.ledger.model.Ledger;
import demo.ledger.repository.FailedEventRepository;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger( EventService.class );
    private final FailedEventRepository repository;

    @Autowired
    public EventService( FailedEventRepository repository ) {
        this.repository = repository;
    }

    public void saveFailedProcessingEvent( FailedProcessingEvent event ) {
        LOGGER.info( "Saving failed processing event: {}", ToStringBuilder.reflectionToString(event) );
        FailedProcessingEvent obj = repository.save( event );
        LOGGER.info( "Saved failed processing event: id={}", obj.getId() );
    }
}
