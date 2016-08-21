package psp2.UltiNaruto.VitaPadClient;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Main {
	static int GAMEPAD_PORT = 5000;
	
	static int KEY_UP = KeyEvent.VK_W;
	static int KEY_DOWN = KeyEvent.VK_S;
	static int KEY_LEFT = KeyEvent.VK_A;
	static int KEY_RIGHT = KeyEvent.VK_D;
	
	static int KEY_TRIANGLE = KeyEvent.VK_I;
	static int KEY_SQUARE = KeyEvent.VK_J;
	static int KEY_CROSS = KeyEvent.VK_K;
	static int KEY_CIRCLE = KeyEvent.VK_L;
	
	static int KEY_L_TRIGGER = KeyEvent.VK_CONTROL;
	static int KEY_R_TRIGGER = KeyEvent.VK_SPACE;
	static int KEY_START = KeyEvent.VK_ENTER;
	static int KEY_SELECT = KeyEvent.VK_SHIFT;
	
	static int KEY_UP_1 = KeyEvent.VK_UP;
	static int KEY_DOWN_1 = KeyEvent.VK_DOWN;
	static int KEY_LEFT_1 = KeyEvent.VK_LEFT;
	static int KEY_RIGHT_1 = KeyEvent.VK_RIGHT;
	
	static int KEY_UP_2 = KeyEvent.VK_NUMPAD8;
	static int KEY_DOWN_2 = KeyEvent.VK_NUMPAD2;
	static int KEY_LEFT_2 = KeyEvent.VK_NUMPAD4;
	static int KEY_RIGHT_2 = KeyEvent.VK_NUMPAD6;

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

	static class PadPacket {
		long buttons;
		byte lx;
		byte ly;
		byte rx;
		byte ry;
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
		return padPacket;
	}
	
	static boolean isButtonPressed(PadPacket padPacket, long button)
	{
		if((padPacket.buttons & button) != 0)
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
			e.printStackTrace();
			return null;
		}
	}

	static String GetHostFromInput(String[] args)
	{
		if(args.length == 0)
		{
			try {
				String host = "INVALID_HOST";
				byte[] temp = new byte[65535];
				System.out.printf("Insert Vita IP: ");
				int bytes_count = System.in.read(temp);
				host = new String(temp, 0, bytes_count).trim();
				return host;
			} catch(IOException e) {
				return "ERROR";
			}
		}
		else
		{
			return args[0];
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

	public static void main(String[] args)
	{
		System.out.printf("VitaPad Client by UltiNaruto\nHeavily based on Rinnegatamente version\n\n");
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
						SendButton(KEY_L_TRIGGER, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_LTRIGGER.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_LTRIGGER.v()))
						SendButton(KEY_L_TRIGGER, false);
					
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_RTRIGGER.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_RTRIGGER.v()))
						SendButton(KEY_R_TRIGGER, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_RTRIGGER.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_RTRIGGER.v()))
						SendButton(KEY_R_TRIGGER, false);
					
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_START.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_START.v()))
						SendButton(KEY_START, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_START.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_START.v()))
						SendButton(KEY_START, false);
					
					if (isButtonPressed(data, PSP2Buttons.SCE_CTRL_SELECT.v()) && !isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_SELECT.v()))
						SendButton(KEY_SELECT, true);
					else if ((!isButtonPressed(olddata, PSP2Buttons.SCE_CTRL_SELECT.v())) && isButtonPressed(data, PSP2Buttons.SCE_CTRL_SELECT.v()))
						SendButton(KEY_SELECT, false);
					
					if ((data.ly == 0) && (!(olddata.ly == 0))) SendButton(KEY_UP_1, true);
					else if ((olddata.ly == 0) && (!(data.ly  == 0))) SendButton(KEY_UP_1, false);
					if ((data.lx == 0) && (!(olddata.lx == 0))) SendButton(KEY_LEFT_1, true);
					else if ((olddata.lx == 0) && (!(data.lx == 0))) SendButton(KEY_LEFT_1, false);
					if ((data.lx == -1) && (!(olddata.lx == -1))) SendButton(KEY_RIGHT_1, true);
					else if ((olddata.lx == -1) && (!(data.lx == -1))) SendButton(KEY_RIGHT_1, false);
					if ((data.ly == -1) && (!(olddata.ly == -1))) SendButton(KEY_DOWN_1, true);
					else if ((olddata.ly == -1) && (!(data.ly == -1))) SendButton(KEY_DOWN_1, false);
					
					if ((data.ry == 0) && (!(olddata.ry == 0))) SendButton(KEY_UP_2, true);
					else if ((olddata.ry == 0) && (!(data.ry == 0))) SendButton(KEY_UP_2, false);
					if ((data.rx == 0) && (!(olddata.rx == 0))) SendButton(KEY_LEFT_2, true);
					else if ((olddata.rx == 0) && (!(data.rx == 0))) SendButton(KEY_LEFT_2, false);
					if ((data.rx == -1) && (!(olddata.rx == -1))) SendButton(KEY_RIGHT_2, true);
					else if ((olddata.rx == -1) && (!(data.rx == -1))) SendButton(KEY_RIGHT_2, false);
					if ((data.ry == -1) && (!(olddata.ry == -1))) SendButton(KEY_DOWN_2, true);
					else if ((olddata.ry == -1) && (!(data.ry == -1))) SendButton(KEY_DOWN_2, false);
					
					olddata = data;
				}
			} catch (IOException e) {
				break;
			}
		}
		System.exit(1);
	}
}
