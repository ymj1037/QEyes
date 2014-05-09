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
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

/**
 * Http协议基类
 */

public class HttpConnection {
	
	public String httpUpload(String url, String filePath, String uid/*List<NameValuePair> params*/) {
		HttpClient httpClient = new DefaultHttpClient();
		String response = "";
		try
		{				   
			//设置通信协议版本				   
			httpClient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
				   
			HttpPost httpPost = new HttpPost(url);
			File file = new File(filePath);
			if (!file.exists())
				return response;
			MultipartEntity mpEntity = new MultipartEntity(); //文件传输
			ContentBody cbFile = new FileBody(file);
			mpEntity.addPart(
                    "uid",
                    new StringBody(uid, Charset
                                    .forName(org.apache.http.protocol.HTTP.UTF_8)));

			mpEntity.addPart("q_img", cbFile); 

			httpPost.setEntity(mpEntity);
			System.out.println("executing request " + httpPost.getRequestLine());
				   
			HttpResponse httpResponse = httpClient.execute(httpPost);

			if (httpResponse.getStatusLine().getStatusCode() == 200)
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
	
	public void httpDownload(String url, String filePath) {
		
	}
	
	public String httpGetResponse(String uri) {
		HttpClient httpClient = new DefaultHttpClient();
		String response = "";
		try
		{
			HttpGet get = new HttpGet(uri);
			HttpResponse httpResponse = httpClient.execute(get);
			//HttpEntity entity = httpResponse.getEntity();
			if (httpResponse.getStatusLine().getStatusCode() == 200)
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
	
	public String httpPostResponse(String uri, List<NameValuePair> params) {
		HttpClient httpClient = new DefaultHttpClient();
		String response = "";
		HttpPost post = new HttpPost(uri);
		try
		{
			post.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			HttpResponse httpResponse = httpClient.execute(post);
			if (httpResponse.getStatusLine().getStatusCode() == 200)
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
