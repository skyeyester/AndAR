package edu.dhbw.andar.pub;

import edu.dhbw.andar.exceptions.AndARException;
import edu.dhbw.andopenglcam.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class DataMessageHandler extends Handler{ 
	
	private static final String LOG_TAG = "DataMessageHandler";
	private boolean hadadd = false;
	private DrawOnTop mDraw;
	private String result;
	private CustomActivity marActivity;
	private final int MEG_Thread=1;
	private final int MEG_Thread2=2;
	//private String path = "/storage/sdcard0/Android/data/artoolkit"; //ASUS
	private String path = "/mnt/sdcard/Android/data/artoolkit";   //SONY
	
	/**
	 * 
	 * constructor method
	 * */
	public DataMessageHandler(CustomActivity arActivity){
		this.result = "Waiting...";
		this.marActivity = arActivity;
	}
	
	
    public void handleMessage(Message msg){
       	super.handleMessage(msg);	
    	if(hadadd == false){
    		//need to modify
    		//mDraw = new DrawOnTop(marActivity, result);
    		//marActivity.addContentView(mDraw, new LayoutParams (LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    		hadadd=true;
    		//Log.i(LOG_TAG ,"AddContentView"+result);
    	}

        switch (msg.what){
        	//depend on message
	        case MEG_Thread:
	            //Display
	        	result=msg.getData().getString("regmsg");
	        	Log.i(LOG_TAG ,"Recognize Result: "+result);
	           	//mDraw.reDraw(result);
	           	View toastRoot = marActivity.getLayoutInflater().inflate(R.layout.toast_layout, null);
	           	Toast toast=new Toast(marActivity.getApplicationContext());
	           	toast.setView(toastRoot);
	           	TextView tv=(TextView)toastRoot.findViewById(R.id.textformat);
	           	tv.setText(result);
	           	toast.setDuration(Toast.LENGTH_SHORT);
	           	toast.show();
	           	//Toast.makeText(marActivity, result, Toast.LENGTH_SHORT).show();
	           	result=" ";
	            break;
	        case MEG_Thread2:
	        	//register new pattern file downloaded from server in SD card 
	        	String filename = msg.getData().getString("pattName");
	        	Log.i(LOG_TAG , "Get File:"+filename);
	        	//yellow color
	        	Log.i(LOG_TAG , "Before Regster "+filename);
	        	CustomObject arobject = new CustomObject
				(filename, filename, 80.0, new double[]{0,0}, new float[]{1,1,0});
	        	try {
	        		marActivity.getArtoolkit().registerARObject(arobject, path);
		        	Log.i(LOG_TAG , "After Regster "+filename);
	        	} catch (AndARException e) {
	        		// TODO Auto-generated catch block
	        		e.printStackTrace();
	        		Log.i(LOG_TAG , "Regster "+filename+" failed");
	        	}
	        	break;
        }
   }
}


class DrawOnTop extends View { 

	private String displaytext;
	private Paint paint;  
    public DrawOnTop(Context context, String text) { 
            super(context); 
            // TODO Auto-generated constructor stub
            this.displaytext=text;
            this.paint = new Paint();
    } 

    @Override
    protected void onDraw(Canvas canvas) { 
            // TODO Auto-generated method stub 
        	super.onDraw(canvas); 
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(255, 250, 250, 210));
            
            //Set Size
            float dips = 64.0f;
            // Convert the dips to pixels
            final float scale = getResources().getDisplayMetrics().density;
            int ps = (int) (dips * scale + 0.5f);
            paint.setTextSize(ps);
            canvas.drawText(displaytext, 350, 600, paint);
    } 
    
    protected void reDraw(String text) { 
    	this.displaytext=text;
        //re-draw execute onDraw process again
    	this.invalidate();
    } 

} 