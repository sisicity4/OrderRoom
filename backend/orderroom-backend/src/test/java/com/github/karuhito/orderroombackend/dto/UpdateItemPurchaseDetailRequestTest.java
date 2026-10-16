package com.github.karuhito.orderroombackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

public class UpdateItemPurchaseDetailRequestTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    // 正常系 
    @Test // 1. 両方あり、正の値の場合
    void testUpdateItemPurchaseDetailRequest() {
        UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(1000, UUID.randomUUID());
        Set<ConstraintViolation<UpdateItemPurchaseDetailRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    @Test // actualPriceが-1の場合、エラーになることを確認する
    void testUpdateItemPurchaseDetailRequestWithNegativePrice() {
        UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(-1, UUID.randomUUID());
        Set<ConstraintViolation<UpdateItemPurchaseDetailRequest>> violations = validator.validate(request);

        Iterator<ConstraintViolation<UpdateItemPurchaseDetailRequest>> iterator = violations.iterator();


        assertEquals(1, violations.size());

        ConstraintViolation<UpdateItemPurchaseDetailRequest> violation = iterator.next();
        assertEquals("actualPrice", violation.getPropertyPath().toString());
        assertEquals("実購入額をマイナスに設定することはできません", violation.getMessage());
    }

    @Test  // 両方nullの場合、クリアと判断され通ることを確認
    void testUpdateItemPurchaseDetailRequestWithNullValues() {
        UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(null, null);
        Set<ConstraintViolation<UpdateItemPurchaseDetailRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    @Test // actualPriceの境界値(0)の場合エラーにならないことを確認
    void testUpdateItemPurchaseDetailRequestWithZeroPrice() {
        UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(0, UUID.randomUUID());
        Set<ConstraintViolation<UpdateItemPurchaseDetailRequest>> violations = validator.validate(request);
        
        assertEquals(0, violations.size());
    }

    @Test  // requestの中身がactualPriceのみの場合エラーになることを確認する
    void testUpdateItemPurchaseDetailRequestWithOnlyActualPrice() {
        UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(1000, null);
        Set<ConstraintViolation<UpdateItemPurchaseDetailRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<UpdateItemPurchaseDetailRequest> violation = violations.iterator().next();
        assertEquals("paidByParticipantId", violation.getPropertyPath().toString());
        assertEquals("実購入額を入力した場合は購入者も選択してください", violation.getMessage());
    }

    @Test  // requestの中身がpaidByParticipantIdのみの場合エラーになることを確認する
    void testUpdateItemPurchaseDetailRequestWithOnlyPaidByParticipantId() {
        UpdateItemPurchaseDetailRequest request = new UpdateItemPurchaseDetailRequest(null, UUID.randomUUID());
        Set<ConstraintViolation<UpdateItemPurchaseDetailRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<UpdateItemPurchaseDetailRequest> violation = violations.iterator().next();
        assertEquals("actualPrice", violation.getPropertyPath().toString());
        assertEquals("購入者を選択した場合は実購入額も入力してください", violation.getMessage());
    }
}
