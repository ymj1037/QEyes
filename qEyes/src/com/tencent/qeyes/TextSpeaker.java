/**
 * 文本转语音模块
 * Author: minjieyu
 * Date:2014/5.9
 * Version:1.0
 */
package com.tencent.qeyes;

import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class TextSpeaker implements TextToSpeech.OnUtteranceCompletedListener, TextToSpeech.OnInitListener, MsgType {
	
	private Context context;
	private TextToSpeech tts;   
	private Handler UIHandler;
	
	public TextSpeaker(final Context context, Handler UIHandler) {
		this.context = context;        
		this.UIHandler = UIHandler;
		tts = new TextToSpeech(context, this);    		
	}
	
	@Override
	public void onInit(int status) {             
		if (status == TextToSpeech.SUCCESS) {              
			int result = tts.setLanguage(Locale.CHINA); 				
			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {                        
				Toast.makeText(context, "Language is not available.", Toast.LENGTH_SHORT).show();                    
			}                
		}
		tts.setOnUtteranceCompletedListener(this);
		//此处必须采取msg的方式将tts成功初始化的消息传递出去
		//否则由于tts初始化需要一段时间，在此段时间内将无法朗读文字
		Message msg = new Message();
		msg.what = TTS_INITIAL_SUCCESS;
		UIHandler.sendMessage(msg);
	}  
	
	public void speak(String text) {       
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);  
	}
	
	// 将文字加入朗读队列
	public void speakAppend(String text) {       
		tts.speak(text, TextToSpeech.QUEUE_ADD, null);  
	}
	
	// 阻塞朗读文字
	public void speakBlocked(String text) {
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
		while(tts.isSpeaking()) {			
		}
	}
	
	// 朗读并发出消息
	public void speakAndCallBack(String text, String msg) {       
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, msg);
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, params); 
	}
	
	// 将文字加入朗读队列并发出消息
	public void speakAppendAndCallBack(String text, String msg) {       
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, msg);
		tts.speak(text, TextToSpeech.QUEUE_ADD, params);
	}

	@Override
	public void onUtteranceCompleted(String utteranceId) {
		Message msg = new Message();
		msg.what = MSG_TS;
		msg.obj = utteranceId;
		UIHandler.sendMessage(msg);
	}
}

