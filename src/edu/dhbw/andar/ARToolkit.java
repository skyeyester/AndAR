/**
	Copyright (C) 2009,2010  Tobias Domhan

    This file is part of AndOpenGLCam.

    AndObjViewer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    AndObjViewer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with AndObjViewer.  If not, see <http://www.gnu.org/licenses/>.
 
 */
package edu.dhbw.andar;


import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.microedition.khronos.opengles.GL10;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;
import android.widget.Toast;
import edu.dhbw.andar.exceptions.AndARException;
import edu.dhbw.andar.interfaces.MarkerVisibilityListener;
import edu.dhbw.andar.pub.CustomActivity;
import edu.dhbw.andar.pub.DataMessageHandler;
import edu.dhbw.andar.util.GraphicsUtil;
import edu.dhbw.andar.util.IO;

/**
 * Interface to the ARToolkit.
 * @author Tobias Domhan
 *
 */
public class ARToolkit {
	private final Resources res;
	private final String calibFileName = "camera_para.dat";
	//private double[] glTransMat = new double[16];
	private boolean initialized = false;
	private int screenWidth = 0;
	private int screenHeight = 0;
	private int imageWidth = 0;
	private int imageHeight = 0;
	
	//modify by ccwu
	private final CameraStatus cameraState;
	//Network modify by ccwu
	private String ipname_instream;
	private int portnumber;
	private boolean remoteQuery;
	
	/**
	 * Every object get'S his own unique ID. 
	 * This counter may never be decremented.
	 */
	private int nextObjectID = 0;
	/**
	 * The transformation matrix is accessed, when drawing the object(read),
	 * but is also written to when detecting the markers from a different thread.
	 */
	private Object transMatMonitor = new Object();
	//modify by ccwu
	private DetectMarkerWorker detectMarkerWorker;
	private List<MarkerVisibilityListener> visListeners = new ArrayList<MarkerVisibilityListener>();
	private Vector<ARObject> arobjects = new Vector<ARObject>();
	/**
	 * absolute path of the local files:
	 * the calib file will be stored there, among other things
	 */
	private File baseFolder;
	
	
	public ARToolkit(Resources res, File baseFile, CameraStatus camStatus) {
		artoolkit_init();
		this.baseFolder = baseFile;
		this.res = res;
		//modify by ccwu
		this.cameraState = camStatus;
		this.detectMarkerWorker = new DetectMarkerWorker(cameraState);
		this.remoteQuery = false;
	}
	
	/**
	 * Registers an object to the ARToolkit. This means:
	 * The toolkit will try to determine the pose of the object.
	 * If it is visible the draw method of the object will be invoked.
	 * The corresponding translation matrix will be applied inside opengl
	 * before doing so.
	 * TODO: registering a object with the same pattern twice will not work, as arloadpatt will create different IDs for the same pattern, and the detecting function will return only the first id as being detected. we need to store patt load id's in an hash -> loadpatt as a native function returning the ID -> pass this id to the object registering function.
	 * @param arobject The object that shell be registered.
	 */
	public synchronized void registerARObject(ARObject arobject) 
		throws AndARException{	
		if(arobjects.contains(arobject)) 
			return;//don't register the same object twice
		try {
			//transfer pattern file to private space
			IO.transferFileToPrivateFS(baseFolder,
					arobject.getPatternName(), res);
			arobjects.add(arobject);
			arobject.setId(nextObjectID);
			String patternFile = baseFolder.getAbsolutePath() + File.separator + 
			arobject.getPatternName();
			addObject(nextObjectID, arobject, patternFile,
					arobject.getMarkerWidth(), arobject.getCenter());
			nextObjectID++;
		} catch (IOException e) {
			e.printStackTrace();
			throw new AndARException(e.getMessage());
		}		
	}
	
	
	public synchronized void unregisterARObject(ARObject arobject) {
		if(arobjects.contains(arobject)) {
			arobjects.remove(arobject);
			//remove from the native library
			removeObject(arobject.getId());
		}
	}
	
	/**
	 * Registers an object which in External storage (SD Card) to the ARToolkit
	 * //modify by ccwu
	 * 
	 * @param arobject The object that shell be registered.
	 * @param path The location dictionary that the object shell be loaded  
	 */
	public synchronized void registerARObject(ARObject arobject, String path) throws AndARException{
		if(arobjects.contains(arobject)){
			return;//don't register the same object twice
		}
		arobjects.add(arobject);
		arobject.setId(nextObjectID);
		//open pattern file in SD card
		String patternFile = path + File.separator + arobject.getPatternName();
		addObject(nextObjectID, arobject, patternFile,
				arobject.getMarkerWidth(), arobject.getCenter());
		nextObjectID++;
	}
	
	/**
	 * native libraries
	 */
	static {
		//arToolkit
		System.loadLibrary( "ar" );
	}
	
