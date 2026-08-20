package com.wk.ti.event;

import java.io.Serializable;

public record ImportFailedEvent(
        String importId,
        String reason
) implements Serializable {}
