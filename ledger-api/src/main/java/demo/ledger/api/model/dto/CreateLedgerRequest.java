package demo.ledger.api.model.dto;

import demo.ledger.api.model.validation.ValidUUID;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
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

}
