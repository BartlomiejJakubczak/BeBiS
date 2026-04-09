package com.bebis.BeBiS.profile.jpa;

import com.bebis.BeBiS.equipment.jpa.EquipmentEntity;
import com.bebis.BeBiS.profile.WowCharacter;
import com.bebis.BeBiS.profile.dto.CharacterSyncData;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "wow_characters")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(exclude = "equipment")
public class WowCharacterEntity {
    @EmbeddedId
    private CompositeKey pk;

    @OneToOne(mappedBy = "character", cascade = CascadeType.ALL) // mappedBy means "I'm defining the relationship"
    @PrimaryKeyJoinColumn
    private EquipmentEntity equipment;

    @Column(nullable = false)
    private String name;

    @Column
    private int level;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WowCharacter.Race race;

    @Column(name = "wow_class", nullable = false)
    @Enumerated(EnumType.STRING)
    private WowCharacter.WowClass wowClass;

    @Column(name = "realm_name", nullable = false)
    private String realmName;

    public boolean updateFrom(CharacterSyncData fresh) {
        if (this.name == null || this.level == 0 || this.race == null || this.wowClass == null || this.realmName == null) {
            setFields(fresh);
            return true;
        } else {
            boolean anyUpdate = !this.name.equals(fresh.name())
                    || this.level != fresh.level()
                    || !this.race.equals(fresh.race())
                    || !this.wowClass.equals(fresh.wowClass())
                    || !this.realmName.equals(fresh.realmName());
            if (anyUpdate) {
                setFields(fresh);
            }
            return anyUpdate;
        }
    }

    private void setFields(CharacterSyncData fresh) {
        this.name = fresh.name();
        this.level = fresh.level();
        this.race = fresh.race();
        this.wowClass = fresh.wowClass();
        this.realmName = fresh.realmName();
    }

    @Embeddable
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @EqualsAndHashCode
    public static class CompositeKey implements Serializable {

        @Column(name = "id")
        private long id;

        @Column(name = "realm_slug", nullable = false)
        private String realmSlug;

        @Column(name = "blizzard_account_id")
        private long blizzardAccountId;
    }
}
