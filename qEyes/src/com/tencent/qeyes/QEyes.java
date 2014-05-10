/**
 * Main Activity
 * Author: richardfeng
 * Date:2014/5.9
 * Version:1.0
 */
package com.tencent.qeyes;

import java.io.FileOutputStream;
import java.io.IOException;

import com.tencent.qeyes.R;
import com.tencent.qeyes.QEyesStateMachine.State;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Description:
 * <br/>site: <a href="http://qeyes.tencent.com">tencent.com</a>
 * <br/>Copyright (C), 2001-2014, RichardFeng, MinjieYu
 * <br/>This program is protected by copyright laws.
 * <br/>Program Name: QEyes
 * <br/>Date: 2014.5.5
 * @author  RichardFeng richardfeng54@gmail.com
 * @version  1.00
 */
public class QEyes extends Activity implements MsgType {
	
	final String FILE_NAME = "qEyes.jpg";

	SurfaceView sView;
	SurfaceHolder surfaceHolder;
	Camera camera;
	QEyesHttpConnection qHttp;
	boolean shortPress = false;
	boolean isPreview = false;
	int screenWidth, screenHeight;
	String uid;
	MainHandler qHandler = new MainHandler();	

	static QEyesStateMachine qState;
	static class MainHandler extends Handler {
				
		@Override 
		public void handleMessage(Message msg) {			
			switch (msg.what) {
				case TTS_INITIAL_SUCCESS : {
					QEyes.qState.textSpeaker.speakAndCallBack("您好,您可以随时长按音量键退出程序!", "INIT");	
					break;
				}
				case MSG_QUESTION_DISPATCHED : {
					QEyes.qState.setState(State.STATE_WAITING_PHASE_TWO);
					break;
				}
				case MSG_QUESTION_ANSWERED : {
					QEyes.qState.setState(State.STATE_SPEAKING_RESULTS);
					break;
				}
				case MSG_SVR_TIMEOUT : {
					QEyes.qState.setState(State.STATE_NO_RESPONSE);
					break;
				}
				case MSG_TS: {
					if (qState.curState == State.STATE_EVALUATE_STOP)
						break;
					String strMsg = (String)msg.obj;
					if (strMsg.equals("INIT")) {
						QEyes.qState.setState(State.STATE_INIT);
					} else if (strMsg.equals("COMMENT")) {
						QEyes.qState.setState(State.STATE_EVALUATE_PHASE_ONE);
					} else if (strMsg.equals("EXIT")) {
						System.exit(0);
					}
					break;
				}					
				default : {
					break;
				}
			}
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		init();
	}
	
	// 重写这个函数来禁止摄像头自动旋转
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	}
	
	@Override
	public void onStop() {
		qState.textSpeaker.speakBlocked("程序切换至后台!");
		super.onStop();
	}
	
	@SuppressWarnings("deprecation")
	private void init() {

		uid = getUID(getApplicationContext());
		
		//在HOME键之后返回时可以不用重新初始化
		if (qHttp == null) {
			qHttp = new QEyesHttpConnection(uid, qHandler);
		}		
		
		if (qState == null) {
			qState = new QEyesStateMachine(qHttp, qHandler);
			qState.setSpeaker(getApplicationContext());
		}
			
		// initial settings
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
			WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.main);
		
		// 允许http请求运行于主线程
		if (android.os.Build.VERSION.SDK_INT > 9) {
		    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		    StrictMode.setThreadPolicy(policy);
		}
		
		WindowManager wm = getWindowManager();
		Display display = wm.getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		display.getMetrics(metrics);
		screenWidth = metrics.widthPixels;
		screenHeight = metrics.heightPixels;
		sView = (SurfaceView) findViewById(R.id.sView);
		sView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surfaceHolder = sView.getHolder();			
		
		setSystemMusicVolume();
		
