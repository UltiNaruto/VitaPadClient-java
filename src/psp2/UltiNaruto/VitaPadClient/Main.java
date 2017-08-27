package psp2.UltiNaruto.VitaPadClient;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;

import com.wrapper.WrapperMain.utils.JarUtils;

import psp2.UltiNaruto.VitaPadClient.configuration.InvalidConfigurationException;
import psp2.UltiNaruto.VitaPadClient.configuration.file.FileConfiguration;
import psp2.UltiNaruto.VitaPadClient.configuration.file.YamlConfiguration;

public class Main {
	static FileConfiguration config;
	static File configFile;
	static long lastCheck;
	static int PSP2_SCREEN_WIDTH = 1920;
	static int PSP2_SCREEN_HEIGHT = 1088;
	
	static int GAMEPAD_PORT = 5000;
	
	static int KEY_UP = KeyEvent.VK_W;
	static int KEY_DOWN = KeyEvent.VK_S;
	static int KEY_LEFT = KeyEvent.VK_A;
	static int KEY_RIGHT = KeyEvent.VK_D;
	
	static int KEY_TRIANGLE = KeyEvent.VK_I;
	static int KEY_SQUARE = KeyEvent.VK_J;
	static int KEY_CROSS = KeyEvent.VK_K;
	static int KEY_CIRCLE = KeyEvent.VK_L;
	
	static int KEY_L = KeyEvent.VK_CONTROL;
	static int KEY_R = KeyEvent.VK_SPACE;
	static int KEY_START = KeyEvent.VK_ENTER;
	static int KEY_SELECT = KeyEvent.VK_SHIFT;
	
	static int KEY_LANALOG_UP = KeyEvent.VK_UP;
	static int KEY_LANALOG_DOWN = KeyEvent.VK_DOWN;
	static int KEY_LANALOG_LEFT = KeyEvent.VK_LEFT;
	static int KEY_LANALOG_RIGHT = KeyEvent.VK_RIGHT;
	
	static int KEY_RANALOG_UP = KeyEvent.VK_NUMPAD8;
	static int KEY_RANALOG_DOWN = KeyEvent.VK_NUMPAD2;
	static int KEY_RANALOG_LEFT = KeyEvent.VK_NUMPAD4;
	static int KEY_RANALOG_RIGHT = KeyEvent.VK_NUMPAD6;

	static enum PSP2Buttons {
		SCE_CTRL_SELECT(0x000001),	//!< Select button.
		SCE_CTRL_START(0x000008),	//!< Start button.
		SCE_CTRL_UP(0x000010),	//!< Up D-Pad button.
		SCE_CTRL_RIGHT(0x000020),	//!< Right D-Pad button.
		SCE_CTRL_DOWN(0x000040),	//!< Down D-Pad button.
		SCE_CTRL_LEFT(0x000080),	//!< Left D-Pad button.
		SCE_CTRL_LTRIGGER(0x000100),	//!< Left trigger.
		SCE_CTRL_RTRIGGER(0x000200),	//!< Right trigger.
		SCE_CTRL_TRIANGLE(0x001000),	//!< Triangle button.
		SCE_CTRL_CIRCLE(0x002000),	//!< Circle button.
		SCE_CTRL_CROSS(0x004000),	//!< Cross button.
		SCE_CTRL_SQUARE(0x008000),	//!< Square button.
		SCE_CTRL_ANY(0x010000);	//!< Any input intercepted.
		long value;
		PSP2Buttons(long v)
		{
			this.value = v;
		}

		long v()
		{
			return value;
		}
	};
	
	static enum PSP2TouchScreenEvents {
		NO_INPUT(0),
		MOUSE_MOV(0x01),
		LEFT_CLICK(0x08),
		RIGHT_CLICK(0x10);
		int value;
		PSP2TouchScreenEvents(int v)
		{
			this.value = v;
		}

		int v()
		{
			return value;
		}
	}

	static class PadPacket {
		long buttons;
		byte lx;
		byte ly;
		byte rx;
		byte ry;
		short tx;
		short ty;
		byte click;
	}
	
	static short toShort(byte[] bytes, int offset) {
		short ret = 0;
		for (int i=1; i>=0 && i+offset<bytes.length; i--) {
			ret <<= 8;
			ret |= (short)bytes[i] & 0xFF;
		}
		return ret;
	}

	static int toInt(byte[] bytes, int offset) {
		int ret = 0;
		for (int i=3; i>=0 && i+offset<bytes.length; i--) {
			ret <<= 8;
			ret |= (int)bytes[i] & 0xFF;
		}
		return ret;
	}

