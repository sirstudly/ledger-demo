package demo.ledger.model.exception;

import demo.ledger.model.dto.EventType;

public class NotFoundException extends LedgerEventException {

    public NotFoundException( String message ) {
        super( message );
    }

    public NotFoundException( EventType eventType, String uuid, String message ) {
        super( eventType, uuid, message );
    }

}
