package com.github.karuhito.orderroombackend.entity;



import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ItemStatusConverter implements AttributeConverter<ItemStatus, String> {

    @Override
    public String convertToDatabaseColumn(ItemStatus attribute) {
        // Java(大文字Enum) -> DB保存用(小文字文字列)
        String dbData = attribute.name();
        return dbData.toLowerCase();
    }

    @Override
    public ItemStatus convertToEntityAttribute(String dbData) {
        // DB保存値(小文字文字列) -> Java(大文字Enum)
        ItemStatus attribute = ItemStatus.valueOf(dbData.toUpperCase());
        return attribute;
    }
}