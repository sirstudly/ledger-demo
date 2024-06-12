package demo.ledger.api.model.dto;

import demo.ledger.api.model.validation.ValidUUID;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UuidLookup {
    @ValidUUID
    @NotBlank
    private String uuid;
}
