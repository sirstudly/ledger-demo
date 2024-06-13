package demo.ledger.api.model.dto;

import demo.ledger.model.LedgerTransaction;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode( callSuper = true )
public class CreateLedgerTransactionResponse extends RestResponse {
    private LedgerTransaction ledgerTransaction;

    public CreateLedgerTransactionResponse( RequestStatus status ) {
        super( status );
    }

    @Override
    public CreateLedgerTransactionResponse withError( String error ) {
        return (CreateLedgerTransactionResponse) super.withError( error );
    }

    public CreateLedgerTransactionResponse withLedgerTransaction( LedgerTransaction ledgerTransaction ) {
        setLedgerTransaction( ledgerTransaction );
        return this;
    }
}
