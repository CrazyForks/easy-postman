package com.laker.postman.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class CurlRequest {
    public String url;
    public String method;
    public Map<String, String> headers = new LinkedHashMap<>();
    public String body;
    public Map<String, String> params = new LinkedHashMap<>();
    // 新增字段，用于存储解析出的表单数据
    public Map<String, String> formData = new LinkedHashMap<>();
    // 新增字段，用于存储解析出的文件信息（键为字段名，值为文件名）
    public Map<String, String> formFiles = new LinkedHashMap<>();
    public boolean followRedirects = false; // 是否跟随重定向
}