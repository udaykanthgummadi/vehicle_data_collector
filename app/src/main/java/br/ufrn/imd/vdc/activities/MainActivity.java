package br.ufrn.imd.vdc.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import br.ufrn.imd.vdc.R;
import br.ufrn.imd.vdc.helpers.BluetoothManager;
import br.ufrn.imd.vdc.helpers.ObdServiceManager;
import br.ufrn.imd.vdc.obd.CommandTask;
import br.ufrn.imd.vdc.obd.ObdCommandList;
import br.ufrn.imd.vdc.obd.ObdCommandTask;
import br.ufrn.imd.vdc.services.ObdGatewayService;

public class MainActivity extends TaskProgressListener implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getName();

    // final ObdServiceManager serviceManager = new ObdServiceManager(this);
    Button btnStartService;
    Button btnStopService;
    Button btnEnqueueCommands;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setup();
    }

    private void setup() {
        btnStartService = (Button) findViewById(R.id.btn_start_service);
        btnStopService = (Button) findViewById(R.id.btn_stop_service);
        btnEnqueueCommands = (Button) findViewById(R.id.btn_enqueue_commands);

        btnStartService.setOnClickListener(this);
        btnEnqueueCommands.setOnClickListener(this);
        btnStopService.setOnClickListener(this);

        // Clear log button
        Button btnClearLog = (Button) findViewById(R.id.btn_clear_log);
        btnClearLog.setOnClickListener(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        updateState(ObdServiceManager.Status.DISCONNECTED);

        // Start/Stop bluetooth
        Button btnStartBluetooth = (Button) findViewById(R.id.btn_connect_bluetooth);
        Button btnStopBluetooth = (Button) findViewById(R.id.btn_disconnect_bluetooth);
        btnStartBluetooth.setOnClickListener(this);
        btnStopBluetooth.setOnClickListener(this);
    }

    private void setActionBarSubtitle(String subtitle) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(subtitle);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            this.startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connect_bluetooth:
                Log.d(TAG, "onClick: Creating bluetooth connection");

                // Bluetooth
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                String btDeviceMAC = prefs.getString(SettingsActivity.BLUETOOTH_DEVICES, "-1");

                if (!btDeviceMAC.equals("-1") &&
                    BluetoothManager.getInstance().setUpDevice(btDeviceMAC)) {
                    setActionBarSubtitle(
                        BluetoothManager.getInstance().getDevice().getName() + " - " + btDeviceMAC);
                }
                break;
            case R.id.btn_disconnect_bluetooth:
                Log.d(TAG, "onClick: disconnecting bluetooth");
                setActionBarSubtitle(getString(R.string.disconnecting));
                BluetoothManager.getInstance().disconnect();
                if (!BluetoothManager.getInstance().isConnected()) {
                    setActionBarSubtitle(getString(R.string.disconnected));
                }
                break;
            case R.id.btn_start_service:
                Log.d(TAG, "onClick: Calling doBindService()");

                if (!BluetoothManager.getInstance().isDeviceBonded()) {
                    Log.d(TAG, "onClick: Device isn't bonded");
                    return;
                }
                ObdGatewayService.enqueueTask(this, new ObdCommandTask(ObdCommandList.getInstance()
                    .setupDevice()));
                ObdGatewayService.enqueueTask(this, new ObdCommandTask(ObdCommandList.getInstance()
                    .vehicleInformation()));
                break;
            case R.id.btn_stop_service:
                Log.d(TAG, "onClick: Calling doUnbindService()");
                // serviceManager.doUnbindService();
                break;
            case R.id.btn_enqueue_commands:
                Log.d(TAG, "onClick: Enqueuing commands");
                if (BluetoothManager.getInstance().isDeviceBonded()) {
                    return;
                }
                ObdGatewayService.enqueueTask(this, new ObdCommandTask(ObdCommandList.getInstance()
                    .dynamicData()));
                break;
            case R.id.btn_clear_log:
                Log.d(TAG, "onClick: Clearing OBD Commands Log");
                clearObdLog();
                break;
            default:
                break;
        }
    }

    private void clearObdLog() {
        TextView tvResultsLog = (TextView) findViewById(R.id.tv_results);
        tvResultsLog.setText("");
    }

    @Override
    public void updateState(CommandTask task) {
        TextView tvResultsLog = (TextView) findViewById(R.id.tv_results);
        tvResultsLog.append(task.getCommand().toString());
    }

    @Override
    public void updateState(ObdServiceManager.Status status) {
        switch (status) {
            case CONNECTED:
                btnStartService.setEnabled(false);
                btnStopService.setEnabled(true);
                btnEnqueueCommands.setEnabled(true);
                break;
            case DISCONNECTED:
                setActionBarSubtitle(getString(R.string.disconnected));
                btnStartService.setEnabled(true);
                btnStopService.setEnabled(false);
                btnEnqueueCommands.setEnabled(false);
                break;
            case CONNECTING:
                setActionBarSubtitle(getString(R.string.connecting));
                btnStartService.setEnabled(false);
                btnStopService.setEnabled(false);
                btnEnqueueCommands.setEnabled(false);
                break;
            case DISCONNECTING:
                setActionBarSubtitle(getString(R.string.disconnecting));
                btnStartService.setEnabled(false);
                btnStopService.setEnabled(false);
                btnEnqueueCommands.setEnabled(false);
                break;
            default:
                Log.d(TAG, "updateState: Unhandled option");
                break;
        }
    }
}
