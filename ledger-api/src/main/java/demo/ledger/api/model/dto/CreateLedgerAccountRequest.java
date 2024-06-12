package demo.ledger.api.model.dto;

import demo.ledger.api.model.validation.ValidUUID;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateLedgerAccountRequest {

    @NotBlank
    @ValidUUID
    @Schema( description = "A unique UUID to identify this ledger account" )
    private String uuid;

    @NotBlank
    @ValidUUID
    @Schema( description = "The UUID of the ledger this account belongs to" )
    private String ledgerUuid;

    @NotBlank
    @Size( max = 1000 )
    @Schema( description = "The name of this ledger" )
    private String name;

    @Size( max = 1000 )
    @Schema( description = "A short description of this ledger" )
    private String description;

    @NotBlank
    @Pattern( regexp = "^[A-Za-z]{3}$", message = "must be a three-character code" )
    @Schema( description = "The ISO 4217 three-letter currency code for this account" )
    private String currency;

}
