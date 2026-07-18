package com.github.karuhito.orderroombackend.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.karuhito.orderroombackend.entity.Item;

public interface ItemRepository  extends JpaRepository<Item, UUID>{
}