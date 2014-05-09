package com.tencent.qeyes;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.net.Uri;
import android.os.Message;
import android.os.SystemClock;

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
		STATE_EVALUATE_EXIT     	//程序退出
	}
	
	public State curState;
	public TextSpeaker textSpeaker;
	public QEyesHttpConnection qHttp;
	public AudioSpeaker audioSpeaker;
	public String response;
	public boolean isAudio;
	public QEyes.MainHandler handler;
	
	QEyesStateMachine(QEyesHttpConnection qHttp, QEyes.MainHandler handler) {
		curState = State.STATE_ZERO;
		textSpeaker = null;
		audioSpeaker = null;
		response = null;
		isAudio = false;
		this.qHttp = qHttp;
		this.handler = handler;
	}
	
	public void setSpeaker(Context context) {
		textSpeaker = new TextSpeaker(context, handler);
		audioSpeaker = new AudioSpeaker(context);
	}
	
	public boolean setState(State s) {
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
				if (isSingleColor()) {
					speak("识别为单色,重新拍摄请按音量加,上传请按音量减!");
				} else {
					speak("已拍摄,上传请按音量加,取消请按音量减!");
				}
				break;
			}
			case STATE_UPLOAD_FAILURE : {
				speak("上传失败,重试请按音量加,取消请按音量减!");
				break;
			}
			case STATE_WAITING_PHASE_ONE : {
				speak("已上传,请稍候!");
				final Timer t1 = new Timer();
				t1.schedule(new TimerTask() {					
					Message msg = new Message();					
					@Override
					public void run() {	
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
				speak("收到回复!");
				SystemClock.sleep(1000);
				//改为振动
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
	private boolean isSingleColor() {
		return false;
	}
}
