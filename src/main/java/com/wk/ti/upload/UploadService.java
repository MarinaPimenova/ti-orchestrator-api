package com.wk.ti.upload;

import com.wk.ti.job.AbstractFileProcessingService;
import com.wk.ti.job.config.FileProcessingProperties;
import com.wk.ti.job.model.FileProcessingType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class UploadService extends AbstractFileProcessingService {

    private final FileProcessingProperties properties;

    public UploadService(
            RabbitTemplate rabbitTemplate,
            FileProcessingProperties properties) {

        super(rabbitTemplate);
        this.properties = properties;
    }

    @Override
    protected String storageDirectory() {
        return properties.getUploadFlow().getStoragePath();
    }

    @Override
    protected String exchangeName() {
        return properties.getUploadFlow().getExchange();
    }

    @Override
    protected String requestRoutingKey() {
        return properties.getUploadFlow().getRequestRoutingKey();
    }

    @Override
    protected FileProcessingType fileProcessingType() {
        return FileProcessingType.UPLOAD;
    }
}