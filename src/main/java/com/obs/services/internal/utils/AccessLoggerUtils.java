/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.obs.services.internal.utils;


import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AccessLoggerUtils {
	private static final ThreadLocal<StringBuilder> threadLocalLog = new ThreadLocal<StringBuilder>();
	private static final ThreadLocal<SoftReference<SimpleDateFormat>> simpleDateFormateHolder = new ThreadLocal<SoftReference<SimpleDateFormat>>();
	private static final int INDEX = 5;
	public static volatile boolean ACCESSLOG_ENABLED = true;

	private static String getLogPrefix() {
		if (!ACCESSLOG_ENABLED) {
			return "";
		}
		StackTraceElement[] stacktraces = Thread.currentThread().getStackTrace();
		StackTraceElement stacktrace = null;
		if (stacktraces.length > INDEX) {
			stacktrace = stacktraces[INDEX];
		} else {
			stacktrace = stacktraces[stacktraces.length - 1];
		}

		return new StringBuilder().append(stacktrace.getClassName()).append("|").append(stacktrace.getMethodName())
				.append("|").append(stacktrace.getLineNumber()).append("|").toString();
	}

	private static StringBuilder getLog() {
		StringBuilder logSb = threadLocalLog.get();
		if (logSb == null) {
			logSb = new StringBuilder();
			threadLocalLog.set(logSb);
		}
		return logSb;
	}

	public static void appendLog(Object log, String level) {
		if (!ACCESSLOG_ENABLED) {
			return;
		}


			StringBuilder sb = new StringBuilder(getFormat().format(new Date()));
			sb.append("|").append(AccessLoggerUtils.getLogPrefix()).append(log.toString()).append("\n");
			getLog().append(sb.toString());

	}

	public static SimpleDateFormat getFormat() {
		SoftReference<SimpleDateFormat> holder = simpleDateFormateHolder.get();
		SimpleDateFormat format;
		if (holder == null || ((format = holder.get()) == null)) {
			format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
			holder = new SoftReference<SimpleDateFormat>(format);
			simpleDateFormateHolder.set(holder);
		}
		return format;
	}

	public static void printLog() {
		if (!ACCESSLOG_ENABLED) {
			return;
		}
		String message = getLog().toString();
		if (ServiceUtils.isValid(message)) {

		}
		threadLocalLog.remove();
	}
}
