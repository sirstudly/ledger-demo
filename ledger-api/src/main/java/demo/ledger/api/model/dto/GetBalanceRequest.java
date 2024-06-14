package demo.ledger.api.model.dto;

import demo.ledger.api.model.validation.ValidDateTime;
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
public class GetBalanceRequest {

    @NotBlank
    @ValidUUID
    @Schema( description = "A unique UUID identifying the ledger account to query" )
    private String uuid;

    @ValidDateTime
    @Schema( description = "Date/time in the following format: YYYY-MM-DDThh:mm:ss+HH:MM (eg. 2024-04-11T10:24:35+02:00)" )
    private String timestamp;
}
