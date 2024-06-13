package demo.ledger.api.model.dto;

import demo.ledger.api.model.validation.ValidUUID;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateLedgerTransactionRequest {

    @NotBlank
    @ValidUUID
    @Schema( description = "A unique UUID to identify this ledger transaction" )
    private String uuid;

    @Size( max = 1000 )
    @Schema( description = "A short description of this ledger" )
    private String description;

    @NotEmpty
    @Valid
    private List<LedgerEntryRequest> ledgerEntries;

}
