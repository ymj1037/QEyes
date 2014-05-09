package com.tencent.qeyes;

import java.io.FileOutputStream;
import java.io.IOException;

import com.tencent.qeyes.R;
import com.tencent.qeyes.QEyesStateMachine.State;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
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
	
	final String FILE_NAME = "test.jpg";

	SurfaceView sView;
	SurfaceHolder surfaceHolder;
	int screenWidth, screenHeight;
	Camera camera;
	boolean isPreview = false;
	QEyesHttpConnection qHttp;
	String uid;
	boolean shortPress = false;
	MainHandler qHandler = new MainHandler();	

	static QEyesStateMachine qState;
	static class MainHandler extends Handler {
				
		@Override 
		public void handleMessage(Message msg) {			
			switch (msg.what) {
				case TTS_INITIAL_SUCCESS : {
					//Log.v("-Test-", "接收到TTS_INITIAL_SUCCESS这个消息");
					QEyes.qState.textSpeaker.speakBlocked("欢迎使用盲人辅助软件,您可以随时长按音量键退出程序!");
					QEyes.qState.setState(State.STATE_INIT);			
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
					Log.v("-Activity-", "Handler处理  MSG_TS");
					Log.v("-Activity-", "Handler处理" + msg.obj +"123");
					String strMsg = (String)msg.obj;
					if (strMsg.equals("INIT"))
					{
						Log.v("-Activity-", "Handler处理" + msg.obj);
						QEyes.qState.setState(State.STATE_INIT);
					}
					else if (strMsg.equals("COMMENT"))
						QEyes.qState.setState(State.STATE_EVALUATE_PHASE_ONE);
					else if (strMsg.equals("EXIT"))
						System.exit(0);
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
	
	@Override
	public void onStop() {
		qState.textSpeaker.speakBlocked("程序切换至后台!");
		super.onStop();
	}
	
	@SuppressWarnings("deprecation")
	private void init() {

		uid = getUID(getApplicationContext());
		qHttp = new QEyesHttpConnection(uid);
		
		//在HOME键之后返回时可以不用重新初始化
		if (qState == null) {
			qState = new QEyesStateMachine(qHttp, qHandler);
			qState.setSpeaker(getApplicationContext());
		}
			
		// initial settings
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
			WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.main);
		
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
		
		surfaceHolder.addCallback(new Callback() {
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			}
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				if (!isPreview)	{
					camera = Camera.open(0);  
					camera.setDisplayOrientation(90);
				}
				if (camera != null && !isPreview) {
					try {
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
	
	public void capture(View source) {
		if (camera != null) {
			camera.autoFocus(new AutoFocusCallback() {
				@Override
				public void onAutoFocus(boolean success, Camera camera) {
					if (success) {
						camera.takePicture(new ShutterCallback() {
							public void onShutter() {
							}
						}, new PictureCallback() {
							public void onPictureTaken(byte[] data, Camera c) {
							}
						}, new PictureCallback() {
							@Override
							public void onPictureTaken(byte[] data, Camera camera) {
								final Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
													
								FileOutputStream outStream = null;
								int scale = 1;
								while (scale * scale < data.length / 1024 / 128) {
									scale *= 2;
								}			
								Log.v("-Activity-", scale + "");
								try {
									outStream = openFileOutput(FILE_NAME, MODE_PRIVATE);
									bm.compress(CompressFormat.JPEG, 100, outStream);
									outStream.close();									
									BitmapFactory.Options newOpts = new BitmapFactory.Options();									
									newOpts.inJustDecodeBounds = false;
									newOpts.inSampleSize = scale;//设置缩放比例
									Bitmap bitmap = BitmapFactory.decodeFile(getFilesDir() + "/" + FILE_NAME, newOpts);
									outStream = openFileOutput(FILE_NAME, MODE_PRIVATE);
									bitmap.compress(CompressFormat.JPEG, 80, outStream);	
									outStream.close();							
									//Log.v("-Activity-", "图片压缩完成!");
									
									/*View saveDialog = getLayoutInflater().inflate(R.layout.image, null);
									ImageView show = (ImageView) saveDialog.findViewById(R.id.jpgview);
									Bitmap bm2 = BitmapFactory.decodeFile(getFilesDir() + "/" + FILE_NAME);				
									show.setImageBitmap(bm2);*/
									
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
	
	//Only Listen VOlUME and HOME	
	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
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
						Log.v("-Test-", "State 进入INIT ");
						if (keyCode == KeyEvent.KEYCODE_VOLUME_UP 
								|| keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
							capture(sView);
							qState.setState(State.STATE_PHOTO_ACQUIRED);
						}
						break;
					}
					case STATE_PHOTO_ACQUIRED : {
						if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
							capture(sView);
							qState.setState(State.STATE_PHOTO_ACQUIRED);
						} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
							//if (true) {
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
		 
	private String getUID(Context context) {
		final String deviceId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
	    if (deviceId != null) {
	        return deviceId;
	    } else {
	        return android.os.Build.SERIAL;
	    }
	}
}

