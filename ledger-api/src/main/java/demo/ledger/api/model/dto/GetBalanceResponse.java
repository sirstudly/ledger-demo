package demo.ledger.api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigInteger;
import java.time.OffsetDateTime;

@Builder
@Data
@EqualsAndHashCode( callSuper = true )
@AllArgsConstructor
public class GetBalanceResponse extends RestResponse {

    private String uuid;
    private String name;
    private String description;
    private BigInteger totalDebits;
    private BigInteger totalCredits;
    private OffsetDateTime timestamp;

    @Override
    public GetBalanceResponse withError( String error ) {
        return (GetBalanceResponse) super.withError( error );
    }

}