		surfaceHolder.addCallback(new Callback() {
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			}
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				if (!isPreview)	{
					// 默认打开后置摄像头
					camera = Camera.open(0);  
					// 设置相机的预览角度
					camera.setDisplayOrientation(90);
				}
				if (camera != null && !isPreview) {
					try {
						//设计拍照参数
						Camera.Parameters parameters = camera.getParameters();
						parameters.setPreviewSize(screenWidth, screenHeight);
						parameters.setPreviewFpsRange(4, 10);
						parameters.setPictureFormat(ImageFormat.JPEG);
						parameters.set("jpeg-quality", 100);
						parameters.setPictureSize(screenWidth, screenHeight);
						camera.setPreviewDisplay(surfaceHolder); 
						camera.startPreview();  
					} catch (Exception e) {
						e.printStackTrace();
					}
					isPreview = true;
				}
			}
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				if (camera != null) {
					if (isPreview) camera.stopPreview();
					camera.release();
					camera = null;
				}
			}
		});		
	}
	
	/**
	 * 设置系统播放器音量
	 */
	private void setSystemMusicVolume() {
		// 音频管理对象
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		int minVolume = 0;
		int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		if (volume == 0)//当前值为0则修改为最大值的一半，否则不变
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (minVolume + maxVolume)/2, 0);
	}
	
	private void capture(View source) {
		if (camera != null) {
			camera.autoFocus(new AutoFocusCallback() {
				@Override
				public void onAutoFocus(boolean success, Camera camera) {
					if (success) {
						camera.takePicture(new ShutterCallback() {
							@Override
							public void onShutter() {
								//按下快门时的处理
							}
						}, new PictureCallback() {
							@Override
							public void onPictureTaken(byte[] data, Camera c) {
								//原始图片的处理
							}
						}, new PictureCallback() {
							@Override
							public void onPictureTaken(byte[] data, Camera camera) {
								Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
								
								//拍照所得应旋转90度
								Matrix matrix = new Matrix();  
								matrix.preRotate(90); 
								bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), 
										bm.getHeight(), matrix, true);
													
								FileOutputStream outStream = null;
								
								//自适应调整压缩比例
								int scale = 1;
								while (scale * scale < data.length / 1024 / 128) {
									scale *= 2;
								}			
								
								try {
									outStream = openFileOutput(FILE_NAME, MODE_PRIVATE);
									bm.compress(CompressFormat.JPEG, 100, outStream);
									outStream.close();									
									BitmapFactory.Options newOpts = new BitmapFactory.Options();									
									newOpts.inJustDecodeBounds = false;
									newOpts.inSampleSize = scale;//设置缩放比例
									
									//一次压缩
									Bitmap bitmap = BitmapFactory.decodeFile(getFilesDir() + "/" + FILE_NAME, newOpts);
									outStream = openFileOutput(FILE_NAME, MODE_PRIVATE);
									
									//二次压缩
									bitmap.compress(CompressFormat.JPEG, 50, outStream);	
									outStream.close();
									
								} catch (IOException e) {
									e.printStackTrace();
								}	
								camera.stopPreview();
								camera.startPreview();
								isPreview = true;
							}
						});  
					}
				}
			});  
		}
	}
	
	// 监听音量键	
	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {		
		//通过shortPress变量来区分 长按 和 按下
		if (qState.curState == State.STATE_EVALUATE_EXIT) {
			shortPress = false;
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP 
				|| keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			shortPress = false;
			qHttp.httpTerminate();
			qState.textSpeaker.speakAndCallBack("程序退出,欢迎您下次使用!", "EXIT");
			qState.setState(State.STATE_EVALUATE_EXIT);
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			event.startTracking();
			if(event.getRepeatCount() == 0) {
				shortPress = true;
			}
		} 		
		return true;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP 
				|| keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			if (shortPress)	{
				switch (qState.curState) {
					case STATE_INIT : {
						if (keyCode == KeyEvent.KEYCODE_VOLUME_UP 
								|| keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
							capture(sView);
							qState.setState(State.STATE_PHOTO_ACQUIRED);
						}
						break;
					}
					case STATE_PHOTO_ACQUIRED : {
						if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
							qState.setState(State.STATE_INIT);
						} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
							if(qHttp.httpUpload(getFilesDir() + "/" + FILE_NAME)) {
								qState.setState(State.STATE_WAITING_PHASE_ONE);
							} else {
								qState.setState(State.STATE_UPLOAD_FAILURE);						
							}					
						}
						break;
					}
					case STATE_UPLOAD_FAILURE : {
						if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
							qHttp.httpTerminate();
							if(qHttp.httpUpload(getFilesDir() + "/" + FILE_NAME)) {
								qState.setState(State.STATE_WAITING_PHASE_ONE);
							} else {
								qState.setState(State.STATE_UPLOAD_FAILURE);						
							}
						} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
							qHttp.httpTerminate();
							qState.setState(State.STATE_INIT);					
						}
						break;
					}
					case STATE_NO_RESPONSE : {
						if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
							qHttp.httpTerminate();
							if(qHttp.httpUpload(getFilesDir() + "/" + FILE_NAME)) {
								qState.setState(State.STATE_WAITING_PHASE_ONE);
							} else {
								qState.setState(State.STATE_UPLOAD_FAILURE);						
							}
						} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
							qHttp.httpTerminate();
							qState.setState(State.STATE_INIT);
						}
						break;
					}
					case STATE_EVALUATE_PHASE_ONE : {
						if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
							qHttp.httpComment(2);
							qHttp.httpTerminate();
							qState.setState(State.STATE_INIT);
						} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
							qState.setState(State.STATE_EVALUATE_PHASE_TWO);
						}
						break;
					}
					case STATE_EVALUATE_PHASE_TWO : {
						if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
							qHttp.httpComment(0);
							qHttp.httpTerminate();
							qState.setState(State.STATE_INIT);
						} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
							qHttp.httpComment(1);
							qHttp.httpTerminate();
							qState.setState(State.STATE_INIT);
						}
						break;
					}		
					default:
						break;		
				}		
			}
		}		
		shortPress = false;
		return true;
	}
		 
	// 返回IMSI号或者SERIAL号,作为设备唯一的UID
	private String getUID(final Context context) {
		final String deviceId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
		if (deviceId != null) {
		    return deviceId;
		} else {
		    return android.os.Build.SERIAL;
		}
	}
}

