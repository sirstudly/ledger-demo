package demo.ledger.api.model.dto;

import demo.ledger.api.model.validation.ValidUUID;
import jakarta.validation.constraints.NotBlank;

public class UuidLookup {
    @ValidUUID
    @NotBlank
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid( String uuid ) {
        this.uuid = uuid;
    }
}
