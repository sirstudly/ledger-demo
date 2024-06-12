package demo.ledger.model.exception;

import demo.ledger.model.dto.EventType;

public class LedgerEventException extends Exception {
    private EventType eventType;
    private String uuid;

    public LedgerEventException( String message ) {
        super( message );
    }

    public LedgerEventException( EventType eventType, String uuid, String message ) {
        super( message );
        setEventType( eventType );
        setUuid( uuid );
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType( EventType eventType ) {
        this.eventType = eventType;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid( String uuid ) {
        this.uuid = uuid;
    }
}
