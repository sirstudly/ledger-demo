package demo.ledger.api.model.dto;

import demo.ledger.model.Ledger;
import demo.ledger.model.LedgerAccount;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CreateLedgerAccountResponse extends RestResponse {
    private LedgerAccount ledgerAccount;

    public CreateLedgerAccountResponse( RequestStatus status ) {
        super( status );
    }

    @Override
    public CreateLedgerAccountResponse withError( String error) {
        return (CreateLedgerAccountResponse) super.withError( error );
    }

    public CreateLedgerAccountResponse withLedgerAccount( LedgerAccount ledgerAccount ) {
        setLedgerAccount( ledgerAccount );
        return this;
    }
}
