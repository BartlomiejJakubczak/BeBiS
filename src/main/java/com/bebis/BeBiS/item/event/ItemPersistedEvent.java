package com.bebis.BeBiS.item.event;

import java.time.Instant;

public record ItemPersistedEvent(long baseId, long suffixId, Instant timestamp) {
}
