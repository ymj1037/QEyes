/**
 * QEyes状态机
 * Author: richardfeng
 * Date:2014/5.9
 * Version:1.0
 */
package com.tencent.qeyes;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.net.Uri;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;

/**
 * Qeyes状态机和辅助函数
 */

public class QEyesStateMachine implements MsgType {
	
	enum State{
		STATE_ZERO,					//初始状态0
		STATE_INIT,					//初始状态
		STATE_PHOTO_ACQUIRED,		//拍好照片
		STATE_UPLOAD_FAILURE,		//上传失败
		STATE_WAITING_PHASE_ONE, 	//30s等待抢单
		STATE_WAITING_PHASE_TWO, 	//60s等待回答
		STATE_NO_RESPONSE, 			//无人响应
		STATE_SPEAKING_RESULTS,		//朗读结果
		STATE_EVALUATE_PHASE_ONE,	//评价步骤1
		STATE_EVALUATE_PHASE_TWO,	//评价步骤2
		STATE_EVALUATE_STOP,     	//程序切换到后台
		STATE_EVALUATE_EXIT     	//程序退出
	}
	
	public State curState;
	public TextSpeaker textSpeaker;
	public QEyesHttpConnection qHttp;
	public AudioSpeaker audioSpeaker;
	public String response;
	public boolean isAudio;
	public QEyes.MainHandler handler;
	public Vibrator vibrator;
	
	QEyesStateMachine(QEyesHttpConnection qHttp, QEyes.MainHandler handler) {
		curState = State.STATE_ZERO;
		textSpeaker = null;
		audioSpeaker = null;
		response = null;
		isAudio = false;
		vibrator = null;
		this.qHttp = qHttp;
		this.handler = handler;
	}
	
	public void setSpeaker(Context context) {
		textSpeaker = new TextSpeaker(context, handler);
		audioSpeaker = new AudioSpeaker(context);
		vibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
	}
	
	public boolean setState(State s) {
		if (curState == State.STATE_EVALUATE_EXIT)
			return true;
		curState = s;		
		enterState(curState);
		return true;
	}

	private boolean enterState(State s) {
		switch (s) {
			case STATE_INIT : {
				speak("请按任意音量键拍照!");				
				break;
			}
			case STATE_PHOTO_ACQUIRED : {
				speak("上传请按音量加,取消请按音量减!");				
				break;
			}
			case STATE_UPLOAD_FAILURE : {
				speak("上传失败,重试请按音量加,取消请按音量减!");
				break;
			}
			case STATE_WAITING_PHASE_ONE : {
				speak("已上传,请稍候!");
				Log.v("-Http-", "CHeckAns case qid：" + qHttp.q_id);
				final Timer t1 = new Timer();				
				//为timer分配两个任务，一为论询，二为超时
				t1.schedule(new TimerTask() {					
					Message msg = new Message();					
					@Override
					public void run() {	
						Log.v("-Http-", "CHeckAns run qid：" + qHttp.q_id);
						QEyesHttpResults result = qHttp.httpCheckAns();											
						if (result.ret == 3 || result.ret == 1) {
							// 已被抢 或已回答
							msg.what = MSG_QUESTION_DISPATCHED;
							handler.sendMessage(msg);
							t1.cancel();
						}										
					}
				}, 500, 2000);
				t1.schedule(new TimerTask() {					
					Message msg = new Message();					
					@Override
					public void run() {
						msg.what = MSG_SVR_TIMEOUT;
						handler.sendMessage(msg);
						t1.cancel();						
					}
				}, 30000);
				break;
			}		
			case STATE_WAITING_PHASE_TWO : {
				speak("对方正在输入,请稍候!");
				final Timer t1 = new Timer();
				//为timer分配两个任务，一为论询，二为超时
				t1.schedule(new TimerTask() {					
					Message msg = new Message();					
					@Override
					public void run() {	
						QEyesHttpResults result = qHttp.httpCheckAns(); 
						if (result.ret == 1) {
							if (result.type == 1) {
								isAudio = true;
							} else {
								isAudio = false;
							}
							response = result.content;							
							msg.what = MSG_QUESTION_ANSWERED;
							handler.sendMessage(msg);
							t1.cancel();
						} 										
					}
				}, 5000, 5000);
				t1.schedule(new TimerTask() {					
					Message msg = new Message();					
					@Override
					public void run() {
						msg.what = MSG_SVR_TIMEOUT;
						handler.sendMessage(msg);
						t1.cancel();						
					}
				}, 60000);
				break;
			}
			case STATE_NO_RESPONSE : {
				speak("您的请求暂时无人应答，重试请按音量加，取消请按音量减！");
				break;
			}	
			case STATE_SPEAKING_RESULTS : {
				vibrator.vibrate(500);				
				if (isAudio) {
					audioSpeaker.play(Uri.parse(response));
					while(audioSpeaker.mPlayer.isPlaying()) {						
					}
					setState(State.STATE_EVALUATE_PHASE_ONE);
				} else {
					textSpeaker.speakAppendAndCallBack(response, "COMMENT");
				}
				break;
			}
			case STATE_EVALUATE_PHASE_ONE : {
				speak("您是否满意?满意请按音量加,不满意请按音量减！");
				break;
			}
			case STATE_EVALUATE_PHASE_TWO : {
				speak("举报请按音量加,取消请按音量减！");
				break;
			}
			default:
				break;
		}
		return true;
	}	
	private void speak(String text) {
		textSpeaker.speak(text);
	}
}
