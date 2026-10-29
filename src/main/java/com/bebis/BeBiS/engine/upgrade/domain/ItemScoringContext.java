package com.bebis.BeBiS.engine.upgrade.domain;

import com.bebis.BeBiS.profile.domain.ClassSpec;
import com.bebis.BeBiS.profile.domain.WowCharacter;

/**
 * What item scoring needs to know about the character: the spec picks the stat weights, and the race adds its
 * racial bonuses.
 */
public record ItemScoringContext(ClassSpec classSpec, WowCharacter.Race race) {
}
