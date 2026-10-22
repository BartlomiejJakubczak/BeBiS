package com.bebis.BeBiS.tools;

/**
 * A game concept with a stable numeric id that is the same in the game client, the Blizzard API and VMaNGOS
 * (e.g. playable classes and races). Display names are localized, ids are not, so mapping always goes through the id.
 * <p>
 * Ids also define bitmask positions: bit {@code id - 1} is set when the concept is included in a mask
 * (see {@link EnumTools#fromBitmask}).
 */
public interface GameIdentified {

    int getId();
}
