package br.ufrn.imd.vdc.services;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import br.ufrn.imd.vdc.helpers.BluetoothManager;
import br.ufrn.imd.vdc.obd.CommandTask;
import br.ufrn.imd.vdc.obd.ObdCommandList;
import br.ufrn.imd.vdc.obd.ObdCommandTask;

public class ObdGatewayServiceManager {
    private static final String TAG = ObdGatewayServiceManager.class.getName();
    private static final ObdGatewayServiceManager ourInstance = new ObdGatewayServiceManager();

    private BluetoothManager btManager = BluetoothManager.getInstance();

    private ObdGatewayServiceManager() {
    }

    public static ObdGatewayServiceManager getInstance() {
        return ourInstance;
    }

    public void enqueueTask(Context context, CommandTask task) {
        if (!btManager.isDeviceBonded()) {
            Log.e(TAG, "enqueueTask: Bluetooth Device isn't bonded, can't enqueue task");
            return;
        }
        Log.d(TAG, "enqueueTask: Enqueuing task...");
        Intent intent = new Intent(context, ObdGatewayService.class);
        intent.setAction(ObdGatewayService.ACTION_SEND_OBD_COMMAND);
        ObdGatewayService.enqueueTask(task);
        context.startService(intent);
    }

    public void enqueueInitialCommands(Context context) {
        enqueueTask(context, new ObdCommandTask(ObdCommandList.getInstance().setupDevice()));
        enqueueTask(context, new ObdCommandTask(ObdCommandList.getInstance().vehicleInformation()));
    }

    public void enqueueDefaultCommands(Context context) {
        enqueueTask(context, new ObdCommandTask(ObdCommandList.getInstance().dynamicData()));
    }

    public boolean setUpDevice(String macAddress) {
        return btManager.setUpDevice(macAddress);
    }

    public void disconnect() {
        btManager.disconnect();
    }

    public boolean isConnected() {
        return btManager.isConnected();
    }

    public String getDeviceString() {
        if (btManager.getDevice() != null) {
            return btManager.getDevice().getName() + " - " + btManager.getDevice().getAddress();
        }
        return "";
    }

    public enum State {
        CONNECTED,
        DISCONNECTED,
        CONNECTING,
        DISCONNECTING
    }
}
