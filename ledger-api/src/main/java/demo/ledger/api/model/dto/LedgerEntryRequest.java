package demo.ledger.api.model.dto;

import demo.ledger.api.model.validation.ValidLedgerTransferDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LedgerEntryRequest {

    @NotNull
    @Valid
    private LedgerEntryAccount ledgerAccount;

    @NotNull
    @Schema( description = "The amount to transfer in the ledger currency base unit" )
    @Min( value = 1L, message = "The amount must be positive" )
    private BigInteger amount;

    @ValidLedgerTransferDirection
    private String direction;
}
