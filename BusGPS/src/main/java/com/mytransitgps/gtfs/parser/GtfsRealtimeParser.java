package com.mytransitgps.gtfs.parser;

import java.io.IOException;

import com.google.protobuf.util.JsonFormat;
import com.google.transit.realtime.GtfsRealtime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GTFS-Realtime Protobuf 解析器，将原始字节转换为官方 FeedMessage 对象。
 */
public class GtfsRealtimeParser {

    private static final Logger log = LoggerFactory.getLogger(GtfsRealtimeParser.class);

    public GtfsRealtime.FeedMessage parse(byte[] protobufBytes) throws IOException {
        // 直接解析原始响应字节，不在解析层修正或过滤上游字段。
        try {
            GtfsRealtime.FeedMessage message = GtfsRealtime.FeedMessage.parseFrom(protobufBytes);
            log.info("GTFS-Realtime Protobuf解析完成，payloadBytes={}，entityCount={}，vehicleEntityCount={}",
                    protobufBytes == null ? 0 : protobufBytes.length, message.getEntityCount(),
                    message.getEntityList().stream().filter(GtfsRealtime.FeedEntity::hasVehicle).count());
            return message;
        } catch (IOException ex) {
            log.error("GTFS-Realtime Protobuf解析失败，payloadBytes={}，错误信息={}",
                    protobufBytes == null ? 0 : protobufBytes.length, ex.getMessage());
            throw ex;
        }
    }

    public String toJson(GtfsRealtime.FeedMessage feedMessage) throws IOException {
        // 保留 Protobuf 原字段名，保证 JSON 与数据库字段映射可追溯。
        try {
            return JsonFormat.printer()
                    .preservingProtoFieldNames()
                    .print(feedMessage);
        } catch (IOException ex) {
            log.error("GTFS-Realtime转换parsed JSON失败，entityCount={}，错误信息={}",
                    feedMessage == null ? 0 : feedMessage.getEntityCount(), ex.getMessage());
            throw ex;
        }
    }
}
