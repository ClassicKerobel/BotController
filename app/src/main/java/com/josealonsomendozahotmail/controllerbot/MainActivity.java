package com.josealonsomendozahotmail.controllerbot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import java.net.URISyntaxException;
import java.util.Arrays;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    TextView textView;
    public Socket socket;
    public boolean isReturned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String serverHost = settings.getString("socketServer", "");


        textView = (TextView)findViewById(R.id.text_view);
        textView.append(serverHost + "\n");

        socketClient();

        Log.d(TAG, "Added text to textView");

    }

    @Override
    public void onResume() {
        super.onResume();

        if(isReturned){
            recreate();
            isReturned = false;
        }


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

                isReturned = true;

                break;
        }
        return true;
    }

    // Appends text to textview
    public void tvAppend(TextView tv, final CharSequence text) {
        final TextView ftv = tv;
//        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(text + "\n");
            }
        });

    }


    // This method creates connection to socketio server in order to receive direction of bot
    void socketClient(){
        try {
            socket = IO.socket("https://classickerobel.ddns.net:8094/");
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
//                direction = direction.replace("[", "");
//                direction = direction.replace("]", "");
//                serialPort.write(direction.getBytes());
                tvAppend(textView, direction);
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

