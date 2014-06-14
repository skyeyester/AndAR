package edu.dhbw.andar.pub;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class TCPFileServerThread extends Thread{
	
	private static final String LOG_TAG = "TCPFileServerThread";
	public static final int MEG_Thread2=2;
	private ServerSocket m_serverSocket = null;
	private DataMessageHandler handler;
	private boolean isContinue;
	
	private Socket socketClient = null;
	private InputStream is = null;
	private DataInputStream dis = null;
	private DataOutputStream writer = null;
	//private String path = "/storage/sdcard0/Android/data/artoolkit"; //ASUS
	private String path = "/mnt/sdcard/Android/data/artoolkit";   //SONY
	private int bufferSize = 2048; //2K
	private byte[] buf;
	
	public TCPFileServerThread(int port, DataMessageHandler hand){
		this.handler = hand;
		this.isContinue = true;
		setPriority(MIN_PRIORITY); //this is 1
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
				socketClient = m_serverSocket.accept();
				Log.i(LOG_TAG, "Accepted connection from " + socketClient.getInetAddress().getHostAddress());
				// Read input from client socket
		        is = socketClient.getInputStream();
		        dis = new DataInputStream(is);
		        buf = new byte[bufferSize];
		        while (!socketClient.isClosed()){
		        	// Read a line as filename
	            	String fileName = dis.readLine();
	            	if (fileName == null)
                    {	
	    				buf = null;
	    				try {
	    					dis.close();
	    					is.close();   
	    					socketClient.close(); 
	    				} catch (IOException e) {
	    					// TODO Auto-generated catch block
	    					e.printStackTrace();
	    				}
                        break;
                    }
	            	
	            	Log.i(LOG_TAG, "Read file client socket=[" + fileName + "]");
	            	String filepath = path + "/" + fileName;
	            	if(isFileExist(filepath) == false){
	            		File downLoadFile = new File(filepath);
	            		downLoadFile.createNewFile();
	            		writer = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(downLoadFile)));
	            		int read = 0;
	            		while((read = dis.read(buf)) != -1){   
	            			writer.write(buf, 0, read);   
	            		}
	            		Bundle regmsgObj = new Bundle();
	            		Message regresult = new Message();
	            		regmsgObj.putString("pattName", fileName);
	            		regresult.setData(regmsgObj);
	            		regresult.what = MEG_Thread2;
	            		handler.sendMessage(regresult);
						writer.close();
	            	}else{
	            		Log.i(LOG_TAG,"pattern file is already exist");
	    				buf = null;
	    				try {
	    					dis.close();
	    					is.close();   
	    					socketClient.close(); 
	    				} catch (IOException e) {
	    					// TODO Auto-generated catch block
	    					e.printStackTrace();
	    				}
	            		break;
	            	}
		        }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			yield();
		 }
	}
	
	/**
	 * 
	 * Terminate the Thread 
	 * */
    public void terminate() { 
        isContinue = false; 
    }
    
	/**
	 * 
	 * Check file exist or not
	 * 
	 * @param path file's full path
	 * @return true/false exist/not exist
	 * */
    public static boolean isFileExist(String path){
        return new File(path).exists();
    }

}
