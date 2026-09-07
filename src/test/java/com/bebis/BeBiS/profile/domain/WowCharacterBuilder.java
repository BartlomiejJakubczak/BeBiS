package com.bebis.BeBiS.profile.domain;

public class WowCharacterBuilder {

    private WowCharacter.Id id = new WowCharacter.Id(1L, "soulseeker");
    private String name = "Thelamar";
    private int level = 60;
    private WowCharacter.Race race = WowCharacter.Race.NIGHT_ELF;
    private WowCharacter.WowClass wowClass = WowCharacter.WowClass.ROGUE;
    private WowRealm realm = new WowRealm("Soulseeker", "soulseeker");

    public static WowCharacterBuilder newWowCharacter() {
        return new WowCharacterBuilder();
    }

    public WowCharacterBuilder withId(long id, String realmSlug) {
        this.id = new WowCharacter.Id(id, realmSlug);
        return this;
    }

    public WowCharacterBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public WowCharacterBuilder withLevel(int level) {
        this.level = level;
        return this;
    }

    public WowCharacterBuilder withRace(WowCharacter.Race race) {
        this.race = race;
        return this;
    }

    public WowCharacterBuilder withWowClass(WowCharacter.WowClass wowClass) {
        this.wowClass = wowClass;
        return this;
    }

    public WowCharacterBuilder withRealm(WowRealm realm) {
        this.realm = realm;
        return this;
    }

    public WowCharacter build() {
        return new WowCharacter(id, name, level, race, wowClass, realm);
    }
}
