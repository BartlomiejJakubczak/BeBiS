package com.bebis.BeBiS.item;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<ItemEntity, Integer> {

    ItemEntity findByName(String name);

}
