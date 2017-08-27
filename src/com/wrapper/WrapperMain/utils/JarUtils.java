package com.wrapper.WrapperMain.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class JarUtils {
	public static String getJarFolder() throws UnsupportedEncodingException {
		String path = URLDecoder.decode(ClassLoader.getSystemClassLoader().getResource(".").getPath(), "UTF-8");
		if(OSDetectionUtils.isWindows())
			path = path.substring(1);
		path = path.replace('/', File.separatorChar);
		return path;
	}
}
