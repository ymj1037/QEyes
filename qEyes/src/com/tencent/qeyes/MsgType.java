package com.tencent.qeyes;

public interface MsgType {
	int TTS_INITIAL_SUCCESS = 0x0001;
	int TTS_INITIAL_FAIL = 0x0002;
	int MSG_QUESTION_DISPATCHED = 0x0003;
	int MSG_QUESTION_ANSWERED = 0x0004;
	int MSG_SVR_TIMEOUT = 0x0005;
	int MSG_SPK_COMPLETE = 0x123;
	int MSG_HTTP_TIMEOUT = 0x0006;
	int MSG_HTTP_SUCCESS = 0x0007;
	int MSG_TS = 0x0008;
}
