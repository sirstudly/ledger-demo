package demo.ledger.api.model.dto;

import demo.ledger.model.Ledger;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateLedgerResponse extends RestResponse {
    private Ledger ledger;

    public CreateLedgerResponse( RequestStatus status ) {
        super( status );
    }

    @Override
    public CreateLedgerResponse withError(String error) {
        return (CreateLedgerResponse) super.withError( error );
    }

    public CreateLedgerResponse withLedger( Ledger ledger ) {
        setLedger( ledger );
        return this;
    }
}
