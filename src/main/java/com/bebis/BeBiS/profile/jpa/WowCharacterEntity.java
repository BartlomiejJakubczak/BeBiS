package com.bebis.BeBiS.profile.jpa;

import com.bebis.BeBiS.profile.WowCharacter;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class WowCharacterEntity {
    @EmbeddedId
    private CompositeKey pk;

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

    public boolean updateFrom(WowCharacterEntity fresh) {
        boolean anyUpdate = !this.name.equals(fresh.getName())
                || this.level != fresh.getLevel()
                || !this.race.equals(fresh.getRace())
                || !this.wowClass.equals(fresh.getWowClass())
                || !this.realmName.equals(fresh.getRealmName());
        this.name = fresh.getName();
        this.level = fresh.getLevel();
        this.race = fresh.getRace();
        this.wowClass = fresh.getWowClass();
        this.realmName = fresh.getRealmName();
        return anyUpdate;
    }

    @Embeddable
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class CompositeKey implements Serializable {

        @Column(name = "id", nullable = false)
        private long id;

        @Column(name = "realm_slug", nullable = false)
        private String realmSlug;

        @Column(name = "blizzard_account_id", nullable = false)
        private long blizzardAccountId;
    }
}
