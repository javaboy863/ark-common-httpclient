package com.ark.common.http;

import com.ark.common.http.http.HttpClientUtil;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * @Author: tingwei.yuan
 * @Description:
 * @DateTime: 2022/11/14 11:57
 **/
public class HttpClientTest {

    @Test
    public void testGet1() {
        try {
            String result = HttpClientUtil.reqGet("http://www.baidu.com");
            System.out.println("testGet1 返回结果：" + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    * @Description:  url带参数：url?platformId=20000&w=xAAAAXoq&token=4a9b0540-7a91-4f2b-935d-c0ee1410cb8b
    **/
    @Test
    public void testCatNum1() {
        try {
            String url = "http://www.baidu.com?" +
                    "a=20000&w=xAAAAXoq&b=4a9b0540-7a91-4f2b-935d-c0ee1410cb8b";
            String result = HttpClientUtil.reqGet(url);
            System.out.println("testcatNum 返回结果：" + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    * @Author: tingwei.yuan
    * @Description:  url不带参数，请求参数另外设置
    * @Return:
    **/
    @Test
    public void testCatNum2() {
        try {
            String url = "http://www.baidu.com";
            Map<String, Object> params = new HashMap<>();
            params.put("a", 6403);
            params.put("w", "xAAAAXoq");
            params.put("b","4a9b0540-7a91-4f2b-935d-c0ee1410cb8b");
            String result = HttpClientUtil.reqGet(url, params);
            System.out.println("testcatNum 返回结果：" + result);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
    * @Description:  json格式请求参数
    **/
    @Test
    public void testPost1() {
        try {
            String url = "http://www.baidu.com";
            String data ="{\"pageNumber\":1,\"pageSize\":100}";
            String result = HttpClientUtil.reqPost(url, data, "application/json");
            System.out.println("testPost1 返回结果：" + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    * @Description: 表单格式请求参数
    **/
    @Test
    public void testPost3() {
        try {
            String url = "http://www.baidu.com";
            Map<String, Object> params = new HashMap<>();
            params.put("pfid", 20000);
            String result = HttpClientUtil.reqPost(url, params);
            System.out.println("testPost3 返回结果：" + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
