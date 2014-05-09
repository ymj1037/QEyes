package com.tencent.qeyes;

import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class TextSpeaker implements
TextToSpeech.OnUtteranceCompletedListener, TextToSpeech.OnInitListener, MsgType{
	private Context context;
	private TextToSpeech tts;   
	private Handler UIHandler;
	public TextSpeaker(final Context context, Handler UIHandler) {
		// TODO Auto-generated constructor stub        
		this.context = context;        
		this.UIHandler = UIHandler;
		tts = new TextToSpeech(context, this);    		
		}     
	
	public void speak(String text) {       
		//HashMap<String, String> params = new HashMap<String, String>();
		//params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Hello");
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);  
		Log.v("-Test-", "Speak " + text);
		}
	
	public void speakAppend(String text) {       
		//HashMap<String, String> params = new HashMap<String, String>();
		//params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Hello");
		tts.speak(text, TextToSpeech.QUEUE_ADD, null);    
		Log.v("-Test-", "Speak " + text);
		}
	
	public void speakAndCallBack(String text, String msg) {       
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, msg);
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);   
		Log.v("-Test-", "Speak " + text);
		}
	
	public void speakAppendAndCallBack(String text, String msg) {       
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, msg);
		tts.speak(text, TextToSpeech.QUEUE_ADD, params); 
		Log.v("-Test-", "Speak " + text);
		}
	
	@Override
	public void onInit(int status) {
		// TODO Auto-generated method stub               
			if (status == TextToSpeech.SUCCESS)                
			{                    
				int result = tts.setLanguage(Locale.CHINA); 				
				if (result == TextToSpeech.LANG_MISSING_DATA                            
						|| result == TextToSpeech.LANG_NOT_SUPPORTED)                    
				{                        
					Toast.makeText(context, "Language is not available.",                                
							Toast.LENGTH_SHORT).show();                    
					}                
				}
				tts.setOnUtteranceCompletedListener(this);
				Message msg = new Message();
				msg.what = TTS_INITIAL_SUCCESS;
				UIHandler.sendMessage(msg);
				//Log.v("-Test-", "已经发送TTS_INITIAL_SUCCESS这个消息");
			}  

	@Override
	public void onUtteranceCompleted(String utteranceId) {
		// TODO Auto-generated method stub
		Message msg = new Message();
		msg.what = MSG_TS;
		msg.obj = utteranceId;
		Log.v("-Test-", "Speak完 " + utteranceId);
		UIHandler.sendMessage(msg);
		}
	public void speakBlocked(String text) {
		// TODO Auto-generated method stub
		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);  
		Log.v("-Test-", "Speak " + text);
		while(tts.isSpeaking())
			;
	}
}

