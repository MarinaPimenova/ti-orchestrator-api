package com.wk.ti.job.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "ti.file-processing")
@Component
public class FileProcessingProperties {

    private Flow importFlow;
    private Flow uploadFlow;

    @Data
    public static class Flow {
        private String storagePath;
        private String exchange;
        private String requestRoutingKey;
    }
}
