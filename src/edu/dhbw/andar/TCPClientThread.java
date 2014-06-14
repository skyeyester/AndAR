package edu.dhbw.andar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import android.util.Log;

class TCPClientThread extends Thread {
	private byte byteBuffer[] = new byte[1024];
	private OutputStream sckoutstream;
	private ByteArrayOutputStream byoutputstream;
	private Socket tempSocket;
	
	public TCPClientThread(ByteArrayOutputStream myoutputstream, String ipaddr, int port) {
		setPriority(MIN_PRIORITY);
		this.byoutputstream = myoutputstream;
		//this.tempSocket = mySocket;
		try {
			this.tempSocket = new Socket(ipaddr, port);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			myoutputstream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			long startTime = System.currentTimeMillis();
			//sent picture
			this.sckoutstream = tempSocket.getOutputStream();
			ByteArrayInputStream inputstream = new ByteArrayInputStream(byoutputstream.toByteArray());
			StringBuffer amountmsg = new StringBuffer();
			amountmsg.append("size=");
			amountmsg.append(String.valueOf(inputstream.available()));
			amountmsg.append("\r\n");
			Log.e("ThreadLoading","Size="+String.valueOf(inputstream.available()));
			this.sckoutstream.write(amountmsg.toString().getBytes());
			int amount;
			while ((amount = inputstream.read(byteBuffer)) != -1) {
				sckoutstream.write(byteBuffer, 0, amount);
			}
			
			inputstream.close();
			byteBuffer=null;
			amountmsg=null;
			byoutputstream.flush();
			byoutputstream.close();
			this.sckoutstream.flush();
			this.sckoutstream.close();
			this.tempSocket.close();
			
			long stopTime = System.currentTimeMillis();
			long totalTime = stopTime-startTime;
			Log.e("TimeConsumption", String.valueOf(totalTime));
			Log.e("ErrorWriteTest", "Thread dead!!");
			//System.gc();
			//System.runFinalization();
			//System.gc();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//System.exit(0);
	}

}