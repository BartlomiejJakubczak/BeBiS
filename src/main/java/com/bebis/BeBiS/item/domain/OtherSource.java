package com.bebis.BeBiS.item.domain;

import com.bebis.BeBiS.profile.domain.WowCharacter;

/**
 * Placeholder for sources that aren't modeled yet (drops, vendors, crafting, reputation). It restricts nobody, so an
 * item that is a quest reward and also has another source stays obtainable for characters the quest excludes.
 * <p>
 * Nothing creates it yet; replace it with concrete sources once they are exported.
 */
public record OtherSource() implements Item.Source {

    @Override
    public boolean allows(WowCharacter.Race race) {
        return true;
    }

    @Override
    public boolean allows(WowCharacter.WowClass wowClass) {
        return true;
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }
}
