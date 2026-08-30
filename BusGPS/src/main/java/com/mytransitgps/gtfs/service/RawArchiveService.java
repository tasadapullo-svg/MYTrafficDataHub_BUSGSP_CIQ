package com.mytransitgps.gtfs.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 原始响应归档服务，负责将未经修改的 Protobuf 字节可靠写入 RAW 文件。
 */
public class RawArchiveService {

    private static final Logger log = LoggerFactory.getLogger(RawArchiveService.class);

    public Path save(Path path, byte[] bytes) throws IOException {
        // RAW 层只保存原始响应字节，不做解析、修正或重新编码。
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
            log.info("RAW Protobuf文件保存完成，bytes={}，path={}", bytes == null ? 0 : bytes.length, path);
            return path;
        } catch (IOException ex) {
            log.error("RAW Protobuf文件保存失败，path={}，bytes={}，错误信息={}",
                    path, bytes == null ? 0 : bytes.length, ex.getMessage());
            throw ex;
        }
    }
}
