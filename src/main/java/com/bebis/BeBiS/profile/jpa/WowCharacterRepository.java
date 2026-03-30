package com.bebis.BeBiS.profile.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WowCharacterRepository extends JpaRepository<WowCharacterEntity, WowCharacterEntity.CompositeKey> {

    // The underscore tells JPA: "Look inside 'pk', then find 'blizzardAccountId'"
    List<WowCharacterEntity> findAllByPk_BlizzardAccountId(long blizzardAccountId);
}
