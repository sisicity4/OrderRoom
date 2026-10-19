package com.github.karuhito.orderroombackend.converter;

import com.github.karuhito.orderroombackend.entity.ItemStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToItemStatusConverter implements Converter<String, ItemStatus> {
  @Override
  public ItemStatus convert(String source) {
    ItemStatus[] statuses = ItemStatus.values();
    for (ItemStatus status : statuses) {
      if (status.getValue().equals(source)) {
        return status;
      }
    }
    throw new IllegalArgumentException("不正なstatusです: " + source);
  }
}
