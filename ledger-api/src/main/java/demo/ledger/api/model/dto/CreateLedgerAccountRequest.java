package demo.ledger.api.model.dto;

import demo.ledger.api.model.validation.ValidUUID;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateLedgerAccountRequest {

    @NotBlank
    @ValidUUID
    @Schema( description = "A unique UUID to identify this ledger account" )
    private String uuid;

    @NotBlank
    @Size(max = 1000)
    @Schema( description = "The name of this ledger")
    private String name;

    @Size(max = 1000)
    @Schema( description = "A short description of this ledger")
    private String description;

    @NotBlank
    @Pattern( regexp = "^[A-Za-z]{3}$", message = "must be a three-character code" )
    @Schema( description = "The ISO 4217 three-letter currency code for this account" )
    private String currency;

    @NotBlank
    @ValidUUID
    @Schema( description = "The UUID of the ledger this account belongs to")
    private String ledgerUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid( String uuid ) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency( String currency ) {
        this.currency = currency;
    }

    public String getLedgerUuid() {
        return ledgerUuid;
    }

    public void setLedgerUuid( String ledgerUuid ) {
        this.ledgerUuid = ledgerUuid;
    }
}
