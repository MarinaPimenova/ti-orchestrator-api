package com.wk.ti.upload.listener;

import com.wk.ti.rabbit.config.RabbitConfig;
import com.wk.ti.sse.SseEmitterRegistry;
import com.wk.ti.upload.event.UploadCompletedEvent;
import com.wk.ti.upload.event.UploadFailedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadResultListener {

    private final SseEmitterRegistry sseRegistry;
    private final MeterRegistry meterRegistry;

    @RabbitListener(queues = RabbitConfig.QUEUE_UPLOAD_COMPLETED)
    public void handleSuccess(UploadCompletedEvent event) {
        log.info("Import completed successfully for importId={}", event.uploadId());
        sseRegistry.sendAndComplete(event.uploadId(), event);
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_UPLOAD_FAIL)
    public void handleFailure(UploadFailedEvent event) {
        log.error("Import failed for importId={}. Cause: {}", event.uploadId(), event.reason());
        meterRegistry.counter("upload.failed.count", "reason", event.reason()).increment();
        sseRegistry.sendAndComplete(event.uploadId(), event);
    }
}
