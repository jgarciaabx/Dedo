package com.apiworking.dedo.ZKUSBManager;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import androidx.annotation.NonNull;

import java.util.Random;

/**
 * usb permission and hotplug
 */
public class ZKUSBManager {
    //usb's vendor id for zkteco
    private int vid = 0x1b55;
    //usb's product id
    private int pid = 0;
    //application context
    private Context mContext = null;

    /////////////////////////////////////////////
    //for usb permission
    private static final String SOURCE_STRING = "0123456789-_abcdefghigklmnopqrstuvwxyzABCDEFGHIGKLMNOPQRSTUVWXYZ";
    private static final int DEFAULT_LENGTH = 16;
    private String ACTION_USB_PERMISSION;
    private boolean mbRegisterFilter = false;
    private ZKUSBManagerListener zknirusbManagerListener = null;

    private BroadcastReceiver usbMgrReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE); // Obtenemos el dispositivo USB.

            if (device == null) { // Verificamos si el dispositivo es nulo antes de usarlo.
                zknirusbManagerListener.onCheckPermission(-1); // Reportamos un error si el dispositivo es nulo.
                return;
            }

            if (ACTION_USB_PERMISSION.equals(action)) {
                if (device.getVendorId() == vid && device.getProductId() == pid) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        zknirusbManagerListener.onCheckPermission(0);
                    } else {
                        zknirusbManagerListener.onCheckPermission(-2);
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                if (device.getVendorId() == vid && device.getProductId() == pid) {
                    zknirusbManagerListener.onUSBArrived(device);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if (device.getVendorId() == vid && device.getProductId() == pid) {
                    zknirusbManagerListener.onUSBRemoved(device);
                }
            }
        }

    };


    private boolean isNullOrEmpty(String target) {
        if (null == target || "".equals(target) || target.isEmpty()) {
            return true;
        }
        return false;
    }

    private String createRandomString(String source, int length) {
        if (this.isNullOrEmpty(source)) {
            return "";
        }

        StringBuffer result = new StringBuffer();
        Random random = new Random();

        for(int index = 0; index < length; index++) {
            result.append(source.charAt(random.nextInt(source.length())));
        }
        return result.toString();
    }

    public boolean registerUSBPermissionReceiver() {
        if (mContext == null || mbRegisterFilter) {
            return false; // Evita registrar múltiples veces
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mContext.registerReceiver(usbMgrReceiver, filter);
        mbRegisterFilter = true;
        return true;
    }


    public void unRegisterUSBPermissionReceiver()
    {
        if (null == mContext || !mbRegisterFilter)
        {
            return;
        }
        mContext.unregisterReceiver(usbMgrReceiver);
        mbRegisterFilter = false;
    }


    //End USB Permission
    /////////////////////////////////////////////

    public ZKUSBManager(@NonNull Context context, @NonNull ZKUSBManagerListener listener)
    {
        super();
        if (null == context || null == listener)
        {
            throw new NullPointerException("context or listener is null");
        }
        zknirusbManagerListener = listener;
        ACTION_USB_PERMISSION = createRandomString(SOURCE_STRING, DEFAULT_LENGTH);
        mContext = context;
    }

    //0 means success
    //-1 means device no found
    //-2 means device no permission
    public void initUSBPermission(int vid, int pid) {
        UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        UsbDevice usbDevice = null;

        // Iterar sobre la lista de dispositivos conectados
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            int device_vid = device.getVendorId();
            int device_pid = device.getProductId();
            if (device_vid == vid && device_pid == pid) {
                usbDevice = device;
                break;
            }
        }

        // Verificar si el dispositivo fue encontrado
        if (usbDevice == null) {
            zknirusbManagerListener.onCheckPermission(-1); // Dispositivo no encontrado
            return;
        }

        this.vid = vid;
        this.pid = pid;

        // Verificar permisos para el dispositivo USB
        if (!usbManager.hasPermission(usbDevice)) {
            Intent intent = new Intent(this.ACTION_USB_PERMISSION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    mContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE // Asegurarse de incluir este flag en Android 12+
            );
            usbManager.requestPermission(usbDevice, pendingIntent);
        } else {
            zknirusbManagerListener.onCheckPermission(0); // Permiso ya otorgado
        }
    }


}
