/**
 * Http协议异步调用
 * Author: minjieyu
 * Date:2014/5.9
 * Version:0.9
 */
package com.tencent.qeyes;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Message;


// 本类主要负责http的异步实现，用于未来的程序拓展
public class QEyesHttpAsync extends QEyesHttpConnection {
	private static final int TIME_OUT = 5000;

	public QEyesHttpAsync(String uid, Handler UIHandler) {
		super(uid, UIHandler);
		// TODO Auto-generated constructor stub
	}

	public void QEyesHttpUploadAsync(String fileName) {
		//QEyesHttpThread newThread = new QEyesHttpThread(fileName);
		//newThread.start();
		final String file = fileName;
		final Timer timer1 = new Timer();
		final Timer timer2 = new Timer();
		timer1.schedule(new TimerTask()
		{
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_HTTP_TIMEOUT;
				UIHandler.sendMessage(msg);
				timer2.cancel();
			}
		}, TIME_OUT);
		timer2.schedule(new TimerTask()
		{
			@Override
			public void run() {
				int flag = 0;
				boolean bResult = httpUpload(file);
				while (!bResult && flag < 2)
				{
					bResult = httpUpload(file);
					flag++;
				}		
				if (bResult)
				{
					Message msg = new Message();
					msg.what = MSG_HTTP_SUCCESS;
					UIHandler.sendMessage(msg);	
					timer1.cancel();
				}
				else
				{
					Message msg = new Message();
					msg.what = MSG_HTTP_TIMEOUT;
					UIHandler.sendMessage(msg);
					timer1.cancel();
				}
			}
		}, 0);
	}
	

	public void QEyesHttpTerminateAsync() {
		final Timer timer1 = new Timer();
		final Timer timer2 = new Timer();
		timer1.schedule(new TimerTask()
		{
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_HTTP_TIMEOUT;
				UIHandler.sendMessage(msg);
				timer2.cancel();
			}
		}, TIME_OUT);
		timer2.schedule(new TimerTask()
		{
			@Override
			public void run() {
				int flag = 0;
				boolean bResult = httpTerminate();
				while (!bResult && flag < 2)
				{
					bResult = httpTerminate();
					flag++;
				}		
				if (bResult)
				{
					Message msg = new Message();
					msg.what = MSG_HTTP_SUCCESS;
					UIHandler.sendMessage(msg);	
					timer1.cancel();
				}
				else
				{
					Message msg = new Message();
					msg.what = MSG_HTTP_TIMEOUT;
					UIHandler.sendMessage(msg);
					timer1.cancel();
				}
			}
		}, 0);
	}
	
	public void QEyesHttpCheckAnsAsync() {
		final Timer timer1 = new Timer();
		final Timer timer2 = new Timer();
		timer1.schedule(new TimerTask()
		{
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_HTTP_TIMEOUT;
				UIHandler.sendMessage(msg);
				timer2.cancel();
			}
		}, TIME_OUT);
		timer2.schedule(new TimerTask()
		{
			@Override
			public void run() {
				QEyesHttpResults qResult = httpCheckAns();
				
				Message msg = new Message();
				msg.what = MSG_HTTP_SUCCESS;
				msg.obj = qResult;
				UIHandler.sendMessage(msg);	
				timer1.cancel();
			}
		}, 0);
	}
	
	public void QEyesHttpCommentAsync(int score ) {
		//QEyesHttpThread newThread = new QEyesHttpThread(fileName);
		//newThread.start();
		final int fscore = score;
		final Timer timer1 = new Timer();
		final Timer timer2 = new Timer();
		timer1.schedule(new TimerTask()
		{
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Message msg = new Message();
				msg.what = MSG_HTTP_TIMEOUT;
				UIHandler.sendMessage(msg);
				timer2.cancel();
			}
		}, TIME_OUT);
		timer2.schedule(new TimerTask()
		{
			@Override
			public void run() {
				int flag = 0;
				boolean bResult = httpComment(fscore);
				while (!bResult && flag < 2)
				{
					bResult = httpComment(fscore);
					flag++;
				}		
				if (bResult)
				{
					Message msg = new Message();
					msg.what = MSG_HTTP_SUCCESS;
					UIHandler.sendMessage(msg);	
					timer1.cancel();
				}
				else
				{
					Message msg = new Message();
					msg.what = MSG_HTTP_TIMEOUT;
					UIHandler.sendMessage(msg);
					timer1.cancel();
				}
			}
		}, 0);
	}
}

