package com.mytransitgps.gtfs.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON 输出服务，统一使用稳定格式和 UTF-8 编码写出结构化数据。
 */
public class JsonOutputService {

    private static final Logger log = LoggerFactory.getLogger(JsonOutputService.class);
    private final ObjectMapper objectMapper;

    public JsonOutputService() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Path writeJsonString(Path path, String json) throws IOException {
        // 所有 JSON 明确使用 UTF-8，并统一补一个行尾，便于跨平台查看和校验。
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, json + System.lineSeparator(), StandardCharsets.UTF_8);
            log.info("JSON证据文件保存完成，path={}，chars={}", path, json == null ? 0 : json.length());
            return path;
        } catch (IOException ex) {
            log.error("JSON证据文件保存失败，path={}，错误信息={}", path, ex.getMessage());
            throw ex;
        }
    }

    public Path writeJsonObject(Path path, Object payload) throws IOException {
        // 对象输出统一走同一个 ObjectMapper，避免不同调用点产生格式差异。
        try {
            Files.createDirectories(path.getParent());
            String json = objectMapper.writeValueAsString(payload);
            Files.writeString(path, json + System.lineSeparator(), StandardCharsets.UTF_8);
            log.info("JSON对象证据文件保存完成，path={}，chars={}", path, json.length());
            return path;
        } catch (IOException ex) {
            log.error("JSON对象证据文件保存失败，path={}，错误信息={}", path, ex.getMessage());
            throw ex;
        }
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }
}