	static PadPacket getPadPacketFromBytes(byte[] datas)
	{
		if(datas.length != 8)
			return null;
		PadPacket padPacket = new PadPacket();
		padPacket.buttons = toInt(datas,0);
		padPacket.lx = datas[4];
		padPacket.ly = datas[5];
		padPacket.rx = datas[6];
		padPacket.ry = datas[7];
		padPacket.tx = toShort(datas, 8);
		padPacket.ty = toShort(datas, 10);
		padPacket.click = datas[12];
		return padPacket;
	}
	
	static boolean isButtonPressed(PadPacket padPacket, long button)
	{
		if((padPacket.buttons & button) != 0)
			return true;
		return false;
	}
	
	static boolean isEventCaught(byte src, PSP2TouchScreenEvents dst)
	{
		if((src & dst.v()) != 0)
			return true;
		return false;
	}

	static DataInputStream getSocketInputStream(Socket socket)
	{
		try {
			return new DataInputStream(socket.getInputStream());
		} catch (IOException e) {
			return null;
		}
	}

	static DataOutputStream getSocketOutputStream(Socket socket)
	{
		try {
			return new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			return null;
		}
	}

	static Socket CreateSocket(String IP, int port)
	{
		try {
			return new Socket(IP, port);
		} catch (IOException e) {
			e.printStackTrace(System.out);
			return null;
		}
	}

	@SuppressWarnings("resource")
	static String GetHostFromInput(String[] args)
	{
		if(args.length == 0)
		{
			String host = "INVALID_HOST";
			host = new Scanner(System.in).next().trim();
			System.out.println();
			return host;
		}
		else
		{
			return args[0];
		}
	}
	
