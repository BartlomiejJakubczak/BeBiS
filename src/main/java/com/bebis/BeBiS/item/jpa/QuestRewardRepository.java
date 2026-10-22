package com.bebis.BeBiS.item.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface QuestRewardRepository extends JpaRepository<QuestRewardEntity, QuestRewardEntity.CompositeKey> {

    /**
     * Loads the quest rows of many items at once, so assembling N items costs one query instead of N.
     */
    @Query("SELECT q FROM QuestRewardEntity q WHERE q.pk.baseId IN (:baseIds)")
    List<QuestRewardEntity> findAllByBaseIds(Set<Long> baseIds);
}
