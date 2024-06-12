package demo.ledger.api.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@JsonInclude( JsonInclude.Include.NON_NULL )
@Data
@NoArgsConstructor
public class RestResponse {
    @Schema( description = "The current status of the request" )
    private RequestStatus status;

    @Schema( description = "A single error message" )
    private String error;

    @Schema( description = "Multiple error messages with the key being the name of the field in error" )
    private Map<String, String> errors;

    public RestResponse( RequestStatus status ) {
        this.status = status;
    }

    public RestResponse withError( String error ) {
        this.error = error;
        return this;
    }

    public RestResponse withErrors( Map<String, String> errors ) {
        setErrors( errors );
        return this;
    }

}
