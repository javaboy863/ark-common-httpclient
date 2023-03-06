package com.ark.common.http.http;

import lombok.Data;
import org.apache.http.Header;

import java.util.Arrays;

/**
 **/
@Data
public class HttpResult {
    private Header[] headers;

    private String content;

    private int statusCode = 200;

    private String contentType;

    private boolean success;

    public String toString() {
        return "HttpResult [headers=" + Arrays.toString((Object[])this.headers) + ", content=" + this.content + ", statusCode=" + this.statusCode + ", contentType=" + this.contentType + ", success=" + this.success + "]";
    }
}
