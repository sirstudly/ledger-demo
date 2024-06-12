package demo.ledger.model.exception;

import demo.ledger.model.dto.EventType;

public class DuplicateKeyException extends LedgerEventException {
    public DuplicateKeyException( String message ) {
        super( message );
    }

    public DuplicateKeyException( EventType eventType, String uuid, String message ) {
        super( eventType, uuid, message );
    }
}
