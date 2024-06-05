package demo.ledger.api.model.dto;

import demo.ledger.api.model.validation.ValidUUID;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateLedgerRequest {

    @NotBlank
    @ValidUUID
    @Schema( description = "A unique UUID to identify this ledger" )
    private String uuid;

    @NotBlank
    @Size(max = 1000)
    @Schema( description = "The name of this ledger")
    private String name;
    @Size(max = 1000)
    @Schema( description = "A short description of this ledger")
    private String description;

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
}
