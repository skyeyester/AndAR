package edu.dhbw.andar.pub;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import edu.dhbw.andar.ARToolkit;
import edu.dhbw.andar.AndARActivity;
import edu.dhbw.andar.exceptions.AndARException;
import edu.dhbw.andopenglcam.R;

/**
 * Example of an application that makes use of the AndAR toolkit.
 * @author CCWU
 *
 */
public class CustomActivity extends AndARActivity {
	
	private final int MENU_SCREENSHOT = 0;

	private CustomObject someObject;
	private ARToolkit artoolkit;
	//private String path = "/storage/sdcard0/Android/data/artoolkit"; //ASUS
	private String path = "/mnt/sdcard/Android/data/artoolkit";   //SONY
	
	//Networking modify by ccwu
	private String ip;
	private String port_string;
	private int port;
	private TCPServerThread serverThread;
	private TCPFileServerThread serverFileThread;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		// get IP address
		Bundle bundle = this.getIntent().getExtras();
		if(bundle != null){
		    ip = bundle.getString("ipaddr");
		   	port_string = bundle.getString("port");
		   	port = Integer.valueOf(port_string);
		}else{
		    Log.i("CustomActivity", "Can't get IP address");
		}
		//
		CustomRenderer renderer = new CustomRenderer();//optional, may be set to null
		super.setNonARRenderer(renderer);//or might be omited
		
		/*********** get all file names in the directory***************/
		/*
		File dir = new File(path);
		String[] filename;
		if (dir.isDirectory()){
			
			File [] pattfiles = dir.listFiles(new FilenameFilter() {
			    @Override
			    public boolean accept(File dir, String name) {
			        return name.endsWith(".patt");
			    }
				});
			
			File [] desfiles = dir.listFiles(new FilenameFilter() {
			    @Override
			    public boolean accept(File dir, String name) {
			        return name.endsWith(".des");
			    }
				});
			for (File file : pattfiles) {
				Log.i("CustomActivity","get: "+file.getName());
			}
	
		}else{
			Log.i("CustomActivity","Error in open directory");
		}
		*/
		
		try {
			artoolkit = super.getArtoolkit();
			artoolkit.setIPAddress(ip);
			artoolkit.setPoreNumber(port);
			/*
			someObject = new CustomObject
				("Hiro", "patt.hiro", 80.0, new double[]{0,0});
			artoolkit.registerARObject(someObject);
			*/
			someObject = new CustomObject
			("Android", "android.patt", 80.0, new double[]{0,0}, new float[]{1,0,1});
			artoolkit.registerARObject(someObject);
			
			someObject = new CustomObject
			("Barcode", "barcode.patt", 80.0, new double[]{0,0}, new float[]{0,1,1});
			artoolkit.registerARObject(someObject);
			
			someObject = new CustomObject
			("MarCloud", "mar.patt", 80.0, new double[]{0,0});
			artoolkit.registerARObject(someObject);
			/*
			someObject = new CustomObject
			("Lenna", "lenna.patt", 80.0, new double[]{0,0}, new float[]{0,1,1});
			artoolkit.registerARObject(someObject, path);
			
			someObject = new CustomObject
			("Human", "kanji.patt", 80.0, new double[]{0,0}, new float[]{0,1,1});
			artoolkit.registerARObject(someObject, path);
			*/
		} catch (AndARException ex){
			//handle the exception, that means: show the user what happened
			Log.e("CustomActivity","Error in onCreate");
		}
		
		/////////////modify by CCWU//////////////////
		//Deal with Recognize Result
		DataMessageHandler reghandler = new DataMessageHandler(this);
		//receive recognize result
		serverThread = new TCPServerThread(8888, reghandler);
		serverThread.start();
		//receive pattern file
		serverFileThread = new TCPFileServerThread(6666, reghandler);
		serverFileThread.start();
	}
	
	@Override
	protected void onPause() {
		Log.i("CustomActivity", "CustomActivity onPause start");
		serverThread.terminate();
		serverFileThread.terminate();
		super.onPause();
		Log.i("CustomActivity", "CustomActivity onPause finish");
	}
	/**
	 * Inform the user about exceptions that occurred in background threads.
	 * This exception is rather severe and can not be recovered from.
	 * Inform the user and shut down the application.
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		Log.e("AndAR EXCEPTION", ex.getMessage());
		this.finish();
	}	
	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {		
		menu.add(0, MENU_SCREENSHOT, 0, getResources().getText(R.string.takescreenshot))
		.setIcon(R.drawable.screenshoticon);
		return true;
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/*if(item.getItemId()==1) {
			artoolkit.unregisterARObject(someObject);
		} else if(item.getItemId()==0) {
			try {
				someObject = new CustomObject
				("test", "patt.hiro", 80.0, new double[]{0,0});
				artoolkit.registerARObject(someObject);
			} catch (AndARException e) {
				e.printStackTrace();
			}
		}*/
		switch(item.getItemId()) {
		case MENU_SCREENSHOT:
			new TakeAsyncScreenshot().execute();
			break;
		}
		return true;
	}
	
	class TakeAsyncScreenshot extends AsyncTask<Void, Void, Void> {
		
		private String errorMsg = null;

		@Override
		protected Void doInBackground(Void... params) {
			Bitmap bm = takeScreenshot();
			FileOutputStream fos;
			try {
				fos = new FileOutputStream("/sdcard/AndARScreenshot"+new Date().getTime()+".png");
				bm.compress(CompressFormat.PNG, 100, fos);
				fos.flush();
				fos.close();					
			} catch (FileNotFoundException e) {
				errorMsg = e.getMessage();
				e.printStackTrace();
			} catch (IOException e) {
				errorMsg = e.getMessage();
				e.printStackTrace();
			}	
			return null;
		}
		
		protected void onPostExecute(Void result) {
			if(errorMsg == null)
				Toast.makeText(CustomActivity.this, getResources().getText(R.string.screenshotsaved), Toast.LENGTH_SHORT ).show();
			else
				Toast.makeText(CustomActivity.this, getResources().getText(R.string.screenshotfailed)+errorMsg, Toast.LENGTH_SHORT ).show();
		};
		
	}
	
	
}
