/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.datax.plugin.reader.httpreader;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * http utils
 */
public class HttpUtils {
	
	
	public static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

	/**
	 * get http request content
	 * @param url url
	 * @return http get request response content
	 */
	public static String get(String url){
		CloseableHttpClient httpclient = HttpClients.createDefault();

		HttpGet httpget = new HttpGet(url);
		/** set timeout、request time、socket timeout */
		/*
		* Constants.HTTP_CONNECT_TIMEOUT = 60 * 60 * 1000
		* Constants.HTTP_CONNECTION_REQUEST_TIMEOUT = 60 * 60 * 1000
		* Constants.SOCKET_TIMEOUT = 60 * 60 * 1000
		* */
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(Constants.HTTP_CONNECT_TIMEOUT)
				.setConnectionRequestTimeout(Constants.HTTP_CONNECTION_REQUEST_TIMEOUT)
				.setSocketTimeout(Constants.SOCKET_TIMEOUT)
				.setRedirectsEnabled(true)
				.build();
		httpget.setConfig(requestConfig);
		String responseContent = null;
		CloseableHttpResponse response = null;

		try {
			response = httpclient.execute(httpget);
			//check response status is 200
			if (response.getStatusLine().getStatusCode() == 200) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					/*
					* Constants.UTF_8 = = "UTF-8"
					* */
					responseContent = EntityUtils.toString(entity, Constants.UTF_8);
				}else{
					logger.warn("http entity is null");
				}
			}else{
				logger.error("http get:{} response status code ", response.getStatusLine().getStatusCode());
			}
		}catch (Exception e){
			logger.error(e.getMessage(),e);
		}finally {
			try {
				if (response != null) {
					EntityUtils.consume(response.getEntity());
					response.close();
				}
			} catch (IOException e) {
				logger.error(e.getMessage(),e);
			}

			if (!httpget.isAborted()) {
				httpget.releaseConnection();
				httpget.abort();
			}

			try {
				httpclient.close();
			} catch (IOException e) {
				logger.error(e.getMessage(),e);
			}
		}
		return responseContent;
	}
	/**
	 * 发送HttpPost请求，参数为map * * @param url * 请求地址 * @param map * 请求参数 * @return 返回字符串
	 */
	public static String sendFromPost(String url, Map<String, Object> map) {
		CloseableHttpClient httpclient = HttpClients.createDefault();

		HttpPost httpPost = new HttpPost(url);
		/** set timeout、request time、socket timeout */
		/*
		 * Constants.HTTP_CONNECT_TIMEOUT = 60 * 60 * 1000
		 * Constants.HTTP_CONNECTION_REQUEST_TIMEOUT = 60 * 60 * 1000
		 * Constants.SOCKET_TIMEOUT = 60 * 60 * 1000
		 * */
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(Constants.HTTP_CONNECT_TIMEOUT)
				.setConnectionRequestTimeout(Constants.HTTP_CONNECTION_REQUEST_TIMEOUT)
				.setSocketTimeout(Constants.SOCKET_TIMEOUT)
				.setRedirectsEnabled(true)
				.build();
		httpPost.setConfig(requestConfig);
		String result = null;
		CloseableHttpResponse response = null;
		// 设置参数
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
		}
		// 编码
		UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
		// 参数放入Entity
		httpPost.setEntity(formEntity);
		try {
			// 执行post请求
			response = httpclient.execute(httpPost);
			// 得到entity
			HttpEntity entity = response.getEntity();
			// 得到字符串
			result = EntityUtils.toString(entity,Charset.forName("UTF-8"));
		} catch (IOException e) {
			logger.error(e.getMessage());
		} finally {
			if (response != null) {
				try {
					response.close();
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			}
		}
		return result;
	}

	/**
	 * 发送post请求
	 * @param url  路径
	 * @param jsonObject  参数(json类型)
	 * @return
	 * @throws IOException
	 */
	public static String sendJsonPost(String url, JSONObject jsonObject )  {
		String body = "";

		//创建httpclient对象
		CloseableHttpClient client = HttpClients.createDefault();
		//创建post方式请求对象
		HttpPost httpPost = new HttpPost(url);

		//装填参数
		StringEntity s = new StringEntity(jsonObject.toString(), "utf-8");
		s.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,
				"application/json"));
		//设置参数到请求对象中
		httpPost.setEntity(s);

		//设置header信息
		//指定报文头【Content-type】、【User-Agent】
