package com.bebis.BeBiS.item.domain;

/**
 * How a quest hands out a reward item.
 */
public enum RewardKind {
    /**
     * One of up to six items the player picks from, so it excludes the quest's other choices.
     */
    CHOICE,
    /**
     * Always given on completion.
     */
    GUARANTEED
}
