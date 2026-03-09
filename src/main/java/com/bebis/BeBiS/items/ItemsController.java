package com.bebis.BeBiS.items;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class ItemsController {

    private final ItemsService itemsService;

    public ItemsController(ItemsService itemsService) {
        this.itemsService = itemsService;
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<Item> getItem(@PathVariable int itemId) {
        return new ResponseEntity<>(itemsService.getItem(itemId), HttpStatus.OK);
    }
    
}
