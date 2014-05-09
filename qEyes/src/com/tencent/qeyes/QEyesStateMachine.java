/**
 * QEyes状态机
 * Author: richardfeng
 * Date:2014/5.9
 * Version:1.0
 */
package com.tencent.qeyes;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.net.Uri;
import android.os.Message;

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
		//Log.v("-Test-", "已经进入setspeaker函数");
		textSpeaker = new TextSpeaker(context, handler);
		audioSpeaker = new AudioSpeaker(context);
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
				speak("请按音量键拍照!");				
				break;
			}
			case STATE_PHOTO_ACQUIRED : {
				if (isSingleColor()) {
					speak("识别为单色,重新拍摄请按音量加,上传请按音量减!");
				} else {
					speak("重新拍摄请按音量加,上传请按音量减!");
				}
				break;
			}
			case STATE_UPLOAD_FAILURE : {
				speak("上传失败,重新上传请按音量加,放弃请按按音量减!");
				break;
			}
			case STATE_WAITING_PHASE_ONE : {
				speak("已上传,抢单中,请耐心等待三十秒!");
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
				speak("已被抢单，志愿者正在回答，请耐心等待六十秒!");
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
				speak("暂时无人响应，重新上传请按音量加，放弃请按按音量减！");
				break;
			}	
			case STATE_SPEAKING_RESULTS : {
				speak("收到志愿者回复!");
				if (isAudio) {
					audioSpeaker.play(Uri.parse(response));
					while(audioSpeaker.mPlayer.isPlaying()) {						
					}
				} else {
					textSpeaker.speakAppendAndCallBack(response, "COMMENT");
				}
				//setState(State.STATE_EVALUATE_PHASE_ONE);
				break;
			}
			case STATE_EVALUATE_PHASE_ONE : {
				speak("请对本次回复做出评价，音量加为满意，音量减为不满意！");
				break;
			}
			case STATE_EVALUATE_PHASE_TWO : {
				speak("是否举报恶意回复，音量加为恶意，音量减为非恶意！");
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
	private void speakBlocked(String text) {
		textSpeaker.speakBlocked(text);
	}
	private boolean isSingleColor() {
		return false;
	}
}
