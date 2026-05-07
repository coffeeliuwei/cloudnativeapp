# API Gateway Java-SDK Instruction
## 1 SDK Instruction

Welcome to use Alibabacloud API Gateway SDK. API Gateway's SDK automatically generates Java code according to your customized interface setting.

You could find JavaSDK on github: [by clicking](https://github.com/aliyun/apigateway-sdk-core)

To be noted that，all API documents are grouped by `RegionId`、`Group`。

* SDK Folder
	* sdk/{{regionId}}		`JavaSDK folder，contains all API for the Group`
		* SyncApiClient{{group}}.java	`Group's all API Sync call method`
		* AsyncApiClient{{group}}.java	`Group's all API A-Sync call method`
		* SyncDemo{{group}}.java	`Sync Call Demo`
		* AsyncDemo{{group}}.java	`A-Sync Call Demo`
	* doc/{{regionId}}
		* ApiDocument{{group}}.md	`Group's API Documents`
	* lib
		* sdk-core-java-1.2.3.jar `SDK Core`
		* sdk-core-java-1.2.3-sources.jar		`SDK Core's source code`
	* Readme.md	`Readme.md`
	* LICENSE ` License`




## 2 To Use SDK
### 2.1 Environment Setup

 1. Alibabacloud API Gateway for Java SDK support`JDK 1.6` and above.
 2. You need a pair of AppKey and AppSecret to be used by SDK
 3. Add dependency in pom.xml:

```html
<dependency>
	<groupId>com.aliyun.api.gateway</groupId>
	<artifactId>sdk-core-java</artifactId>
	<version>1.2.3</version>
</dependency>
<dependency>
		<groupId>com.fasterxml.jackson.core</groupId>
		<artifactId>jackson-databind</artifactId>
		<version>2.6.7.5</version>
</dependency>
```


### 2.2 Include SDK's API class into your project

1. Copy `AsyncApiClient*.java` and `SyncApiClient*.java` into your project's folder.
2. Modify the package in the classes；


### 2.3 Create ApiClient
Please refer to the demo code in `(A)SyncDemo*.java`.
Call `newBuilder()` to create a `ApiClientBuilder` object to construct a `ApiClient`：

```
public AsyncDemo{{group}}() {
    this.asyncClient = AsyncApiClient{{group}}.newBuilder()
        .appKey("your app key here")
        .appSecret("your app secret here")
        .build();
}
```


### 2.4 Call API Interface

SDK is generated according to API Gateway's setting. Each API is encapsulated as method.

#3. Advance use case
`sdk-core-java-1.2.3` use ApacheHttpClient_5.3.1 as the client to support advance use case.

### 3.1 Comprehensive ApacheHttpClient setting
[ApacheHttpClient Doc](https://hc.apache.org/httpcomponents-client-4.5.x/tutorial/html/index.html)
[HttpClientBuilder](http://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/impl/client/HttpClientBuilder.html)，in`ApiClientBuilder`，call `builder.setExtraParam("apache.httpclient.builder", ${apacheBuilder})` to pass all parameters in `HttpClientBuilder` to `ApiClientBuilder`。

```
HttpClientBuilder apacheHttpClientBuilder = HttpClientBuilder.create()
    .setHttpProcessor(new MyHttpProcessor())
    .setDefaultRequestConfig(
        RequestConfig.custom()
            .setConnectTimeout(5000)
            .build())
    .disableAuthCaching();

SyncApiClient{{group}} syncClient = SyncApiClient{{group}}.newBuilder()
    .appKey("your app key here")
    .appSecret("your app secret here")
    .connectionTimeoutMillis(10000L) //this will overwrite 5000 to 10000
    .setExtParams("apache.httpclient.builder", apacheHttpClientBuilder)
    .build();
```

### 3.2 Customized HttpClient
API Gateway supports customized HttpClient e.g. OKHttp3 etc. It is only needed to program to realize `com.alibaba.cloudapi.sdk.core.HttpClient` methods.

When calling builder, `setExtParams` could be used to pass customized parameters.
These customized parameters could be passed by calling`HttpClient`'s '`init()`method. Please refer to `com.alibaba.cloudapi.sdk.core.http.ApacheHttpClient`.
If customized httpclient is decided，please pass `-Daliyun.sdk.httpclient="${class}"`.
`${class}`is your `HttpClient`is the full path including name space。
> `-Daliyun.sdk.httpclient` default value is `"com.alibaba.cloudapi.sdk.core.http.ApacheHttpClient"`

```java
import com.alibaba.cloudapi.sdk.core.HttpClient

public class MyHttpClient extends HttpClient{

    private CustomHttpClient customHttpClient;

    @Override
    protected void init(BuilderParams builderParams){
        // init your customHttpClient with params
        Object config1 = builderParams.getExtra("key1");
        Object config2 = builderParams.getExtra("key2");
        customHttpClient = new CustomHttpClient(config1, config2);
    }

    @Override
    public ApiResponse syncInvoke(ApiRequest request) throws IOException{
        // parse request
        CustomeHttpRequest httpRequest = parseToHttpRequest(request);

        // send http request
        CustomeHttpResponse httpResponse = customHttpClient.execute(httpRequest);

        // parse response
        return parseToApiResponse(httpResponse);
    }

    @Override
    public Future<ApiResponse> asyncInvoke(ApiRequest request, ApiCallBack callback){
        // do async
    }

    @Override
    public void shutdown(){
        // release your custom httpclient
        customHttpClient.shutdown();
    }
}
```
