package com.github.karuhito.orderroombackend.validation;

import java.util.UUID;

import com.github.karuhito.orderroombackend.dto.UpdateItemPurchaseDetailRequest;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class UpdateItemPurchaseDetailRequestValidator implements ConstraintValidator<BothOrNeitherPresent, UpdateItemPurchaseDetailRequest> {

    @Override
    public boolean isValid(UpdateItemPurchaseDetailRequest request, ConstraintValidatorContext context) {
        Integer actualPrice = request.actualPrice();
        UUID paidByParticipantId = request.paidByParticipantId();

        boolean hasActualPrice = actualPrice != null;
        boolean hasPaidId = paidByParticipantId != null;

        if (hasActualPrice == hasPaidId) {
            return true;
        }
        
        String message;
        String property;
        if (hasActualPrice) {
            message = "実購入額を入力した場合は購入者も選択してください";
            property = "paidByParticipantId";
        } else {
            message = "購入者を選択した場合は実購入額も入力してください";
            property = "actualPrice";
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addPropertyNode(property).addConstraintViolation();
        return false;
    } 
}