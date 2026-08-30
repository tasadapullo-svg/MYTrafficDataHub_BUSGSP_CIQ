package com.mytransitgps.modules.ciq.storage;

/**
 * CIQ 八个数据接口的统一代码与本地目录名称。
 *
 * <p>该枚举只负责文件目录标识，不代表接口已经完成业务实现；当前正式实现范围仍由各 Collector 开关控制。
 */
public enum CiqApiCode {
    API01("API01_TrafficSpeedBands"),
    API02("API02_EstimatedTravelTimes"),
    API03("API03_TrafficIncidents"),
    API04("API04_VMS_EMAS"),
    API05("API05_FaultyTrafficLights"),
    API06("API06_ApprovedRoadWorks"),
    API07("API07_TrafficFlow"),
    API08("API08_PlannedRoadOpenings");

    private final String folderName;

    CiqApiCode(String folderName) {
        this.folderName = folderName;
    }

    public String folderName() {
        return folderName;
    }
}
