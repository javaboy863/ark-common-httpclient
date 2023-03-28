# 1.什么是ark-common-httpclient？
&emsp;&emsp;ark-common-httpclient是ark系列框架中的负责发起http请求的组件。
# 2.ark-common-httpclient解决了什么问题？
&emsp;&emsp;在互联网系统中，服务之间的调用除了dubbo ,thrift等rpc调用之外，服务之间还会存在有http调用的情况，例如http调用第三方服务、http调用内部服务等等，各个服务写自己的http调用工具，造成规范不统一、开发效率低，因此需要抽象出统一的http通用组件

# 3.设计目标
- 构建轻量级http通用组件，统一规范，提升研发效率
# 4.功能列表
```
（1）http连接池可配置化
（2）提供标准的rest风格接口服务
        a.get请求：请求服务器获取指定资源，查询/获取数据操作
        b.post请求：请求服务器向指定资源提交数据，修改/更新/插入操作
        c.put请求：请求服务器存储资源，新增/插入操作
        d.delete请求：请求服务器删除指定资源
（3）提供的http方法get/post
         例如：
          HttpClientUtil.reqGet(String url)
         HttpClientUtil.reqPost(String url,Map<String,Object> param)
```
# 5.ark-common-httpclient组件如何使用？

```
a）pom文件中添加mavne依赖：
    <dependency>
    <groupId>com.ark.common</groupId>
    <artifactId>ark-common-httpclient</artifactId>
    <version>1.0</version>
    </dependency>
    
b）连接池参数配置：httpclient.properties，如果不配置有默认值

conn_timeout=8000  //连接超时时间，指客户端和服务器建立连接的超时时间
conn_request_timeout=10000 //从连接池获取连接的超时时间
socket_timeout=3000   //响应超时时间，指客户端从服务器读取数据的超时时间
max_total_conn=1000  //最大连接数

c）测试例子
可参考测试用例，如：
HttpClientUtil.reqGet("http://www.baidu.com");


```