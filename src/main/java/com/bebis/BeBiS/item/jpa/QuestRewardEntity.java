package com.bebis.BeBiS.item.jpa;

import com.bebis.BeBiS.item.domain.RewardKind;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.util.Objects;

/**
 * A row of {@code quest_rewards}: one non-repeatable quest rewarding one gear item. Reference data seeded by Flyway
 * from the VMaNGOS export, never written by the app, hence {@link Immutable}. Not mapped as an association on
 * {@link ItemEntity}, because it refers to items by base id only, and an item can be rewarded by several quests;
 * {@code ItemQueryService} loads and attaches these rows instead.
 */
@Entity
@Immutable
@Table(name = "quest_rewards")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED) // for JPA only, the data is never built field by field
public class QuestRewardEntity {
    @EmbeddedId
    private QuestRewardEntity.CompositeKey pk;

    @Column(name = "quest_min_level", nullable = false)
    private int questMinLevel;

    @Column(name = "quest_level", nullable = false)
    private int questLevel; // actual recommended level

    @Column(name = "reward_kind", nullable = false)
    @Enumerated(EnumType.STRING)
    private RewardKind rewardKind;

    @Column(name = "required_classes", nullable = false)
    private int requiredClassMask; // bit (id − 1) set = allowed, 0 = no restriction

    @Column(name = "required_races", nullable = false)
    private int requiredRaceMask; // bit (id − 1) set = allowed, 0 = no restriction

    /**
     * Entities are equal when their primary keys are, which are assigned (not generated) and therefore always set.
     * Uses the getter rather than the field, because {@code o} may be a Hibernate proxy whose own fields are empty.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof QuestRewardEntity that)) return false;
        return Objects.equals(getPk(), that.getPk());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getPk());
    }

    /**
     * One row per (item, quest): the base item id, since quest rewards never roll suffixes, and the rewarding quest.
     */
    @Embeddable
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @EqualsAndHashCode
    public static class CompositeKey implements Serializable {

        @Column(name = "base_id")
        private long baseId;

        @Column(name = "quest_id")
        private long questId;
    }
}
