package demo.ledger.model.dto;

public class ApiOperation<T> {
    private EventType eventType;
    private T data;

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType( EventType eventType ) {
        this.eventType = eventType;
    }

    public ApiOperation<T> withEventType( EventType eventType ) {
        setEventType( eventType );
        return this;
    }

    public T getData() {
        return data;
    }

    public void setData( T data ) {
        this.data = data;
    }

    public ApiOperation<T> withData( T data ) {
        setData( data );
        return this;
    }
}
