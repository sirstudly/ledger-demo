package demo.ledger.api.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@JsonInclude( JsonInclude.Include.NON_NULL )
public class RestResponse {
    @Schema( description = "The current status of the request" )
    private RequestStatus status;

    private String error;
    private Map<String, String> errors;

    public RestResponse( RequestStatus status ) {
        this.status = status;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public void setError( String error ) {
        this.error = error;
    }

    public RestResponse withError( String error ) {
        this.error = error;
        return this;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors( Map<String, String> errors ) {
        this.errors = errors;
    }

    public RestResponse withErrors( Map<String, String> errors ) {
        setErrors( errors );
        return this;
    }

}
