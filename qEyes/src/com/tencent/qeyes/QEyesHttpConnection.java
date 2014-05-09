/**
 * Http协议调用与封装
 * Author: minjieyu
 * Date:2014/5.9
 * Version:1.0
 */
package com.tencent.qeyes;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Log;

public class QEyesHttpConnection extends HttpConnection implements MsgType {
	private static final String SERVER_IP = "http://203.195.190.137";
	private static final String SERVER_URL = "http://203.195.190.137/mini/qEyes/index.php";
	private static final String UPLOAD_URL = "/client/question";
	private static final String TERMINATE_URL = "/client/terminateQues";
	private static final String CHECK_ANS_URL = "/client/checkAns";
	private static final String COMMENT_URL = "/client/comment";
	static final int RETRY_TIMES = 2;
	protected String uid;			//手机唯一标示
	protected volatile int q_id;	//一定要用volatile关键字，保证其值在不同线程之间的一致性
	protected Handler UIHandler;
	
	public QEyesHttpConnection(String uid, Handler UIHandler) {
		this.uid = uid;
		q_id = 0;
		this.UIHandler = UIHandler;
	}

	// 向服务器上传图片，自带重传机制
	public boolean httpUpload(String fileName) {
			
		// 0:成功 并得到qid
		// 其他:会有相应的错误提示
		
		int flag = 0;//重传次数，不超过 RETRY_TIMES
		String url = SERVER_URL;
		url = url.concat(UPLOAD_URL);		
		
		String response = httpUpload(url, fileName, uid);
		if (response == null && flag < RETRY_TIMES) {
			response = httpUpload(url, fileName, uid);
			flag++;
		}
		
		if (response != null) {
			JSONObject json = null;
			try {
				json = new JSONObject(response);
				int ret = json.getInt("ret");
				String msg = json.getString("msg");	
				if (ret == 0) {			
					q_id = json.getInt("q_id");
					return true;					
				}
				Log.v("-Http-", "Upload Fail with msg : " + msg);
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}
	
	// 向服务器发送放弃消息，自带重传机制
	public boolean httpTerminate() {
		
		// 0:成功
		// 其他:相应的错误信息	
		
		int flag = 0;//重传次数，不超过 RETRY_TIMES
		String url = SERVER_URL;
		url = url.concat(TERMINATE_URL).concat("?uid=")
		.concat(uid).concat("&q_id=").concat(String.valueOf(q_id));	
		
		//url构造好即刻将q_id置0
		q_id = 0;
		String response = httpGetResponse(url);
		
		if (response == null && flag < RETRY_TIMES) {
			response = httpGetResponse(url);
			flag++;
		}
		
		if (response != null) {
			JSONObject json = null;
			try {
				json = new JSONObject(response);
				int ret = json.getInt("ret");
				String msg = json.getString("msg");	
				if (ret == 0) {
					return true;
				}
				Log.v("-Http-", "Terminate Fail with msg : " + msg);
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}

	// 向服务器询问问题的状态
	public QEyesHttpResults httpCheckAns() {
		
		String url = SERVER_URL;
		url = url.concat(CHECK_ANS_URL).concat("?q_id=").concat(String.valueOf(q_id));
		
		String response = httpGetResponse(url);	

		QEyesHttpResults results = new QEyesHttpResults();
		if (response != null)
		{
			JSONObject json = null;
			try {
				json = new JSONObject(response);
				results.ret = json.getInt("ret");
				results.msg = json.getString("msg");
				if (results.ret == 1) {
					JSONObject data = json.getJSONObject("data");
					
					results.volunteer = data.getString("volunteer");
					results.type = data.getInt("type");
					if (results.type == 1) {
						results.content = SERVER_IP + data.getString("content");
					} else {
						results.content = data.getString("content");
					}					
				} else {
					Log.v("-Http-", "CheckAns Fail with msg: "+ results.msg);
				}
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
		return results;
	}

	// 上报评价给服务器  自带重传机制
	public boolean httpComment(int score) {
		// 2:满意   1:不满意   0:恶意信息
		
		int flag = 0;//重传次数，不超过 RETRY_TIMES
		String url = SERVER_URL;
		url = url.concat(COMMENT_URL).concat("?uid=")
		.concat(uid).concat("&q_id=").concat(String.valueOf(q_id))
		.concat("&score=").concat(String.valueOf(score));
		
		String response = httpGetResponse(url);
		
		if (response == null && flag < RETRY_TIMES) {
			response = httpGetResponse(url);
			flag++;
		}
		
		if (response != null) {
			JSONObject json = null;
			try {
				json = new JSONObject(response);
				int ret = json.getInt("ret");
				String msg = json.getString("msg");	
				if (ret == 0) {
					return true;
				}
				Log.v("-Http-", "Comment Fail with msg : "+ msg);
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}	
}
