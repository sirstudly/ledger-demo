package demo.ledger.api.model.dto;

import demo.ledger.api.model.validation.ValidUUID;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LedgerEntryAccount {

    @NotBlank
    @ValidUUID
    @Schema( description = "The unique ledger account UUID for which this ledger entry belongs" )
    private String uuid;

}