	/**
	 * Register a object to the native library. From now on the detection function will determine
	 * if the given object is visible on a marker, and set the transformation matrix accordingly.
	 * @param id a unique ID of the object
	 * @param patternName the fileName of the pattern
	 * @param markerWidth the width of the object
	 * @param markerCenter the center of the object
	 */
	private native void addObject(int id, ARObject obj, String patternName, double markerWidth, double[] markerCenter);
	
	/**
	 * Remove the object from the list of registered objects.
	 * @param id the id of the object.
	 */
	private native void removeObject(int id);
	
	/**
	 * Do some basic initialization, like creating data structures.
	 */
	private native void artoolkit_init();
	
	/**
	 * Do initialization specific to the image/screen dimensions.
	 * @param imageWidth width of the image data
	 * @param imageHeight height of the image data
	 * @param screenWidth width of the screen
	 * @param screenHeight height of the screen
	 */
	private native void artoolkit_init(String filesFolder,int imageWidth, int imageHeight,
			int screenWidth, int screenHeight);
	
	/**
	 * detect the markers in the frame
	 * @param in the image 
	 * @param matrix the transformation matrix for each marker, will be locked right before the trans matrix will be altered
	 * @return number of markers
	 */
	private native int artoolkit_detectmarkers(byte[] in, Object transMatMonitor);
	
	/**
	 * Inverse a three by four matrix.
	 * Matrix has to be 3 by 4! (but is actually a 4 by 4 homogene matrix.)
	 * @param mat1 contains the matrix to be inversed.
	 * @param mat2 will contain the resulting matrix.
	 * @return success?
	 */
	public native static int  arUtilMatInv(double[] mat1, double[] mat2);
	
	/**
	 * Multiply one (three by four) matrix by another.
	 * Matrix has to be 3 by 4! (but is actually a 4 by 4 homogene matrix.)
	 * @param multiplier
	 * @param multiplicand
	 * @param result contains the result, after the method returns.
	 * @return
	 */
	public native static int  arUtilMatMul(double[] multiplier, double[] multiplicand, double[] result);
	
	
	/**
	 * 
	 * @param width of the screen
	 * @param height of the screen
	 */
	protected void setScreenSize(int width, int height) {
		if(Config.DEBUG)
			Log.i("MarkerInfo", "setting screen width("+width+") and height("+height+")");
		this.screenWidth = width;
		this.screenHeight = height;
		initialize();
	}
	
	/**
	 * 
	 * @param width of the image
	 * @param height of the image
	 */
	protected void setImageSize(int width, int height) {
		if(Config.DEBUG)
			Log.i("MarkerInfo", "setting image width("+width+") and height("+height+")");
		this.imageWidth = width;
		this.imageHeight = height;
		initialize();
	}
	
	private void initialize() {
		//make sure all sizes are set
		if(screenWidth>0 && screenHeight>0&&imageWidth>0&&imageHeight>0) {
			if(Config.DEBUG)
				Log.i("MarkerInfo", "going to initialize the native library now");
			artoolkit_init(baseFolder+File.separator+calibFileName, imageWidth, imageHeight, screenWidth, screenHeight);	
			ARObject.glCameraMatrixBuffer = GraphicsUtil.makeFloatBuffer(ARObject.glCameraMatrix);
			if(Config.DEBUG)
				Log.i("MarkerInfo", "alright, done initializing the native library");
			initialized = true;
		}
	}
	
	/**
	 * Detects the markers in the image, and updates the state of the
	 * MarkerInfo accordingly.
	 * @param image
	 */
	public final void detectMarkers(byte[] image) {
		//make sure we initialized the native library
		if(initialized) {			
			detectMarkerWorker.nextFrame(image);
		}
	}
	
	/**
	 * Draw all ARObjects.
	 * @param gl
	 */
	public final void draw(GL10 gl) {
		if(initialized) {
			int counter = 0;
			if(Config.DEBUG)
				Log.i("MarkerInfo", "going to draw opengl stuff now");
			for (ARObject obj : arobjects) {
				if(obj.isVisible()){
					//modify by ccwu
					//draw the 3D model
					Log.i("ARToolKit","draw 3D model");
					obj.draw(gl);
				    //show the marker's name
				    //need to modify
				}else{
					//record the non-visible markers
					counter = counter+1;
				}
			}
			if(counter == arobjects.size()){
				//there is a marker, but don't be register in the local machine
				// or no show in the image frame
				//so visible == false
				//modify by ccwu
				remoteQuery = true;
			}
		}
	}
	
	/**
	 * initialize the objects.
	 * @param gl
	 */
	public final void initGL(GL10 gl) {
		for (ARObject obj : arobjects) {
			if(obj.isVisible())
				obj.init(gl);
		}
	}
	
	
	
