package com.mytransitgps.platform.collection;

/**
 * 所有交通数据采集器的统一端口。
 *
 * Scheduler只负责触发该端口，具体HTTP、解析、质量与持久化由各业务模块内部编排。
 */
public interface DataCollector {
    ModuleCode moduleCode();

    CollectorCode collectorCode();

    CollectionResult collect(CollectionContext context);
}
