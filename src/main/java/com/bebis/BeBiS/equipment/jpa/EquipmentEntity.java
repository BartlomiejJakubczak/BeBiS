package com.bebis.BeBiS.equipment.jpa;

import com.bebis.BeBiS.equipment.Equipment;
import com.bebis.BeBiS.item.jpa.ItemEntity;
import com.bebis.BeBiS.profile.jpa.WowCharacterEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "character_equipment")
@Getter
@Setter
@EqualsAndHashCode(exclude = "character")
public class EquipmentEntity {
    @Id
    private WowCharacterEntity.CompositeKey id;

    @OneToOne // when one-to-one JoinColumns creates columns in the current table, because eq belongs to the character
    @MapsId // look at character and grab its pk for my pk
    @JoinColumns({ // Because the characters' key is composite, I must define all 3 columns.
            @JoinColumn(name = "character_id", referencedColumnName = "id"),
            @JoinColumn(name = "realm_slug", referencedColumnName = "realm_slug"),
            @JoinColumn(name = "blizzard_account_id", referencedColumnName = "blizzard_account_id")
    })
    private WowCharacterEntity character;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    // in this case JoinColumns puts columns in the target table
    // Because it's Unidirectional, I MUST tell Hibernate where to put the Foreign Keys in the target table.
    // I am placing these 3 columns inside the 'equipped_items' table to link back to here.
    // This happens because the link is unidirectional. equipped_items can't look at equipment
    @JoinColumns({
            @JoinColumn(name = "owner_id", referencedColumnName = "character_id", nullable = false),
            @JoinColumn(name = "owner_realm", referencedColumnName = "realm_slug", nullable = false),
            @JoinColumn(name = "owner_account", referencedColumnName = "blizzard_account_id", nullable = false)
    })
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "slot_name") // This column goes into 'equipped_items' to store 'HEAD', 'CHEST', etc.
    Map<Equipment.Slot, EquippedItem> items;

    @Entity
    @Table(name = "equipped_items")
    @Getter
    @Setter
    @NoArgsConstructor
    public static class EquippedItem {

        // This is a simple helper table. A basic auto-increment ID is perfect here.
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "full_name")
        private String fullName;

        // Because ItemEntity uses a composite key, I MUST use @JoinColumns here.
        @ManyToOne(optional = false) // optional = false ensures a slot can't exist without an item
        @JoinColumns({
                @JoinColumn(name = "item_id", referencedColumnName = "item_id"),
                @JoinColumn(name = "random_enchantment_id", referencedColumnName = "random_enchantment_id")
        })
        private ItemEntity item;

        @ElementCollection
        @CollectionTable(name = "player_enchants")
        // will create two rows, equipped_item_id and the one you name in @Column for value
        @Column(name = "enchant_display_string", length = 500)
        private List<String> playerEnchants = new ArrayList<>();
    }
}
