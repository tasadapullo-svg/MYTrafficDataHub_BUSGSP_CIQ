package com.mytransitgps.modules.ciq.config;

/** CIQ 配置与运行前状态，用于 Dashboard 和长期运行前检查。 */
public enum CiqConfigurationStatus {
    DISABLED,
    NOT_CONFIGURED,
    COLLECTOR_DISABLED,
    RAW_READY_DATABASE_SCOPE_PENDING,
    READY,
    NORMAL,
    WARNING,
    ERROR
}
