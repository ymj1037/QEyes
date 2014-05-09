package com.tencent.qeyes;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Log;

/**
 * QEyes相关的Http协议类
 */

public class QEyesHttpConnection extends HttpConnection implements MsgType {
	private static final String SERVER_IP = "http://203.195.190.137";
	private static final String SERVER_URL = "http://203.195.190.137/mini/qEyes/index.php";
	private static final String UPLOAD_URL = "/client/question";
	private static final String TERMINATE_URL = "/client/terminateQues";
	private static final String CHECK_ANS_URL = "/client/checkAns";
	private static final String COMMENT_URL = "/client/comment";
	protected String uid;			//手机唯一标示
	protected int q_id;
	protected Handler UIHandler;
	
	public QEyesHttpConnection(String uid, Handler UIHandler) {
		this.uid = uid;
		q_id = 0;
		this.UIHandler = UIHandler;
	}

	public boolean httpUpload(String fileName) {
		// 向服务器上传图片	
		// 0:成功 并得到qid
		// 其他:会有相应的错误提示
		//return false;
		
		String url = SERVER_URL;
		url = url.concat(UPLOAD_URL);		
		Log.v("-Http-", "Send Upload Request : " + url);
		
		String response = httpUpload(url, fileName, uid);
		Log.v("-Http-", "Upload Response: " + response);
		
		if (response != "")
		{
			JSONObject json = null;
			try {
				json = new JSONObject(response);
				int ret = json.getInt("ret");
				String msg = json.getString("msg");	
				if (ret == 0)
				{			
					q_id = json.getInt("q_id");
					Log.v("-Http-", "Upload Success with qid : " + q_id);
					return true;					
				}
				Log.v("-Http-", "Upload Fail with msg : " + msg);
			} catch (JSONException e) {
				e.printStackTrace();
				Log.v("-Http-", "Upload Fail: Exception");
				return false;
			}
		}
		return false;
	}
	
	public boolean httpTerminate() {
		// 向服务器发送放弃消息
		// 0:成功
		// 其他:相应的错误信息	
		
		//q_id = 2;
		String url = SERVER_URL;
		url = url.concat(TERMINATE_URL).concat("?uid=")
		.concat(uid).concat("&q_id=").concat(String.valueOf(q_id));		
		Log.v("-Http-", "Send Terminate Request : " + url);
		
		String response = httpGetResponse(url);
		Log.v("-Http-", "Terminate Response: " + response);
		if (response != "")
		{
			JSONObject json = null;
			try {
				json = new JSONObject(response);
				int ret = json.getInt("ret");
				String msg = json.getString("msg");	
				if (ret == 0) {
					Log.v("-Http-", "Terminate Success with qid : " + q_id);
					return true;
				}
				Log.v("-Http-", "Terminate Fail with msg : " + msg);
			} catch (JSONException e) {
				e.printStackTrace();
				Log.v("-Http-", "Terminate Fail: Exception");
				return false;
			}
		}
		return false;
	}

	public QEyesHttpResults httpCheckAns() {
		//向服务器询问结果
		//q_id =30;
		String url = SERVER_URL;
		url = url.concat(CHECK_ANS_URL).concat("?q_id=").concat(String.valueOf(q_id));
		Log.v("-Http-", "Send CheckAns Request : " + url);
		
		String response = httpGetResponse(url);
		Log.v("-Http-", "CheckAns Response: " + response);		

		QEyesHttpResults results = new QEyesHttpResults();
		if (response != "")
		{
			JSONObject json = null;
			try {
				json = new JSONObject(response);
				results.ret = json.getInt("ret");
				results.msg = json.getString("msg");
				if (results.ret == 1)
				{
					JSONObject data = json.getJSONObject("data");
					
					results.volunteer = data.getString("volunteer");
					results.type = data.getInt("type");
					if (results.type == 1) {
						results.content = SERVER_IP + data.getString("content");
					} else {
						results.content = data.getString("content");
					}					
					Log.v("-Http-", "CheckAns Succeed! ");
				} else {
					Log.v("-Http-", "CheckAns Fail with msg: "+ results.msg);
				}
			} catch (JSONException e) {
				e.printStackTrace();
				Log.v("-Http-", "CheckAns Fail: Exception");
				return null;
			}
		}
		return results;
	}

	public boolean httpComment(int score) {
		// 上报评价给服务器  
		// 2:满意 
		// 1:不满意
		// 0:恶意信息
		//q_id = 30;
		String url = SERVER_URL;
		url = url.concat(COMMENT_URL).concat("?uid=")
		.concat(uid).concat("&q_id=").concat(String.valueOf(q_id))
		.concat("&score=").concat(String.valueOf(score));		
		Log.v("-Http-", "Send Comment Request : " + url);
		
		String response = httpGetResponse(url);
		Log.v("-Http-", "Comment Response: " + response);
		if (response != "")
		{
			JSONObject json = null;
			try {
				json = new JSONObject(response);
				int ret = json.getInt("ret");
				String msg = json.getString("msg");	
				if (ret == 0) {
					Log.v("-Http-", "Comment Success with score: " + score);
					return true;
				}
				Log.v("-Http-", "Comment Fail with msg : "+ msg);
			} catch (JSONException e) {
				e.printStackTrace();
				Log.v("-Http-", "Comment Fail: Exception");
				return false;
			}
		}
		return false;
	}	
}
