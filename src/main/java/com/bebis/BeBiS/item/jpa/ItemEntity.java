package com.bebis.BeBiS.item.jpa;

import com.bebis.BeBiS.item.Item;
import com.bebis.BeBiS.item.StatType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_category")
@Getter
@Setter
@EqualsAndHashCode
public abstract class ItemEntity {
    @EmbeddedId
    private CompositeKey id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Item.Quality quality;

    @Column(name = "inventory_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Item.InventoryType inventoryType;

    @Column(name = "item_level")
    private int itemLevel;

    @Column(name = "required_level")
    private int requiredLevel;

    @Column(name = "unique_equipped")
    private boolean uniqueEquipped;

    @ElementCollection // Tells Hibernate this is a separate collection table, not a standard column.
    @CollectionTable(name = "item_stats", joinColumns = {
            @JoinColumn(name = "item_id"),
            @JoinColumn(name = "random_enchantment_id")
    })
    //Defines the table name and the foreign key linking it back to the Item
    @MapKeyColumn(name = "stat_type") // Tells Hibernate to use the 'stat_type' column as the key (e.g., STAMINA)
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "stat_value") // The Map Value: Defines the column for the actual number (e.g., 19)
    private Map<StatType, Integer> stats = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "item_effects", joinColumns = {
            @JoinColumn(name = "item_id"),
            @JoinColumn(name = "random_enchantment_id")
    })
    @Column(name = "effect_description", length = 1000) // Effects can be long!
    private List<String> specialEffects = new ArrayList<>();

    @Embeddable
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class CompositeKey {

        @Column(name = "item_id")
        private long itemId;

        @Column(name = "random_enchantment_id")
        private long randomEnchantmentId;
    }
}
