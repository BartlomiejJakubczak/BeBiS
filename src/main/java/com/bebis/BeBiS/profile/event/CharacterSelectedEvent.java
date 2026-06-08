package com.bebis.BeBiS.profile.event;

import com.bebis.BeBiS.profile.domain.CharacterInfo;

import java.time.Instant;

public record CharacterSelectedEvent(CharacterInfo characterInfo, Instant timestamp) {
}
