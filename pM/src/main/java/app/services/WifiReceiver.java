package app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Created by liuhaodong1 on 16/2/3.
 * Get the wifi state for later usage
 */

public class WifiReceiver extends BroadcastReceiver {

    public static final String TAG = "WifiReceiver";
       boolean isConnected;

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION)){
                //signal strength changed
                Log.e(TAG,"signal strength changed");
            }
            else if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if(info.getState().equals(NetworkInfo.State.DISCONNECTED)){
                    Log.e(TAG,"DISCONNECTED");
                }
                else if(info.getState().equals(NetworkInfo.State.CONNECTED)){

                    WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    Log.e(TAG,"wifi name = "+wifiInfo.getSSID());
                }

            }
            else if(intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
                int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);

                if(wifistate == WifiManager.WIFI_STATE_DISABLED){
                    Log.e(TAG,"WIFI_STATE_DISABLED");
                }
                else if(wifistate == WifiManager.WIFI_STATE_ENABLED) {
                    Log.e(TAG, "WIFI_STATE_ENABLED");
                }
            }
        }
}
