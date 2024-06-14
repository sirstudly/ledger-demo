package demo.ledger.api.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target( ElementType.FIELD )
@Constraint( validatedBy = {} )
@Retention( RUNTIME )
@Pattern( regexp = "^[0-9]{4}-[0-9]{2}-[0-9]{2}T([0-9]{2}:){2}[0-9]{2}[+|-][0-9]{2}:[0-9]{2}$" )
@ReportAsSingleViolation
public @interface ValidDateTime {
    String message() default "Date/time must be specified in the following format YYYY-MM-DDThh:mm:ss+HH:MM (eg. 2024-04-11T10:24:35+02:00)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
