package demo.ledger.api.model.dto;

import demo.ledger.api.model.validation.ValidUUID;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UuidLookup {
    @ValidUUID
    @NotBlank
    @Schema( description = "A valid UUID to lookup (lowercase)" )
    private String uuid;
}
