package com.wk.ti.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterRegistry {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String importId) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3-minute timeout
        emitters.put(importId, emitter);

        emitter.onCompletion(() -> emitters.remove(importId));
        emitter.onTimeout(() -> emitters.remove(importId));
        emitter.onError((e) -> emitters.remove(importId));

        return emitter;
    }

    public void sendAndComplete(String importId, Object data) {
        SseEmitter emitter = emitters.remove(importId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("import-result").data(data));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }
    }
}
