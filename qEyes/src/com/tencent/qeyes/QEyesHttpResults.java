package com.tencent.qeyes;

/**
 * Http返回结构体
 */

public class QEyesHttpResults {
	int ret;			//0:未回答
						//1:已回答
						//2:问题失效
						//3:问题已被抢
						//4:其他错误
	String msg;			//具体的错误信息
	String volunteer;	//志愿者QQ号:暂时无用
	int type;			//语音还是文字
	String content;		//内容
}