	/** 
	 * @param visListener listener to add to the registered listeners.
	 * @deprecated Use addVisibilityListener instead.
	 */
	@Deprecated
	public void setVisListener(MarkerVisibilityListener visListener) {		
		this.visListeners.add(visListener);
	}
	
	public void addVisibilityListener(
			MarkerVisibilityListener markerVisibilityListener) {
		visListeners.add(markerVisibilityListener);		
	}
	
	/**
	 * modify by ccwu
	 * 
	 * @param ip remote server's ip address
	 */
	public void setIPAddress(String ip){
		if(ip != null){
			this.ipname_instream = ip;
		}else{
			Log.e("ARToolKit", "IP address error");
		}
	}
	
	/**
	 * modify by ccwu
	 * 
	 * @param number remote server's port number 
	 */
	public void setPoreNumber(int number){
		if(number > 0){
			this.portnumber = number;
		}else{
			Log.e("ARToolKit", "port number error");
		}
	}

	class DetectMarkerWorker extends Thread {
		private byte[] curFrame;
		private boolean newFrame = false;
		private int lastNumMarkers=0;
		
		//modify by ccwu
		private final CameraStatus cameraState;
		private Random rndSender;  //decide send or not :reduce heap growth
		private boolean sentOut;
		private boolean noMarker;
		
