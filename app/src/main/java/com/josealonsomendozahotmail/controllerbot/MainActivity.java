package com.josealonsomendozahotmail.controllerbot;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public TextView textView;
    public Button startButton, sendButton, clearButton, stopButton;
    public EditText editText;
    public Socket socket;
    public UsbManager usbManager;
    public UsbDevice device;
    public UsbDeviceConnection connection;
    public String dataToSend;
    public final String ACTION_USB_PERMISSION = "com.josealonsomendozahotmail.controllerbot";
    public UsbSerialDevice serialPort;
    public Boolean isDeviceAttached;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);

        textView = (TextView)findViewById(R.id.text_view);
        startButton = (Button)findViewById(R.id.begin);
        sendButton = (Button)findViewById(R.id.send);
        clearButton = (Button)findViewById(R.id.clear);
        stopButton = (Button)findViewById(R.id.Stop);
        editText = (EditText)findViewById(R.id.editText);

        setUiEnabled(false);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        Log.d(TAG, "Added text to textView");
    }

    @Override
    public void onResume() {
        super.onResume();
        textView.setText("");

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String socketHost = settings.getString("socketServer", "");
        String videoHost = settings.getString("videoServer", "");
        socketClient(socketHost);

        Log.d(TAG, "socketHost " + socketHost);
        Log.d(TAG, "Resumed");

    }

    // Specifies options menu for main activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        Log.d(TAG, "Options menu created");

        return true;
    }

    // When menu item is clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent intent = new Intent(this, SetPreferenceActivity.class);
                this.startActivity(intent);

                Log.d(TAG, "Clicked settings");
                Log.d(TAG, "Initializing Settings activity");

                break;
        }
        return true;
    }

    public void onClickStart(View view) {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new
                            Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        serialPort.close();
        tvAppend(textView, "\nSerial Connection Closed! \n");
    }

    public void onClickClear(View view) {
        textView.setText(" ");
    }

    public void onClickSend(View view) {
        String string = editText.getText().toString();
        tvAppend(textView, "\nData Sent : " + string + "\n");
        serialPort.write(string.getBytes());
        textView.setText(string);
    }

    // Callback which triggers whenever data is read
    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            String data;
            try {
                data = new String(arg0, "UTF-8");
                if (!data.equals("")) {
                    if (dataToSend == null) {
                        dataToSend = data;
                    }else {
                        dataToSend = dataToSend + data;
                    }
                    if (dataToSend.contains("\n")) {
                        socket.emit("info", dataToSend);
                        dataToSend = "";
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
    };

    //Broadcast Receiver to automatically start and stop the Serial connection.
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            setUiEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(textView, "Serial Connection Opened!\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);
            }
        }
    };

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);

    }

    // Appends text to textview
    public void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                    ftv.append(ftext + "\n");
            }
        });

    }

    // This method creates connection to socketio server in order to receive direction of bot
    void socketClient(String url){
        try {
            socket = IO.socket(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // Connection event
        // When the app connects to the server
        Emitter on = socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d(TAG, "socket connected");
                tvAppend(textView, "connected");
            }
        }).on("move", new Emitter.Listener() {
            @Override
            public void call(Object[] args) {
                String direction = Arrays.toString(args);
                direction = direction.replace("[", "");
                direction = direction.replace("]", "");
                HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
                if(!usbDevices.isEmpty()){
                    serialPort.write(direction.getBytes());
                }
//                tvAppend(textView, direction);
                Log.d(TAG, direction);
            }

        }).on("info", new Emitter.Listener() {
            @Override
            public void call(Object[] args) {
//                String msg = Arrays.toString(args);
//                serialPort.write(msg.getBytes());
//                tvAppend(textView, msg);
            }

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
            }
        });

        // Here the socket is called, it is initialized
        socket.connect();
    }

}

