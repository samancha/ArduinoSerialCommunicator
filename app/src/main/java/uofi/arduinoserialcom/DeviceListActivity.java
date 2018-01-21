package uofi.arduinoserialcom;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DeviceListActivity extends Activity {

    static final String TAG = "IOWA";

    private static UsbSerialPort sPort = null;
    private TextView mDumpTextView;
    private TextView mTitleTextView;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private boolean dataReceived = false;
    private boolean dataInProgress = false;
    private String finalString = "";

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    DeviceListActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DeviceListActivity.this.updateReceivedData(data);
                        }
                    });
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDumpTextView = (TextView) findViewById(R.id.serialDataText);
        mTitleTextView = (TextView) findViewById(R.id.titleTextView);
        String hex = "213c5468697320697320736f6d65206172626974726172792073746174656d656e743e21";
        String temp = hexToString("213c5468697320697320736f6d65206172626974726172792073746174656d656e743e21");
        Log.d(TAG,"**********: " + temp);
        System.out.println(hex);
        System.out.println(hex.toUpperCase());
        System.out.println("START");
        System.out.println(hex.endsWith("3e21"));
        System.out.println(hex.substring(0, 4).equals("213c"));
        if(hex.substring(0,4).equals("213c") && hex.endsWith("3e21") ){
            System.out.println("%%%%%%% CONDITION TRUE");
        }
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%% ON CREATE");
    }

    public void startScan(View view){
        Toast.makeText(this, "button pressed", Toast.LENGTH_SHORT).show();
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }
    }

    protected void onPause(){
        super.onPause();
        stopIoManager();

        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%% ON PAUSE");
        finish();
    }

    protected void onResume(){
        super.onResume();

        final UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (!availableDrivers.isEmpty()) {

            // Open a connection to the first available driver.
            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            if (connection == null) {
                // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
                mTitleTextView.setText("Connection is NULL");
                Toast.makeText(this,"there is no connection" , Toast.LENGTH_LONG).show();
                return;
            }

            UsbSerialPort port = driver.getPorts().get(0);
            sPort = port;
            try {

                sPort.open(connection);
                Log.v(TAG, "Port Opened");
                sPort.setParameters(115200,UsbSerialPort.DATABITS_8,UsbSerialPort.STOPBITS_1,UsbSerialPort.PARITY_NONE);
            }catch (IOException e){
                mTitleTextView.setText("Error opening device" + e.getMessage());
                try{
                    sPort.close();
                }catch (IOException e2){
                    e2.printStackTrace();
                }
                sPort = null;
            }
            mTitleTextView.setText("Device: " + sPort.getClass().getSimpleName());

        }else{
            Toast.makeText(this,"driver is null" ,Toast.LENGTH_SHORT).show();
            mTitleTextView.setText("Driver is NULL");
        }

        System.out.println("%%%%%%%%%%%%%%%%%%%%%%% ON RESUME");

        onDeviceStateChange();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
            System.out.println("################### STOPIO");

        }
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%% STOPIO");

    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
            System.out.println("################## STARTIO");

        }
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%% STARTIO");

    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
        System.out.println("DEVICECHANGE");

    }

    private void updateReceivedData(byte[] data) {

        String hexString =  HexDump.toHexString(data);

        try{
            if(hexString.substring(0,4).equals("213C") && !dataReceived){
                dataInProgress = true;
                finalString = hexString;
            }else if(dataInProgress && !hexString.contains("3E21")){
                finalString+= hexString;
            }else if(dataInProgress && hexString.contains("3E21")){
                dataReceived = true;
                finalString += hexString;


                stopIoManager();
                if (sPort != null) {
                    try {
                        sPort.close();
                    } catch (IOException e) {
                        // Ignore.
                    }
                    sPort = null;
                }
                mTitleTextView.setText("Match");

                finalString = finalString.replace("5C","");
                //String dataString = hexToString(hexString.toLowerCase());
                finalString = hexToString(finalString.toLowerCase() );
                mDumpTextView.setText(finalString.substring(2,finalString.length()-2));
            }else{
                mDumpTextView.setText("thinking");
            }
        }catch (Exception e){
            throw e;
        }




    }

    private static final Map<String, Character> lookupHex = new HashMap<String, Character>();

    static {
        for(int i = 0; i < 256; i++) {
            String key = Integer.toHexString(i);
            Character value = (char)(Integer.parseInt(key, 16));
            lookupHex.put(key, value);
        }
    }

    public String hexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        for (int count = 0; count < hex.length() - 1; count += 2) {
            String output = hex.substring(count, (count + 2));
            sb.append((char)lookupHex.get(output));
        }
        return sb.toString();
    }


}
