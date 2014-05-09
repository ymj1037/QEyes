/***
 * Author minjieyu
 * Date 2014/05/09
 */
package com.tencent.qeyes;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

/**
 * 用于播放语音的类
 */

public class AudioSpeaker {
	private Context context;
	public MediaPlayer mPlayer;
	
	public AudioSpeaker(final Context context) {     
		this.context = context;        
		}         
	public void play(int rsid) {       
		mPlayer = MediaPlayer.create(context, rsid);
		mPlayer.start(); 
		}
	public void play(String filePath) {       
		mPlayer = new MediaPlayer();
		try
		{	
			mPlayer.setDataSource(filePath);
			mPlayer.prepare();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}		
		mPlayer.start(); 
	}
	public void play(Uri uri) {       
		mPlayer = new MediaPlayer();
		try
		{	
			mPlayer.setDataSource(context, uri);
			mPlayer.prepare();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}		
		mPlayer.start(); 
	}
}
