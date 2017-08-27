/**
 * 
 */
/**
 * @author NatsuDragnir
 *
 */
package com.wrapper.WrapperMain;

import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import com.wrapper.WrapperMain.utils.JarUtils;
import com.wrapper.WrapperMain.utils.OSDetectionUtils;

import psp2.UltiNaruto.VitaPadClient.Main;

class StreamViewer implements Runnable {
	private final InputStream inputStream;

	StreamViewer(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	private BufferedReader getBufferedReader(InputStream is) {
		return new BufferedReader(new InputStreamReader(is));
	}

	@Override
	public void run() {
		BufferedReader br = getBufferedReader(inputStream);
		String ligne = "";
		try {
			while ((ligne = br.readLine()) != null) {
				System.out.println(ligne);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

public class WrapperMain {
	public static final String ProjectName = "VitaPadClient";
	static void SetLogger() throws IOException
	{
		File logger = new File("log.txt");
		if(!logger.exists())
			logger.createNewFile();
		PrintStream out = new PrintStream(new FileOutputStream(JarUtils.getJarFolder()+"log.txt"));
		System.setOut(out);
	}
	public static void main (String [] args)/* throws IOException, InterruptedException, URISyntaxException, UnsupportedEncodingException*/{
		try {
			//SetLogger();
			if(System.console() == null && !GraphicsEnvironment.isHeadless()){
				if(OSDetectionUtils.isWindows())
				{
					ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "VitaPadClient Java", "java", "-jar", "\""+ProjectName + ".jar\"");
					pb.directory(new File(JarUtils.getJarFolder()));
					Process p = pb.start();
					new Thread(new StreamViewer(p.getInputStream())).start();
					new Thread(new StreamViewer(p.getErrorStream())).start();
				}
				else if(OSDetectionUtils.isMac())
				{
					ProcessBuilder pb = new ProcessBuilder("osascript", "-e", "tell app \"Terminal\" to do script \"clear && java -jar "+JarUtils.getJarFolder()+ProjectName+".jar\"", "-e", "activate app \"Terminal\"", "&", "exit");
					pb.directory(new File(JarUtils.getJarFolder()));
					Process p = pb.start();
					new Thread(new StreamViewer(p.getInputStream())).start();
					new Thread(new StreamViewer(p.getErrorStream())).start();
				} 
				else
				{
					ProcessBuilder pb = new ProcessBuilder("x-terminal-emulator", "-e", "bash -c \"clear;java -jar "+ProjectName + ".jar\"");
					pb.directory(new File(JarUtils.getJarFolder()));
					Process p = pb.start();
					new Thread(new StreamViewer(p.getInputStream())).start();
					new Thread(new StreamViewer(p.getErrorStream())).start();
				}
			}else{
				Main.Start(args);
				System.exit(0);
			}
		} catch (final Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
}