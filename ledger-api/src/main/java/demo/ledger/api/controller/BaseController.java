package demo.ledger.api.controller;

import demo.ledger.api.model.dto.RequestStatus;
import demo.ledger.api.model.dto.RestResponse;
import demo.ledger.api.model.exception.ValidationException;
import demo.ledger.model.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Common controller class.
 */
public abstract class BaseController {

    private static final Logger LOGGER = LoggerFactory.getLogger( BaseController.class );

    private KafkaTemplate<String, String> kafkaTemplate;

    public BaseController( KafkaTemplate<String, String> kafkaTemplate ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Returns the event topic used when sending messages for this controller.
     *
     * @return non-null topic
     */
    public abstract String getEventTopic();

    @ResponseStatus( HttpStatus.BAD_REQUEST )
    @ExceptionHandler( MethodArgumentNotValidException.class )
    public RestResponse handleValidationExceptions( MethodArgumentNotValidException ex ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach( ( error ) -> {
            String fieldName = ( (FieldError) error ).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put( fieldName, errorMessage );
        } );
        return new RestResponse( RequestStatus.failed ).withErrors( errors );
    }

    @ResponseStatus( HttpStatus.BAD_REQUEST )
    @ExceptionHandler( MissingServletRequestParameterException.class )
    public RestResponse handleValidationExceptions( MissingServletRequestParameterException ex ) {
        Map<String, String> errors = new HashMap<>();
        errors.put( ex.getParameterName(), ex.getMessage() );
        return new RestResponse( RequestStatus.failed ).withErrors( errors );
    }

    @ResponseStatus( HttpStatus.BAD_REQUEST )
    @ExceptionHandler( HandlerMethodValidationException.class )
    public RestResponse handleValidationExceptions( HandlerMethodValidationException ex ) {
        Map<String, String> errors = new HashMap<>();
        AtomicReference<Object> argument = new AtomicReference<>();
        ex.getAllValidationResults()
                .stream()
                .peek( e -> argument.set( e.getArgument() ) )
                .map( ParameterValidationResult::getResolvableErrors )
                .flatMap( List::stream )
                .map( MessageSourceResolvable::getDefaultMessage )
                .peek( LOGGER::info )
                .forEach( error -> errors.put( argument.get().toString(), error ) );
        return new RestResponse( RequestStatus.failed ).withErrors( errors );
    }

    @ResponseStatus( HttpStatus.NOT_FOUND )
    @ExceptionHandler( NotFoundException.class )
    public RestResponse handleEntityNotFoundException( NotFoundException ex ) {
        return new RestResponse( RequestStatus.failed ).withError( ex.getMessage() );
    }

    @ResponseStatus( HttpStatus.BAD_REQUEST )
    @ExceptionHandler( ValidationException.class )
    public RestResponse handleValidationException( ValidationException ex ) {
        return new RestResponse( RequestStatus.failed ).withError( ex.getMessage() );
    }

    @ResponseStatus( HttpStatus.BAD_REQUEST )
    @ExceptionHandler( HttpMessageNotReadableException.class )
    public RestResponse handleHttpMessageNotReadableException( HttpMessageNotReadableException ex ) {
        return new RestResponse( RequestStatus.failed ).withError( "Request unreadable" );
    }

    protected void sendMessage( String key, String payload ) {
        kafkaTemplate.send( getEventTopic(), key, payload ).whenComplete( ( result, ex ) -> {
            if ( ex == null ) {
                LOGGER.info( "Sent message=[key={}, payload={}] to topic={} with offset={}",
                        key, payload, getEventTopic(), result.getRecordMetadata().offset() );
            }
            else {
                LOGGER.info( "Unable to send message=[key={},payload={}] to topic={} due to : {}",
                        key, payload, getEventTopic(), ex.getMessage() );
            }
        } );
    }
}
