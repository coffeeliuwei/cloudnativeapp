//
//  Created by  fred on 2017/1/12.
//  Copyright © 2016年 Alibaba. All rights reserved.
//

package com.alibaba.cloudapi.client;
import com.alibaba.cloudapi.sdk.client.ApacheHttpClient;
import com.alibaba.cloudapi.sdk.enums.Scheme;
import com.alibaba.cloudapi.sdk.enums.HttpMethod;
import com.alibaba.cloudapi.sdk.model.ApiRequest;
import com.alibaba.cloudapi.sdk.model.ApiResponse;
import com.alibaba.cloudapi.sdk.model.ApiCallback;
import com.alibaba.cloudapi.sdk.model.HttpClientBuilderParams;
import com.alibaba.cloudapi.sdk.enums.ParamPosition;
import com.alibaba.cloudapi.sdk.enums.WebSocketApiType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class HttpApiClient用户中心 extends ApacheHttpClient{
    public final static String HOST = "de14143ebe0640eb92103d3005ba23bb-cn-hangzhou.alicloudapi.com";
    static HttpApiClient用户中心 instance = new HttpApiClient用户中心();
    public static HttpApiClient用户中心 getInstance(){return instance;}
    public static final ObjectMapper mapper = new ObjectMapper();

    public void init(HttpClientBuilderParams httpClientBuilderParams){
        httpClientBuilderParams.setScheme(Scheme.HTTP);
        httpClientBuilderParams.setHost(HOST);
        super.init(httpClientBuilderParams);
    }




    public void userInfo(String name , ApiCallback callback) {
        String path = "/user/userInfo/[name]";
        ApiRequest request = new ApiRequest(HttpMethod.GET , path);
        request.addParam("name" , name , ParamPosition.PATH , true);



        sendAsyncRequest(request , callback);
    }

    public ApiResponse userInfoSyncMode(String name) {
        String path = "/user/userInfo/[name]";
        ApiRequest request = new ApiRequest(HttpMethod.GET , path);
        request.addParam("name" , name , ParamPosition.PATH , true);



        return sendSyncRequest(request);
    }

}