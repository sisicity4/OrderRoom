package com.github.karuhito.orderroombackend.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {UpdateItemPurchaseDetailRequestValidator.class})
public @interface BothOrNeitherPresent{
    String message() default "両方入力または未入力にする必要があります";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
