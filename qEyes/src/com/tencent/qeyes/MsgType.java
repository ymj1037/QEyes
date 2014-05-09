/**
 * 消息类型
 * Author: richardfeng
 * Date:2014/5.9
 * Version:1.0
 */
package com.tencent.qeyes;

public interface MsgType {
	int TTS_INITIAL_SUCCESS = 0x0001;		//文本播放器初始化成功
	int MSG_QUESTION_DISPATCHED = 0x0002;	//问题已被抢
	int MSG_QUESTION_ANSWERED = 0x0003;		//问题已回答
	int MSG_SVR_TIMEOUT = 0x0004;			//服务器响应超时
	int MSG_HTTP_TIMEOUT = 0x0005;			//HTTP请求超时
	int MSG_HTTP_SUCCESS = 0x0006;			//HTTP请求成功
	int MSG_TS = 0x0007;					//文本播放器相关消息
}
