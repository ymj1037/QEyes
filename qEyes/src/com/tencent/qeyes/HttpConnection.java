/**
 * Http协议基类
 * Author: minjieyu
 * Date:2014/5.9
 * Version:1.0
 */
package com.tencent.qeyes;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.R.integer;
import android.util.Log;

public class HttpConnection {
	static final int CONNECTION_TIMEOUT = 3000;
	static final int SO_TIMEOUT = 5000;
	
	/**
	 * 上传图片
	 * @param url http上传地址
	 * @param filePath 上传文件路径
	 * @param uid 
	 * @return http response
	 */
	public String httpUpload(String url, String filePath, String uid) {		
		String response = null;
		HttpClient httpClient = new DefaultHttpClient();
		try
		{				   
			//设置通信协议版本				   
			//httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
			HttpParams params=httpClient.getParams(); 
			//设置超时参数
			HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(params, SO_TIMEOUT); 
			//post请求
			HttpPost httpPost = new HttpPost(url);
			File file = new File(filePath);
			if (!file.exists())
				return response;
			MultipartEntity mpEntity = new MultipartEntity(); //文件传输，模拟form表单
			ContentBody cbFile = new FileBody(file);
			mpEntity.addPart("uid", new StringBody(uid, 
                    		Charset.forName(org.apache.http.protocol.HTTP.UTF_8)));
			mpEntity.addPart("q_img", cbFile);
			httpPost.setEntity(mpEntity);
			//上传
			HttpResponse httpResponse = httpClient.execute(httpPost);
			
			Log.v("-Http-", "StatusCode: " + httpResponse.getStatusLine().getStatusCode());
			/*while (httpResponse.getStatusLine().getStatusCode() != 200 && flag < RETRY_TIMES)
			{
				Log.v("-Http-", "StatusCode: " + httpResponse.getStatusLine().getStatusCode());
				httpClient.getConnectionManager()..shutdown();
				httpResponse = httpClient.execute(httpPost);
				flag++;
			}*/
			if (httpResponse.getStatusLine().getStatusCode() == 200 )
			{
				response = EntityUtils.toString(httpResponse.getEntity());				
			}	
			httpClient.getConnectionManager().shutdown();
			return response;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return response;
		}
	}
	
	/**
	 * http get请求
	 * @param uri
	 * @return http response
	 */
	public String httpGetResponse(String uri) {
		String response = null;
		HttpClient httpClient = new DefaultHttpClient();	
		
		try
		{
			HttpGet get = new HttpGet(uri);
			HttpParams params=httpClient.getParams(); 
			HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(params, SO_TIMEOUT); 
			//执行get请求
			HttpResponse httpResponse = httpClient.execute(get);
			//HttpEntity entity = httpResponse.getEntity();
			/*while (httpResponse.getStatusLine().getStatusCode() != 200 && flag < retryTimes)
			{
				httpClient.getConnectionManager().shutdown();
				httpResponse = httpClient.execute(get);
				flag++;
			}*/
			if (httpResponse.getStatusLine().getStatusCode() == 200 )
			{
				response = EntityUtils.toString(httpResponse.getEntity());				
			}	
			httpClient.getConnectionManager().shutdown();
			return response;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return response;
		}
	}
	
	/**
	 * http post请求
	 * @param uri
	 * @param params post参数
	 * @return http response
	 */
	public String httpPostResponse(String uri, List<NameValuePair> params) {
		String response = null;
		HttpClient httpClient = new DefaultHttpClient();
		HttpPost post = new HttpPost(uri);
		try
		{
			HttpParams param=httpClient.getParams(); 
			HttpConnectionParams.setConnectionTimeout(param, CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(param, SO_TIMEOUT); 
			post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			HttpResponse httpResponse = httpClient.execute(post);

			/*while (httpResponse.getStatusLine().getStatusCode() != 200 && flag < RETRY_TIMES)
			{
				httpClient.getConnectionManager().shutdown();
				httpResponse = httpClient.execute(post);
				flag++;
			}*/
			if (httpResponse.getStatusLine().getStatusCode() == 200 )
			{
				response = EntityUtils.toString(httpResponse.getEntity());				
			}	
			httpClient.getConnectionManager().shutdown();
			return response;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return response;
		}
	}
}
