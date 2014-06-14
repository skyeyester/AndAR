package edu.dhbw.andar.pub;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class TCPServerThread extends Thread{
	
	private static final String LOG_TAG = "TCPServerThread";
	public static final int MEG_Thread=1;
	private ServerSocket m_serverSocket = null;
	DataMessageHandler handler;
	private boolean isContinue;
	/**
	 * 
	 * constructor method
	 * */
	
	public TCPServerThread(int port, DataMessageHandler hand){
		/** 
		 * java.lang.Thread.setPriority (int priority)
		 * from low to high :1~10
		 * **/
		
		setPriority(MIN_PRIORITY); //this is 1
		//setPriority(NORM_PRIORITY); //this is 5
		this.handler = hand;
		this.isContinue = true;
		try {
			m_serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void run(){
		 while(isContinue){
			 // Wait for new client connection
			 Log.i(LOG_TAG, "Waiting for client connection...");
			 try {
				Socket socketClient;
				socketClient = m_serverSocket.accept();
				Log.i(LOG_TAG, "Accepted connection from " + socketClient.getInetAddress().getHostAddress());
				// Read input from client socket
	            InputStream is = socketClient.getInputStream();
	            DataInputStream dis = new DataInputStream(is);
	            while (!socketClient.isClosed()){
	            	// Read a line
	            	String sLine = dis.readLine();
	            	if (sLine == null)
                    {
                        break;
                    }
	            	Log.i(LOG_TAG, "Read client socket=[" + sLine + "]");
	    			Bundle regmsgObj = new Bundle();
	        		Message regresult = new Message();
	        		regmsgObj.putString("regmsg", sLine);
	        		regresult.setData(regmsgObj);
    				regresult.what = MEG_Thread;
    				handler.sendMessage(regresult);
	            }
	            // Close streams
	            dis.close();
	            is.close();
	            // Close client socket
	            Log.i(LOG_TAG, "Read data from client ok. Close connection from " + socketClient.getInetAddress().getHostAddress());
	            socketClient.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			yield();
		 }//while loop for accept new client
		 
		 try {
			m_serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * terminate the Thread 
	 * */
    public void terminate() { 
        this.isContinue = false; 
    } 
}
