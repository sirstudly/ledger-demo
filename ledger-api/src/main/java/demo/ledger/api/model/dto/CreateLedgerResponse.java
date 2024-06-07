package demo.ledger.api.model.dto;

import demo.ledger.model.Ledger;

public class CreateLedgerResponse extends RestResponse {
    private Ledger ledger;

    public CreateLedgerResponse( RequestStatus status ) {
        super( status );
    }

    public Ledger getLedger() {
        return ledger;
    }

    public void setLedger( Ledger ledger ) {
        this.ledger = ledger;
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
