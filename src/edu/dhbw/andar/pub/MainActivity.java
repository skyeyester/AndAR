package edu.dhbw.andar.pub;

import edu.dhbw.andopenglcam.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

public class MainActivity extends Activity{
	
	EditText ipfield;
	EditText portfield;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ipfield = (EditText)findViewById(R.id.editText_IP);
        portfield = (EditText)findViewById(R.id.editText_port);
    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
	
    public boolean onOptionsItemSelected(MenuItem item) {
    	int item_id = item.getItemId();
    	
    	switch (item_id){
    		case R.id.menu_ok:
    			// Send a message using content of the edit text widget
                String ipaddr = ipfield.getText().toString();
                String port = portfield.getText().toString();
                // Check that there's actually something to send
                if (ipaddr.length() > 0 && port.length() > 0) {
                // Get the message bytes and tell the MainActivity to config
               	Intent intent = new Intent();
               	intent.setClass(MainActivity.this, CustomActivity.class);
               	Bundle bundle = new Bundle();
               	bundle.putString("ipaddr",ipaddr);
               	bundle.putString("port",port);
               	intent.putExtras(bundle);
               	startActivity(intent);
               }
               break;
    		case R.id.menu_reset:
    			ipfield.setText("");
    			portfield.setText("");
    			break;	
    		default: return false;
    	}
    	return true;
    }    

}
