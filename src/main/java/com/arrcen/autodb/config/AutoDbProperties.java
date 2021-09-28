package com.arrcen.autodb.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: wangziheng
 * @Date: 2021/3/8
 */
@Component
@ConfigurationProperties(prefix = "hip.auto")
@MapperScan("com.arrcen.*.dao")
public class AutoDbProperties {
    private boolean enabled = false;
    private String[] modelPath;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String[] getModelPath() {
        return modelPath;
    }

    public void setModelPath(String[] modelPath) {
        this.modelPath = modelPath;
    }

}
