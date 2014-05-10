/**
 * 颜色识别结构体
 * Author: richardfeng
 * Date:2014/5.10
 * Version:1.0
 */
package com.tencent.qeyes;

public class ColorInfo {
	public boolean isPure;
	public String detail;
	
	ColorInfo() {
		isPure = false;
		detail = "非单色";
	}
	
	ColorInfo(boolean isPure, String detail) {
		this.isPure = isPure;
		this.detail = detail;
	}
}
