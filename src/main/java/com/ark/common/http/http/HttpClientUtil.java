package com.ark.common.http.http;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.CharsetUtils;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @Description:  http请求工具类 支持https
**/
public class HttpClientUtil {

    private static String EMPTY_STR = "";
    private static RequestConfig requestConfig;
    private static final String HTTP_PREFIX = "HTTP";
    private static final String HTTP_HEADER = "HTTP:";
    private static CloseableHttpClient closeableHttpClient;
    private static final String HTTP_SPLIT= "//";

    public static final int CONN_TIMEOUT;//连接超时时间：指客户端和服务器建立连接的超时时间

    public static final int CONN_REQUEST_TIMEOUT;//指从连接池获取连接的超时时间

    public static final int SOCKET_TIMEOUT;//数据处理超时时间，指客户端从服务器读取数据的timeout，响应超时时间

//    public static final int KEEP_ALIVE; //连接空闲存活时间

    public static final int MAX_TOTAL_CONN;//连接池的最大连接数

    public static final int MAX_ROUTE_CONN;//每个路由的最大连接数

//    public static final int MAX_RETRY_COUNT;//重试次数

    public static final String DEFAULT_ENCODING;//默认字符编码

    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    static {
        PropertiesConfiguration config = null;
        try {
            config = new PropertiesConfiguration();
            config.setEncoding("UTF-8");
            config.load("httpclient.properties");
            config.setThrowExceptionOnMissing(false);
        } catch (ConfigurationException exc) {
            logger.error("", (Throwable)exc);
            config = new PropertiesConfiguration();
        }

        CONN_TIMEOUT = NumberUtils.toInt(config.getString("conn_timeout"), 10000);//默认10s
        CONN_REQUEST_TIMEOUT = NumberUtils.toInt(config.getString("conn_request_timeout"), 10000);//默认10s
        SOCKET_TIMEOUT = NumberUtils.toInt(config.getString("socket_timeout"), 10000);//默认10s
//        KEEP_ALIVE = NumberUtils.toInt(config.getString("keep_alive"), 60000 * 6);//默认永不过期 -1
        MAX_TOTAL_CONN = NumberUtils.toInt(config.getString("max_total_conn"), 800);//默认最大800
        MAX_ROUTE_CONN = NumberUtils.toInt(config.getString("max_route_conn"), 128);
//        MAX_RETRY_COUNT = NumberUtils.toInt(config.getString("max_retry_count"), 3);//默认3次
        DEFAULT_ENCODING = config.getString("default_encoding", "UTF-8");
        logger.debug("CONN_TIMEOUT_MS={}", Integer.valueOf(CONN_TIMEOUT));
        logger.debug("CONN_REQ_TIMEOUT_MS={}", Integer.valueOf(CONN_REQUEST_TIMEOUT));
        logger.debug("SOCKET_TIMEOUT_MS={}", Integer.valueOf(SOCKET_TIMEOUT));
//        logger.debug("KEEP_ALIVE_MS={}", Integer.valueOf(KEEP_ALIVE));
        logger.debug("TOTAL_CONN_MAX={}", Integer.valueOf(MAX_TOTAL_CONN));
        logger.debug("ROUTE_CONN_MAX={}", Integer.valueOf(MAX_ROUTE_CONN));
        logger.debug("DEFAULT_ENCODING={}", DEFAULT_ENCODING);

        //定义 http连接的 策略  可以允许 http和 https
        ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
        LayeredConnectionSocketFactory sslsf = createSSLConnSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder
                .<ConnectionSocketFactory> create().register("http", plainsf)
                .register("https", sslsf).build();

        //初始化http请求池
        PoolingHttpClientConnectionManager cm;
        cm = new PoolingHttpClientConnectionManager(registry);
        cm.setMaxTotal(MAX_TOTAL_CONN);//整个连接池最大连接数
        cm.setDefaultMaxPerRoute(MAX_ROUTE_CONN);//每路由最大连接数，默认值是2
        //连接存活策略
//        ConnectionKeepAliveStrategy connKeepAliveStrategy = new ConnectionKeepAliveStrategy() {
//            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
//                return HttpClientUtils.KEEP_ALIVE;
//            }
//        };
        //自定义重试策略，默认重试3次
//        HttpRequestRetryHandler thisRetryHandler = new HttpRequestRetryHandler() {
//            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
//                if (executionCount >= HttpClientUtils.MAX_RETRY_COUNT || exception instanceof java.io.InterruptedIOException || exception instanceof java.net.UnknownHostException || exception instanceof org.apache.http.conn.ConnectTimeoutException || exception instanceof javax.net.ssl.SSLException || exception instanceof javax.net.ssl.SSLHandshakeException)
//                    return false;
//                if (exception instanceof org.apache.http.NoHttpResponseException)
//                    return true;
//                HttpClientContext clientContext = HttpClientContext.adapt(context);
//                HttpRequest request = clientContext.getRequest();
//                boolean idempotent = !(request instanceof org.apache.http.HttpEntityEnclosingRequest);
//                if (idempotent)
//                    return true;
//                return false;
//            }
//        };

        closeableHttpClient = HttpClients
                .custom()
                .setConnectionManager(cm)
//                .setKeepAliveStrategy(connKeepAliveStrategy)
                .build();

        //初始化请求参数
        requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONN_TIMEOUT)//设置连接超时时间，单位毫秒
                .setConnectionRequestTimeout(CONN_REQUEST_TIMEOUT)//设置从connect Manager获取Connection 超时时间，单位毫秒
                .setSocketTimeout(SOCKET_TIMEOUT).build();//请求获取数据的超时时间，单位毫秒。 如果访问一个接口，多少时间内无法返回数据
    }


    /**
     * 通过连接池获取HttpClient
     *
     * @return
     */
    private static CloseableHttpClient getHttpClient() {
        return closeableHttpClient;
    }

    public static String reqGet(String url) throws IOException {
        return reqGet(url, DEFAULT_ENCODING);
    }


    public static String reqGet(String url, String charSet) throws IOException {
        HttpGet httpGet = buildHttpGet(url);
        return getResult(httpGet, charSet);
    }

    public static String reqGet(String url, Map<String, Object> params) throws URISyntaxException, IOException {
        return reqGet(url, params, DEFAULT_ENCODING);
    }

    public static String reqGet(String url, Map<String, Object> params,List<BasicClientCookie> cookieList) throws URISyntaxException, IOException {
        return reqGet(url, params, DEFAULT_ENCODING,cookieList);
    }

    public static String reqGet(String url, Map<String, Object> params, String charSet) throws URISyntaxException, IOException {
        return reqGet(url, params, charSet, null);
    }

    public static String reqGet(String url, Map<String, Object> params, String charSet, List<BasicClientCookie> cookieList) throws URISyntaxException, IOException {
        URIBuilder ub = new URIBuilder(processURL(url));

        ArrayList<NameValuePair> pairs = covertParams2NVPS(params);
        ub.setParameters(pairs);

        HttpGet httpGet = new HttpGet(ub.build());

        //处理cookie
        HttpContext httpContext = processCookie(cookieList);

        return getResult(httpGet, charSet, httpContext);
    }

    public static String reqGet(String url, Map<String, Object> headers, Map<String, Object> params) throws URISyntaxException, IOException {
        return reqGet(url, headers, params, DEFAULT_ENCODING);
    }

    public static String reqGet(String url, Map<String, Object> headers, Map<String, Object> params, String charSet,List<BasicClientCookie> cookieList) throws URISyntaxException, IOException {
        URIBuilder ub = new URIBuilder(processURL(url));

        ArrayList<NameValuePair> pairs = covertParams2NVPS(params);
        ub.setParameters(pairs);

        HttpGet httpGet = new HttpGet(ub.build());
        for (Map.Entry<String, Object> param : headers.entrySet()) {
            httpGet.addHeader(param.getKey(), String.valueOf(param.getValue()));
        }

        //处理cookie
        HttpContext httpContext = processCookie(cookieList);

        return getResult(httpGet, charSet,httpContext);
    }

    public static String reqGet(String url, Map<String, Object> headers, Map<String, Object> params, String charSet) throws URISyntaxException, IOException {
        URIBuilder ub = new URIBuilder(processURL(url));
        ArrayList<NameValuePair> pairs = covertParams2NVPS(params);
        ub.setParameters(pairs);

        HttpGet httpGet = new HttpGet(ub.build());
        for (Map.Entry<String, Object> param : headers.entrySet()) {
            httpGet.addHeader(param.getKey(), String.valueOf(param.getValue()));
        }
        return getResult(httpGet, charSet);
    }

    public static String reqPost(String url) throws IOException {
        HttpPost httpPost = buildHttpPost(url);
        return getResult(httpPost, DEFAULT_ENCODING);
    }

    public static String reqPost(String url, Map<String, Object> params) throws IOException {
        return reqPost(url, params, DEFAULT_ENCODING);
    }

    public static String reqPost(String url, Map<String, Object> params, String charSet) throws IOException {
        HttpPost httpPost = buildHttpPost(url);
        ArrayList<NameValuePair> pairs = covertParams2NVPS(params);

        httpPost.setEntity(new UrlEncodedFormEntity(pairs, DEFAULT_ENCODING));
        return getResult(httpPost, charSet);
    }


    public static String reqPost(String url, String data, String contentType) throws IOException {
        return reqPost(url, data, contentType, DEFAULT_ENCODING);
    }


    public static String reqPost(String url, String data, String contentType, String charSet) throws IOException {
        HttpPost httpPost = buildHttpPost(url);
        StringEntity stringEntity = new StringEntity(data, ContentType.create(contentType, Consts.UTF_8));
        httpPost.setEntity(stringEntity);
        return getResult(httpPost, charSet);
    }

    public static String reqPost(String url, String data, String contentType, String charSet ,RequestConfig requestConfig) throws IOException {
        HttpPost httpPost = buildHttpPost(url);
        StringEntity stringEntity = new StringEntity(data, ContentType.create(contentType, Consts.UTF_8));
        httpPost.setEntity(stringEntity);
        return getResult(httpPost, charSet,requestConfig);
    }

    public static String reqPost(String url, Map<String, Object> headers, Map<String, Object> params) throws IOException {
        return reqPost(url, headers, params, DEFAULT_ENCODING);
    }

    public static String reqPost(String url, Map<String, Object> headers, Map<String, Object> params, List<BasicClientCookie> cookieList) throws IOException {
        return reqPost(url, headers, params, DEFAULT_ENCODING, cookieList);
    }

    public static String reqPost(String url, Map<String, Object> headers, Map<String, Object> params, String charSet) throws IOException {
        return reqPost(url, headers, params, charSet, null);
    }

    public static String reqPost(String url, Map<String, Object> headers, Map<String, Object> params, String charSet, List<BasicClientCookie> cookieList) throws IOException {
        HttpPost httpPost = buildHttpPost(url);

        for (Map.Entry<String, Object> param : headers.entrySet()) {
            httpPost.addHeader(param.getKey(), String.valueOf(param.getValue()));
        }

        ArrayList<NameValuePair> pairs = covertParams2NVPS(params);
        httpPost.setEntity(new UrlEncodedFormEntity(pairs, DEFAULT_ENCODING));

        //处理cookie
        HttpContext httpContext = processCookie(cookieList);

        return getResult(httpPost, charSet, httpContext);
    }

    private static ArrayList<NameValuePair> covertParams2NVPS(Map<String, Object> params) {
        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
        for (Map.Entry<String, Object> param : params.entrySet()) {
            pairs.add(new BasicNameValuePair(param.getKey(), String.valueOf(param.getValue())));
        }

        return pairs;
    }

    public static String reqPost(String url, Map<String, Object> params, String charSet,boolean isNullToString) throws IOException {
        HttpPost httpPost = buildHttpPost(url);
        ArrayList<NameValuePair> pairs = covertParams2NVPS(params,isNullToString);

        httpPost.setEntity(new UrlEncodedFormEntity(pairs, DEFAULT_ENCODING));
        return getResult(httpPost, charSet);
    }

    private static ArrayList<NameValuePair> covertParams2NVPS(Map<String, Object> params, boolean isNullToString) {
        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
        for (Map.Entry<String, Object> param : params.entrySet()) {
            Object object = param.getValue();
            if (object == null && !isNullToString) {
                pairs.add(new BasicNameValuePair(param.getKey(), null));
            } else {
                pairs.add(new BasicNameValuePair(param.getKey(), String.valueOf(param.getValue())));
            }
        }

        return pairs;
    }

    /**
     * 创建SSL/TLS安全连接
     *
     * @return
     */
    private static SSLConnectionSocketFactory createSSLConnSocketFactory(){
        SSLConnectionSocketFactory sslsf = null;
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            sslsf = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        } catch (Exception e){
            logger.error("异常", e);
        }
        return sslsf;
    }


    /**
     * 处理Http请求
     *
     * @param request
     * @return
     */
    private static String getResult(HttpRequestBase request, String charSet) throws IOException {
        return getResult(request, charSet, null, requestConfig);
    }


    /**
     * 处理Http请求
     *
     * @param request
     * @return
     */
    private static String getResult(HttpRequestBase request, String charSet, HttpContext httpContext) throws IOException {
        return getResult(request,charSet,httpContext,requestConfig);
    }


    /**
     * 处理Http请求
     *
     * @param request
     * @return
     */
    private static String getResult(HttpRequestBase request, String charSet, RequestConfig requestConfig) throws IOException {
        return getResult(request, charSet, null, requestConfig);
    }


    /**
     * 处理Http请求
     *
     * @param request
     * @return
     */
    private static String getResult(HttpRequestBase request, String charSet, HttpContext httpContext, RequestConfig requestConfig) throws IOException {
        request.setConfig(requestConfig);
        String result = EMPTY_STR;
        CloseableHttpClient httpClient = getHttpClient();
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request,httpContext);
            result = wrapResponsetoStr(response,Charset.forName(charSet));
        } catch (Exception e) {
            logger.error("http调用异常, e" ,e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return result;
    }

    private static HttpResult getResultObj(HttpRequestBase request, String charSet, HttpContext httpContext, RequestConfig requestConfig) throws IOException {
        request.setConfig(requestConfig);
        CloseableHttpClient httpClient = getHttpClient();
        HttpResult httpResult = null;
        try (CloseableHttpResponse response = httpClient.execute(request,httpContext)) {
            httpResult = wrapResponsetoResult((HttpResponse)response, Charset.forName(charSet));
        }
        return httpResult;
    }

    private static HttpPost buildHttpPost(String url) {
        return new HttpPost(processURL(url));
    }

    private static HttpGet buildHttpGet(String url) {
        return new HttpGet(processURL(url));
    }

    private static String processURL(String url) {
        if (StringUtils.isBlank(url)) {
            return url;
        }
        String upperUrl = url.toUpperCase();
        if (upperUrl.startsWith(HTTP_PREFIX)) {
            return url;
        } else if (upperUrl.startsWith(HTTP_SPLIT)) {
            return HTTP_HEADER + url;
        } else {
            return HTTP_HEADER + HTTP_SPLIT + url;
        }
    }

    private static HttpContext processCookie(List<BasicClientCookie> cookieList) {
        HttpContext httpContext = null;
        if (!CollectionUtils.isEmpty(cookieList)) {
            httpContext = new BasicHttpContext();
            BasicCookieStore cookieStore = new BasicCookieStore();
            BasicClientCookie[] array = new BasicClientCookie[cookieList.size()];
            cookieStore.addCookies(cookieList.toArray(array));
            httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
        }
        return httpContext;
    }


    /**
     * 发送 post请求（带文件）
     * @create by zoran.huang
     * @param url 地址
     * @param fileParams 附件
     *
     */
    public static String httpPostFileRequest(String url, Map<String,File> fileParams) throws Exception{
        return httpPostFileRequest(url,null,null,fileParams);
    }
    /**
     * 发送 post请求（带文件）
     * @create by zoran.huang
     * @param url 地址
     * @param maps 参数
     * @param fileParams 附件
     *
     */
    public static  String httpPostFileRequest(String url, Map<String, Object> maps, Map<String,File> fileParams) throws Exception{
        return httpPostFileRequest(url,null,maps,fileParams);
    }
    /**
     * 发送 post请求（带文件）
     * @create by zoran.huang
     * @param url 地址
     * @param maps 参数
     * @param headers 头信息
     * @param fileParams 附件
     *
     */
    public static String httpPostFileRequest(String url,Map<String, Object> headers, Map<String, Object> maps, Map<String,File> fileParams) throws Exception{
        HttpPost httpPost = new HttpPost(url);// 创建httpPost
        MultipartEntityBuilder meBuilder = MultipartEntityBuilder.create();
        meBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        meBuilder.setCharset(CharsetUtils.get("UTF-8"));
        if(maps != null ){
            for (String key : maps.keySet()) {
                meBuilder.addPart(key, new StringBody(String.valueOf(maps.get(key)), ContentType.TEXT_PLAIN));
            }
        }
        if (headers != null ){
            for (Map.Entry<String, Object> param : headers.entrySet()) {
                httpPost.addHeader(param.getKey(), String.valueOf(param.getValue()));
            }
        }

        if (fileParams != null ){
            for(Map.Entry<String, File>  file : fileParams.entrySet()) {
                FileBody fileBody = new FileBody(file.getValue());
                meBuilder.addPart(file.getKey(), fileBody);
            }
        }
        HttpEntity reqEntity = meBuilder.build();
        httpPost.setEntity(reqEntity);
        return getResult(httpPost,DEFAULT_ENCODING);
    }


    public static HttpPost getPost(String url, Map<String,Object> params) throws Exception{
        HttpPost httpPost = buildHttpPost(url);// 创建httpPost
        ArrayList<NameValuePair> pairs = covertParams2NVPS(params);
        httpPost.setEntity(new UrlEncodedFormEntity(pairs, DEFAULT_ENCODING));

        return httpPost;

    }

    private static String getJsonString(String resultStr,String callback) {
        if(StringUtils.isNotEmpty(resultStr) && StringUtils.isNotEmpty(callback)){
            int index = callback.indexOf(callback);
            if(index>-1 && resultStr.length()>callback.length()){
                resultStr = resultStr.substring(callback.length()+1,resultStr.length()-1);
            }
        }
        return resultStr;
    }

    private static String wrapResponsetoStr(HttpResponse response, Charset charSet) throws ParseException, IOException {
        if (response == null) {
            return EMPTY_STR;
        }
        HttpEntity entity = response.getEntity();
        String content = (entity != null) ? EntityUtils.toString(entity, charSet) : EMPTY_STR;
        return content;
    }

    private static HttpResult wrapResponsetoResult(HttpResponse response, Charset charSet) throws ParseException, IOException {
        HttpResult httpResult = new HttpResult();
        if (response == null)
            return httpResult;
        int statusCode = response.getStatusLine().getStatusCode();
        httpResult.setStatusCode(statusCode);
        httpResult.setSuccess((statusCode >= 200 && statusCode < 300));
        Header[] headers = response.getAllHeaders();
        httpResult.setHeaders(headers);
        HttpEntity entity = response.getEntity();
        String contentType = (entity.getContentType() == null) ? null : entity.getContentType().getValue();
        String content = (entity != null) ? EntityUtils.toString(entity, charSet) : null;
        httpResult.setContentType(contentType);
        httpResult.setContent(content);
        if (logger.isDebugEnabled()) {
            logger.debug("RESP_LINE={}", response);
            for (Header header : headers)
                logger.debug("HEADER={}:{}", header.getName(), header.getValue());
        }
        return httpResult;
    }

    /**
    * @Author: tingwei.yuan
    * @Description:  http请求头部数据组装
    * @Datetime: 2022/11/10 20:07
    * @Params: [cookieMap, headerMap, contentType]
    * @Return:
    **/
    public Header[] genHeaderArray(Map<String, String> cookieMap, Map<String, String> headerMap, ContentType contentType) {
        List<Header> headerList = new ArrayList<>();
        String encoding = contentType.getCharset().name();
        if (cookieMap != null && !cookieMap.isEmpty())
            headerList.add(new BasicHeader("Cookie", toCookieString(cookieMap)));
        if (headerMap != null && !headerMap.isEmpty())
            for (Map.Entry<String, String> entry : headerMap.entrySet())
                headerList.add(new BasicHeader(entry.getKey(), entry.getValue()));
        headerList.add(new BasicHeader("Accept", "text/html, application/xhtml+xml, */*"));
        headerList.add(new BasicHeader("Accept-Encoding", "gzip, deflate"));
        headerList.add(new BasicHeader("Accept-Language", "zh-CN"));
        headerList.add(new BasicHeader("Content-Type", contentType.getMimeType() + ((encoding == null || encoding.isEmpty()) ? "" : ("; charset=" + encoding.trim()))));
        headerList.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0) like Gecko"));
        headerList.add(new BasicHeader("Connection", "Keep-Alive"));
        return headerList.<Header>toArray(new Header[0]);
    }

    public String toCookieString(Map<String, String> cookieMap) {
        if (cookieMap == null || cookieMap.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookieMap.entrySet())
            sb.append((String)entry.getKey() + "=" + (String)entry.getValue() + ";");
        return sb.toString();
    }
}
