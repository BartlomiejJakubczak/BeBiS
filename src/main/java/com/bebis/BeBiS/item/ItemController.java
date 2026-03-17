package com.bebis.BeBiS.item;

import com.bebis.BeBiS.integration.blizzard.dto.ItemResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemsService;

    public ItemController(ItemService itemsService) {
        this.itemsService = itemsService;
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<ItemResponse> getItem(@PathVariable int itemId) {
        return new ResponseEntity<>(itemsService.getItem(itemId), HttpStatus.OK);
    }

}
