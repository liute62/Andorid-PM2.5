package com.example.pm;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginException;

import app.bluetooth.DeviceControlActivity;
import app.utils.Const;

/**
 * Created by Administrator on 12/7/2015.
 */
public class BluetoothFragment extends Fragment implements View.OnClickListener,AdapterView.OnItemClickListener{

    Activity mActivity;
    ImageView mProfile;
    ImageView mBack;
    ImageView mScan;


    ImageView mRefresh;
    ImageView mStop;
    ListView mDevicesList;
    ProgressBar mProgress;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private static final int REQUEST_ENABLE_BT = 1;
    // stop searching after ten seconds
    private static final long SCAN_PERIOD = 10000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mHandler = new Handler();

        // check if current devices support bluetooth
        if (!mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mActivity.getApplicationContext(), Const.Info_Bluetooth_ptc_Not_Support, Toast.LENGTH_SHORT).show();
        }
        // Initial Bluetooth adapter, api > 4.3
        final BluetoothManager bluetoothManager = (BluetoothManager) mActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // check  if current devices support bluetooth
        if (mBluetoothAdapter == null) {
            Log.e("Blutooth Fragment","mBluetoothAdapter == null");
            Toast.makeText(mActivity.getApplicationContext(),Const.Info_Bluetooth_Not_Support, Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bluetooth, null);
        mProfile = (ImageView) view.findViewById(R.id.bluetooth_profile);
        mBack = (ImageView)view.findViewById(R.id.bluetooth_back);
        mScan = (ImageView) view.findViewById(R.id.bluetooth_scan);
        mRefresh = (ImageView) view.findViewById(R.id.bluetooth_refresh);
        mStop = (ImageView)view.findViewById(R.id.bluetooth_stop);
        mDevicesList = (ListView)view.findViewById(R.id.bluetooth_device_list);
        mProgress = (ProgressBar)view.findViewById(R.id.bluetooth_progressbar);
        mProgress.setVisibility(View.GONE);
        mProfile.setOnClickListener(this);
        mBack.setOnClickListener(this);
        mScan.setOnClickListener(this);
        mRefresh.setOnClickListener(this);
        mStop.setOnClickListener(this);
        mDevicesList.setOnItemClickListener(this);
        mLeDeviceListAdapter = new LeDeviceListAdapter(mActivity);
        mDevicesList.setAdapter(mLeDeviceListAdapter);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bluetooth_profile:
                mProfile.setSelected(true);
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.toggle();
                break;
            case R.id.bluetooth_back:
                Fragment mainFragment = new MainFragment();
                mActivity.getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.content, mainFragment)
                        .commit();
                break;
            case R.id.bluetooth_scan:
                mProgress.setVisibility(View.VISIBLE);
                if(mLeDeviceListAdapter == null){
                    Toast.makeText(mActivity.getApplicationContext(),Const.Info_Bluetooth_Not_Support, Toast.LENGTH_SHORT).show();
                }else {
                    mLeDeviceListAdapter.clear();
                }
               scanLeDevice(true);
                break;
            case R.id.bluetooth_refresh:
                break;
            case R.id.bluetooth_stop:
                scanLeDevice(false);
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // To make sure bluetooth could work. If not,  ask user for permission
        mProgress.setVisibility(View.GONE);
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    //    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main, menu);
//        if (!mScanning) {
//            menu.findItem(R.id.menu_stop).setVisible(false);
//            menu.findItem(R.id.menu_scan).setVisible(true);
//            menu.findItem(R.id.menu_refresh).setActionView(null);
//        } else {
//            menu.findItem(R.id.menu_stop).setVisible(true);
//            menu.findItem(R.id.menu_scan).setVisible(false);
//            menu.findItem(R.id.menu_refresh).setActionView(
//                    R.layout.actionbar_indeterminate_progress);
//        }
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menu_scan:
//                mLeDeviceListAdapter.clear();
//                scanLeDevice(true);
//                break;
//            case R.id.menu_stop:
//                scanLeDevice(false);
//                break;
//        }
//        return true;
//    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        // 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
//        if (!mBluetoothAdapter.isEnabled()) {
//            if (!mBluetoothAdapter.isEnabled()) {
//                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//            }
//        }
//
//        // Initializes list view adapter.
//        mLeDeviceListAdapter = new LeDeviceListAdapter();
//        setListAdapter(mLeDeviceListAdapter);
//        scanLeDevice(true);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        // User chose not to enable Bluetooth.
//        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
//            finish();
//            return;
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        scanLeDevice(false);
//        mLeDeviceListAdapter.clear();
//    }

//    @Override
//    protected void onListItemClick(ListView l, View v, int position, long id) {
//        System.out.println("==position=="+position);
//        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
//        if (device == null) return;
//        final Intent intent = new Intent(mActivity, DeviceControlActivity.class);
//        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
//        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
//        if (mScanning) {
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//            mScanning = false;
//        }
//        startActivity(intent);
//    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mProgress.setVisibility(View.GONE);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(mActivity, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter(Activity mActivity) {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = mActivity.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    mProgress.setVisibility(View.GONE);
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