		/**
		 * @param camStatus
		 */
		public DetectMarkerWorker(CameraStatus camStatus) {
			this.cameraState = camStatus;
			this.rndSender = new Random(66);
			this.sentOut = false;
			this.noMarker = false;
			setPriority(MIN_PRIORITY);
			setDaemon(true);
			start();
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public synchronized void run() {			
			setName("DetectMarkerWorker");
			while(true) {
				while(!newFrame) {
					//spurious wakeups
					try {
						wait();//wait for next frame
					} catch (InterruptedException e) {}
				}
				newFrame = false;
				//the monitor is locked inside the method
				int currNumMakers = artoolkit_detectmarkers(curFrame, transMatMonitor);
				if(lastNumMarkers > 0 && currNumMakers > 0) {
					//visible
					noMarker = false;
					Log.i("DetectMarkerWorker","the marker is visible");
				} else if(lastNumMarkers == 0 && currNumMakers > 0) {
					//detected a marker
					noMarker = false;
					notifyChange(true);
					Log.i("DetectMarkerWorker","detected a marker");
				} else if(lastNumMarkers > 0 && currNumMakers == 0) {
					//lost the marker
					notifyChange(false);
					Log.i("DetectMarkerWorker","lost the marker");
				}else{
					//no marker in the frame, do nothing
					noMarker = true;
				}
				lastNumMarkers = currNumMakers;
				
				if(remoteQuery == true && noMarker == false){
					//no pattern file in local
					//modify by ccwu
					int choose = rndSender.nextInt(100);
					if(choose > 80){ sentOut = true; }
				}
				if(sentOut == true){
					remoteQuery = false;
					noMarker = true;
					sentOut = false;
					//no pattern file in local or no marker in the frame
					//modify by ccwu
					//A temp stream buffer
					ByteArrayOutputStream outstr = new ByteArrayOutputStream();
					Log.i("DetectMarkerWorker","frame size="+curFrame.length);
					//Log.i("DetectMarkerWorker","width="+this.cameraState.width);
					//Log.i("DetectMarkerWorker","height="+this.cameraState.height);
					Bitmap bitmap = getYUVtoARGBBitmap(curFrame, this.cameraState.width, this.cameraState.height);
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, outstr);
	                        
					Thread tcpth = new TCPClientThread(outstr, ipname_instream, portnumber);
					tcpth.start();

					try {
						outstr.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					//use for debug
					/**
					FileOutputStream out = null;
					try {
						Random generator = new Random();
				    	int n = 10000;
				    	n = generator.nextInt(n);
				    	String fname = "Image-"+ n +".png";
						out = new FileOutputStream("/storage/sdcard0/"+fname);
						BufferedOutputStream bos = new BufferedOutputStream(out);
						bitmap.compress(Bitmap.CompressFormat.PNG, 100,  bos);
						bos.flush();
						bos.close();
						out.close();
					} catch (FileNotFoundException e1) {
						// TODO Auto-generated catch block
						Log.e("onPreviewFrame","FileNotFoundException");
						e1.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						Log.e("onPreviewFrame","IOException");
						e.printStackTrace();
					}
					 **/
				}
			}
		}
		
		private void notifyChange(boolean visible) {
			for (final MarkerVisibilityListener visListener : visListeners) {
				visListener.makerVisibilityChanged(visible);
			}
		}
		
		final void nextFrame(byte[] frame) {
			if(this.getState() == Thread.State.WAITING) {
				//ok, we are ready for a new frame:
				curFrame = frame;
				newFrame = true;
				//do the work:
				synchronized (this) {
					this.notify();
				}	
			} else {
				//ignore it
			}
		}
		
		private int[] decodeYUV420SP(byte[] yuv420sp, int width, int height) throws NullPointerException, IllegalArgumentException {
			final int frameSize = width * height;
	        if (yuv420sp == null) {
	        	throw new NullPointerException("buffer yuv420sp is null");
	        }
	        Log.d("--width---height---yuv420sp.length", width+"---"+height+"---"+yuv420sp.length);
	        
	        if (yuv420sp.length < frameSize) {
	            throw new IllegalArgumentException("buffer yuv420sp is illegal");
	        }

	        int[] rgb = new int[frameSize];

	        for (int j = 0, yp = 0; j < height; j++) {
	            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
	            for (int i = 0; i < width; i++, yp++) {
	                int y = (0xff & ((int) yuv420sp[yp])) - 16;
	                if (y < 0) y = 0;
	                if ((i & 1) == 0) {
	                    v = (0xff & yuv420sp[uvp++]) - 128;
	                    u = (0xff & yuv420sp[uvp++]) - 128;
	                }
	                int y1192 = 1192 * y;
	                int r = (y1192 + 1634 * v);
	                int g = (y1192 - 833 * v - 400 * u);
	                int b = (y1192 + 2066 * u);
	                if (r < 0) r = 0; else if (r > 262143) r = 262143;
	                if (g < 0) g = 0; else if (g > 262143) g = 262143;
	                if (b < 0) b = 0; else if (b > 262143) b = 262143;

	                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
	             }
	        }

	        return rgb;
	     }
		
		public final Bitmap getYUVtoARGBBitmap(byte[] yuvframe, int width, int height){
	         final int decodeWidth = 320;
	         final int decodeHeight = 240;
	         int[] data = decodeYUV420SP(yuvframe,width,height);
			 Bitmap bitmap = Bitmap.createBitmap(
					 data,
	                 width,
	                 height,
	                 Bitmap.Config.ARGB_8888
	         );
			 if (width > decodeWidth || height > decodeHeight) {
		          bitmap = scaleBitmap(bitmap, decodeWidth, decodeHeight);
		         }
			return bitmap;	
		}
		
		private Bitmap scaleBitmap(Bitmap bitmap, int newWidth, int newHeight) {
	        int width = bitmap.getWidth();
	        int height = bitmap.getHeight();

	        float scaleWidth = ((float) newWidth) / width;
	        float scaleHeight = ((float) newHeight) / height;
	        if (scaleWidth >= 1 && scaleHeight >= 1) {
	            return bitmap;
	        }

	        Matrix matrix = new Matrix();
	        float scaleop = scaleWidth > scaleHeight ? scaleHeight : scaleWidth;

	        matrix.postScale(scaleop, scaleop);

	        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
	    }
		
		private int[] convertByteToColorInt(byte[] rgbDataframe){
			//int red, green, blue;
			int size = rgbDataframe.length;
			//check size
			if (size == 0){
				return null;
			}
			//check size is 3X 
			int arg = 0;
			if (size % 3 != 0){
				arg = 1;
			}
			int[] color = new int[size / 3 + arg];
			if (arg == 0){
				for(int i = 0; i < color.length; ++i){
					/*
					red = convertByteToInt(rgbDataframe[i * 3]);
				    green = convertByteToInt(rgbDataframe[i * 3 + 1]);
				    blue = convertByteToInt(rgbDataframe[i * 3 + 2]); 
				    color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
				    */
					color[i] = (rgbDataframe[i * 3] << 16 & 0x00FF0000) |   
	                           (rgbDataframe[i * 3 + 1] << 8 & 0x0000FF00 ) |   
	                           (rgbDataframe[i * 3 + 2] & 0x000000FF ) | 0xFF000000;  
				}
			}else{
				for(int i = 0; i < color.length - 1; ++i){
					/*
					red = convertByteToInt(rgbDataframe[i * 3]);
					green = convertByteToInt(rgbDataframe[i * 3 + 1]);
					blue = convertByteToInt(rgbDataframe[i * 3 + 2]); 
					color[i] = (red << 16) | (green << 8) | blue | 0xFF000000;
					*/
					color[i] = (rgbDataframe[i * 3] << 16 & 0x00FF0000) |   
	                           (rgbDataframe[i * 3 + 1] << 8 & 0x0000FF00 ) |   
	                           (rgbDataframe[i * 3 + 2] & 0x000000FF ) | 0xFF000000;  
				}
				color[color.length - 1] = 0xFF000000;
			}
			return color;
		}
		
		/*
		private int convertByteToInt(byte data){
			  
			 int heightBit = (int) ((data>>4) & 0x0F);
			 int lowBit = (int) (0x0F & data);
			 return heightBit * 16 + lowBit;
		}*/
	}
	
	

}
