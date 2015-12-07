package app.bluetooth;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.Window;

import com.example.pm.R;

/**
 * Created by Haodong Liu on 11/20/2015.
 */

public class BluetoothActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_bluetooth);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