	static void SendMouseMove(int x, int y) {
		try {
			Point mouse_pos = MouseInfo.getPointerInfo().getLocation();			
			new Robot().mouseMove(x - mouse_pos.x, y - mouse_pos.y);
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}
	
	static void SendButton(int key, boolean pressed) {
		try {
			if(pressed)
				new Robot().keyPress(key);
			else
				new Robot().keyRelease(key);
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}
	
	static void SendMouseButton(int key, boolean pressed) {
		try {
			if(pressed)
				new Robot().mousePress(key);
			else
				new Robot().mouseRelease(key);
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}
	
	static boolean loadConfig()
	{
		try {
			configFile = new File(new File(JarUtils.getJarFolder()), "config.yml");
			config = new YamlConfiguration();
			boolean isExisting = configFile.exists() && (configFile.exists() && !configFile.isFile());
			if(!isExisting)
			{
				config.options().header("Windows and Linux key codes : https://www.cambiaresearch.com/articles/15/javascript-char-codes-key-codes\nFor mac users read above and this : https://www.lifewire.com/what-are-windows-keyboard-equivalents-to-mac-2260203");
				config.options().copyHeader(true);
				config.addDefault("input_mode", "default");
				config.addDefault("keys.default.KEY_DOWN", KeyEvent.VK_S);
				config.addDefault("keys.default.KEY_UP", KeyEvent.VK_W);
				config.addDefault("keys.default.KEY_LEFT", KeyEvent.VK_A);
				config.addDefault("keys.default.KEY_RIGHT", KeyEvent.VK_D);
				config.addDefault("keys.default.KEY_TRIANGLE", KeyEvent.VK_I);
				config.addDefault("keys.default.KEY_SQUARE", KeyEvent.VK_J);
				config.addDefault("keys.default.KEY_CROSS", KeyEvent.VK_K);
				config.addDefault("keys.default.KEY_CIRCLE", KeyEvent.VK_L);
				config.addDefault("keys.default.KEY_L", KeyEvent.VK_CONTROL);
				config.addDefault("keys.default.KEY_R", KeyEvent.VK_SPACE);
				config.addDefault("keys.default.KEY_START", KeyEvent.VK_ENTER);
				config.addDefault("keys.default.KEY_SELECT", KeyEvent.VK_SHIFT);
				config.addDefault("keys.default.KEY_LANALOG_UP", KeyEvent.VK_UP);
				config.addDefault("keys.default.KEY_LANALOG_DOWN", KeyEvent.VK_DOWN);
				config.addDefault("keys.default.KEY_LANALOG_LEFT", KeyEvent.VK_LEFT);
				config.addDefault("keys.default.KEY_LANALOG_RIGHT", KeyEvent.VK_RIGHT);
				config.addDefault("keys.default.KEY_RANALOG_UP", KeyEvent.VK_NUMPAD8);
				config.addDefault("keys.default.KEY_RANALOG_DOWN", KeyEvent.VK_NUMPAD2);
				config.addDefault("keys.default.KEY_RANALOG_LEFT", KeyEvent.VK_NUMPAD4);
				config.addDefault("keys.default.KEY_RANALOG_RIGHT", KeyEvent.VK_NUMPAD6);
				config.options().copyDefaults(true);
				config.save(configFile);
				lastCheck = configFile.lastModified();
			}
			else
			{
				config.load(configFile);
				if(config.getString("input_mode") == "default")
				{
					KEY_DOWN = config.getInt("keys.default.KEY_DOWN");
					KEY_UP = config.getInt("keys.default.KEY_UP");
					KEY_LEFT = config.getInt("keys.default.KEY_LEFT");
					KEY_RIGHT = config.getInt("keys.default.KEY_RIGHT");
					KEY_TRIANGLE = config.getInt("keys.default.KEY_TRIANGLE");
					KEY_SQUARE = config.getInt("keys.default.KEY_SQUARE");
					KEY_CROSS = config.getInt("keys.default.KEY_CROSS");
					KEY_CIRCLE = config.getInt("keys.default.KEY_CIRCLE");
					KEY_L = config.getInt("keys.default.KEY_L");
					KEY_R = config.getInt("keys.default.KEY_R");
					KEY_START = config.getInt("keys.default.KEY_START");
					KEY_SELECT = config.getInt("keys.default.KEY_SELECT");
					KEY_LANALOG_UP = config.getInt("keys.default.KEY_LANALOG_UP");
					KEY_LANALOG_DOWN = config.getInt("keys.default.KEY_LANALOG_DOWN");
					KEY_LANALOG_LEFT = config.getInt("keys.default.KEY_LANALOG_LEFT");
					KEY_LANALOG_RIGHT = config.getInt("keys.default.KEY_LANALOG_RIGHT");
					KEY_RANALOG_UP = config.getInt("keys.default.KEY_RANALOG_UP");
					KEY_RANALOG_DOWN = config.getInt("keys.default.KEY_RANALOG_DOWN");
					KEY_RANALOG_LEFT = config.getInt("keys.default.KEY_RANALOG_LEFT");
					KEY_RANALOG_RIGHT = config.getInt("keys.default.KEY_RANALOG_RIGHT");
				}
				lastCheck = configFile.lastModified();
			}
			return true;
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static void Start(String[] args)
	{
		loadConfig();
		System.out.printf("VitaPad Client by UltiNaruto\nHeavily based on Rinnegatamente version\n\n");
		System.out.printf("Insert Vita IP: ");
		String host = GetHostFromInput(args);
		System.out.printf("IP: %s\nPort: %d\n\n", host, GAMEPAD_PORT);
		Socket my_socket = CreateSocket(host, GAMEPAD_PORT);
		if(my_socket == null)
		{
			System.out.printf("\nFailed creating socket.");
			System.exit(-1);
		}
		System.out.printf("\nClient socket created on port 5000");
		if(getSocketInputStream(my_socket) == null || getSocketOutputStream(my_socket) == null)
		{
			System.out.printf("\nFailed connecting server.");
			System.exit(-1);
		}
		System.out.printf("\nConnection established!");
		boolean firstScan = true;
		PadPacket data = new PadPacket();
		PadPacket olddata = new PadPacket();
		for(;;) {
			try {
				if(configFile.lastModified() != lastCheck)
					loadConfig();
				getSocketOutputStream(my_socket).write("request\0".getBytes("UTF-8"), 0, 8);
				byte[] bDatas = new byte[256];
				int count = getSocketInputStream(my_socket).read(bDatas, 0, 256);
				ByteBuffer bRealDatas = ByteBuffer.allocate(count);
				bRealDatas.put(bDatas, 0, count);
				data = getPadPacketFromBytes(bRealDatas.array());
				if (firstScan) {
					olddata = data;
					firstScan = false;
				}
				if(count != 0)
				{
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_DOWN.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_DOWN.v()))
						SendButton(KEY_DOWN, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_DOWN.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_DOWN.v()))
						SendButton(KEY_DOWN, false);
					
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_UP.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_UP.v()))
						SendButton(KEY_UP, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_UP.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_UP.v()))
						SendButton(KEY_UP, false);
					
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_LEFT.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_LEFT.v()))
						SendButton(KEY_LEFT, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_LEFT.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_LEFT.v()))
						SendButton(KEY_LEFT, false);
					
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_RIGHT.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_RIGHT.v()))
						SendButton(KEY_RIGHT, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_RIGHT.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_RIGHT.v()))
						SendButton(KEY_RIGHT, false);
					
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_TRIANGLE.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_TRIANGLE.v()))
						SendButton(KEY_TRIANGLE, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_TRIANGLE.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_TRIANGLE.v()))
						SendButton(KEY_TRIANGLE, false);
					
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_SQUARE.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_SQUARE.v()))
						SendButton(KEY_SQUARE, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_SQUARE.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_SQUARE.v()))
						SendButton(KEY_SQUARE, false);
					
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_CROSS.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_CROSS.v()))
						SendButton(KEY_CROSS, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_CROSS.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_CROSS.v()))
						SendButton(KEY_CROSS, false);
					
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_CIRCLE.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_CIRCLE.v()))
						SendButton(KEY_CIRCLE, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_CIRCLE.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_CIRCLE.v()))
						SendButton(KEY_CIRCLE, false);
					
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_LTRIGGER.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_LTRIGGER.v()))
						SendButton(KEY_L, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_LTRIGGER.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_LTRIGGER.v()))
						SendButton(KEY_L, false);
					
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_RTRIGGER.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_RTRIGGER.v()))
						SendButton(KEY_R, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_RTRIGGER.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_RTRIGGER.v()))
						SendButton(KEY_R, false);
					
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_START.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_START.v()))
						SendButton(KEY_START, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_START.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_START.v()))
						SendButton(KEY_START, false);
					
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_SELECT.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_SELECT.v()))
						SendButton(KEY_SELECT, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_SELECT.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_SELECT.v()))
						SendButton(KEY_SELECT, false);
					
					if ((data.ly == 0) && (!(olddata.ly == 0))) SendButton(KEY_LANALOG_UP, true);
					else if ((olddata.ly == 0) && (!(data.ly  == 0))) SendButton(KEY_LANALOG_UP, false);
					if ((data.lx == 0) && (!(olddata.lx == 0))) SendButton(KEY_LANALOG_LEFT, true);
					else if ((olddata.lx == 0) && (!(data.lx == 0))) SendButton(KEY_LANALOG_LEFT, false);
					if ((data.lx == -1) && (!(olddata.lx == -1))) SendButton(KEY_LANALOG_RIGHT, true);
					else if ((olddata.lx == -1) && (!(data.lx == -1))) SendButton(KEY_LANALOG_RIGHT, false);
					if ((data.ly == -1) && (!(olddata.ly == -1))) SendButton(KEY_LANALOG_DOWN, true);
					else if ((olddata.ly == -1) && (!(data.ly == -1))) SendButton(KEY_LANALOG_DOWN, false);
					
					if ((data.ry == 0) && (!(olddata.ry == 0))) SendButton(KEY_RANALOG_UP, true);
					else if ((olddata.ry == 0) && (!(data.ry == 0))) SendButton(KEY_RANALOG_UP, false);
					if ((data.rx == 0) && (!(olddata.rx == 0))) SendButton(KEY_RANALOG_LEFT, true);
					else if ((olddata.rx == 0) && (!(data.rx == 0))) SendButton(KEY_RANALOG_LEFT, false);
					if ((data.rx == -1) && (!(olddata.rx == -1))) SendButton(KEY_RANALOG_RIGHT, true);
					else if ((olddata.rx == -1) && (!(data.rx == -1))) SendButton(KEY_RANALOG_RIGHT, false);
					if ((data.ry == -1) && (!(olddata.ry == -1))) SendButton(KEY_RANALOG_DOWN, true);
					else if ((olddata.ry == -1) && (!(data.ry == -1))) SendButton(KEY_RANALOG_DOWN, false);
					
					if (data.click != PSP2TouchScreenEvents.NO_INPUT.v()){
						if (isEventCaught(data.click, PSP2TouchScreenEvents.MOUSE_MOV)) SendMouseMove(data.tx, data.ty);
						if (isEventCaught(data.click, PSP2TouchScreenEvents.LEFT_CLICK) && !isEventCaught(olddata.click, PSP2TouchScreenEvents.LEFT_CLICK)) SendMouseButton(InputEvent.BUTTON1_MASK, true);
						else if (isEventCaught(olddata.click, PSP2TouchScreenEvents.LEFT_CLICK) && !isEventCaught(data.click, PSP2TouchScreenEvents.LEFT_CLICK)) SendMouseButton(InputEvent.BUTTON1_MASK, false);
						if (isEventCaught(data.click, PSP2TouchScreenEvents.RIGHT_CLICK) && !isEventCaught(olddata.click, PSP2TouchScreenEvents.RIGHT_CLICK)) SendMouseButton(InputEvent.BUTTON3_MASK, true);
						else if (isEventCaught(olddata.click, PSP2TouchScreenEvents.RIGHT_CLICK) && !isEventCaught(data.click, PSP2TouchScreenEvents.RIGHT_CLICK)) SendMouseButton(InputEvent.BUTTON3_MASK, false);
					}
					
					olddata = data;
				}
			} catch (IOException e) {
				e.printStackTrace(System.out);
				break;
			}
		}
		System.exit(1);
	}
}