//        httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
		httpPost.setHeader("Content-type", "application/json");
		httpPost.setHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");

		//执行请求操作，并拿到结果（同步阻塞）
		CloseableHttpResponse response = null;
		try {
			response = client.execute(httpPost);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//获取结果实体
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			//按指定编码转换结果实体为String类型
			try {
				/*
				 * Constants.UTF_8 = = "UTF-8"
				 * */
				body = EntityUtils.toString(entity, Constants.UTF_8);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			EntityUtils.consume(entity);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//释放链接
		try {
			response.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return body;
	}
	public static void main(String[] args) {

//		List<Object> list = new ArrayList<>();
//		for (int i = 0; i < 5; i++) {
//			Map<String, Object> m = new HashMap<>();
//			m.put("col1",i);
//			m.put("col2",i*10);
//			list.add(m);
//		}
//		Map<String, Object> map = new HashMap<>();
//		String url  = "https://httpbin.org/post";
//		map.put("data","[{\"col1\":0,\"col2\":0},{\"col1\":1,\"col2\":10},{\"col1\":2,\"col2\":20},{\"col1\":3,\"col2\":30},{\"col1\":4,\"col2\":40}]");
//		String s = sendFromPost(url, map);
//		System.out.println(s);
//
//		map.put("data",list);
//		JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(map));
//		 s = sendJsonPost(url, jsonObject);
//		System.out.println(s);
////		JSONObject jsonObj = JSONObject.parseObject(s);
////		Object json1 = jsonObj.get("json");
////		JSONObject jsonObj1 = JSONObject.parseObject(json1.toString());
////		Object json2 = jsonObj1.get("data");
////		JSONArray jsonArray = (JSONArray) json2;
//
////		System.out.println(jsonArray);
////		for (Object o : jsonArray) {
////			for (Object entry : ((Map)o).entrySet()){
////				System.out.println(((Map.Entry)entry).getKey()  + "  " +((Map.Entry)entry).getValue());
////			}
////
////		}
//
//
//		// json.data
////		String[] split = "json.data".split("\\.");
////		JSONObject jsonObj = JSONObject.parseObject(s);
////
////		JSONObject jsonObj1 = jsonObj;
////		JSONArray jsonArray = null;
////		for (int i = 0; i < split.length; i++) {
////			System.out.println(i+"====>"+(split.length-1));
////			if(i< (split.length-1)){
////				jsonObj1 = JSONObject.parseObject(jsonObj1.get(split[i]).toString());
////			}else {
////				jsonArray = (JSONArray) jsonObj1.get(split[i].toString());
////			}
////		}
////		System.out.println("jsonArray:"+jsonArray.toJSONString());
//
//
//		 url  = "http://data.wuhan.gov.cn/api/portal/home/catalog-type/resource-list?type=1";
//		 s = get(url);
//		System.out.println(s);
//				String[] split = "data".split("\\.");
//		JSONObject jsonObj = JSONObject.parseObject(s,Feature.OrderedField);
//		JSONObject jsonObj1 = jsonObj;
//		JSONArray jsonArray = null;
//		for (int i = 0; i < split.length; i++) {
//			System.out.println(i+"====>"+(split.length-1));
//			if(i< (split.length-1)){
//				jsonObj1 = JSONObject.parseObject(jsonObj1.get(split[i]).toString());
//			}else {
//				jsonArray = (JSONArray) jsonObj1.get(split[i].toString());
//			}
//		}
//		System.out.println("jsonArray:"+jsonArray.toJSONString());
	}
}
