package demo.ledger.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class FailedResponse {

    private String uuid;
    private String error;

}
