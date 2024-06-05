package demo.ledger.api.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target( ElementType.FIELD)
@Constraint(validatedBy={})
@Retention(RUNTIME)
@Pattern(regexp="^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[1-5][0-9A-Fa-f]{3}-[89ABab][0-9A-Fa-f]{3}-[0-9A-Fa-f]{12}$")
@ReportAsSingleViolation
public @interface ValidUUID {
    String message() default "Invalid UUID";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
