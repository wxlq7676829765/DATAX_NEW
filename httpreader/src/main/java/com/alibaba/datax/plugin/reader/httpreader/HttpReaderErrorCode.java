package com.alibaba.datax.plugin.reader.httpreader;

import com.alibaba.datax.common.spi.ErrorCode;

/**
 * Created by haiwei.luo on 14-9-20.
 */
public enum HttpReaderErrorCode implements ErrorCode {
	RUNTIME_EXCEPTION("FtpReader-10", "出现运行时异常, 请联系我们"),
	EMPTY_DIR_EXCEPTION("FtpReader-11", "您尝试读取的文件目录为空."),
	;

	private final String code;
	private final String description;

	private HttpReaderErrorCode(String code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public String getCode() {
		return this.code;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public String toString() {
		return String.format("Code:[%s], Description:[%s].", this.code,
				this.description);
	}
}
