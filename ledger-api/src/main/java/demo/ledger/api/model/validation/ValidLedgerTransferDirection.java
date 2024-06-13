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
@Pattern( regexp = "^(debit|credit)$" )
@ReportAsSingleViolation
public @interface ValidLedgerTransferDirection {
    String message() default "Only one of debit or credit is allowed";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
