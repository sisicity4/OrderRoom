package com.github.karuhito.orderroombackend.exception;

public enum InvalidPurchaseDetailStateReason {
    NOT_ACCEPTED, // ItemのStatusがAccepted(採用)ではない
    NOT_PURCHASED // Itemが購入済みでない
}