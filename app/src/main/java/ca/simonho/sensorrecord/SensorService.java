package ca.simonho.sensorrecord;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.SQLException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SensorService extends Service implements SensorEventListener {

    public static final String TAG = "SensorService";

    public static final int SCREEN_OFF_RECEIVER_DELAY = 100;

    //This can't be set below 10ms due to Android/hardware limitations. Use 9 to get more accurate 10ms intervals
    final short POLL_FREQUENCY = 9; //in milliseconds

    private long lastUpdate = -1;
    long curTime;

    private Messenger messageHandler;

    private SensorManager sensorManager = null;
    private WakeLock wakeLock = null;
    ExecutorService executor;
    DBHelper dbHelper;
    Sensor sensor;
    Sensor accelerometer;
    Sensor gyroscope;
    Sensor gravity;
    Sensor magnetic;
    Sensor step;

    float[] accelerometerMatrix = new float[3];
    float[] gyroscopeMatrix = new float[3];
    float[] gravityMatrix = new float[3];
    float[] magneticMatrix = new float[3];
    float[] rotationMatrix = new float[9];

    private void registerListener() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, step, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void unregisterListener() {
        sensorManager.unregisterListener(this);
    }

    public BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive(" + intent + ")");

            if (!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                return;
            }

            Runnable runnable = new Runnable() {
                public void run() {
                    Log.i(TAG, "Runnable executing...");
                    unregisterListener();
                    registerListener();
                }
            };

            new Handler().postDelayed(runnable, SCREEN_OFF_RECEIVER_DELAY);
        }
    };

    public void sendMessage(String state) {
        Message message = Message.obtain();
        switch (state) {
            case "HIDE" :
                message.arg1 = 0;
                break;
            case "SHOW":
                message.arg1 = 1;
                break;
        }
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Safe not to implement
    }

    public void onSensorChanged(SensorEvent event) {
        sensor = event.sensor;

        //Store sensor data
        int i = sensor.getType();
        if (i == MainActivity.TYPE_ACCELEROMETER) {
            accelerometerMatrix = event.values;
        } else if (i == MainActivity.TYPE_GYROSCOPE) {
            gyroscopeMatrix = event.values;
        } else if (i == MainActivity.TYPE_GRAVITY) {
            gravityMatrix = event.values;
        } else if (i == MainActivity.TYPE_MAGNETIC) {
            magneticMatrix = event.values;
        } else if (i == MainActivity.TYPE_STEP_DETECTOR) {
            try{
                Runnable InsertSteps = new InsertSteps(event.timestamp);
                executor.execute(InsertSteps);
            } catch (SQLException e) {
                Log.e(TAG, "insertSteps: " + e.getMessage(), e);
            }
        }

        SensorManager.getRotationMatrix(rotationMatrix, null, gravityMatrix, magneticMatrix);

        curTime = event.timestamp; //in nanoseconds

        // only allow one update every POLL_FREQUENCY (convert from ms to nano for comparison).
        if((curTime - lastUpdate) > POLL_FREQUENCY*1000000) {

            lastUpdate = curTime;

            //World coordinate system transformation - do in post processing instead
            //accelerometerWorldMatrix[0] = rotationMatrix[0] * accelerometerMatrix[0] + rotationMatrix[1] * accelerometerMatrix[1] + rotationMatrix[2] * accelerometerMatrix[2];
            //accelerometerWorldMatrix[1] = rotationMatrix[3] * accelerometerMatrix[0] + rotationMatrix[4] * accelerometerMatrix[1] + rotationMatrix[5] * accelerometerMatrix[2];
            //accelerometerWorldMatrix[2] = rotationMatrix[6] * accelerometerMatrix[0] + rotationMatrix[7] * accelerometerMatrix[1] + rotationMatrix[8] * accelerometerMatrix[2];

            //insert into database in background thread
            try{
                Runnable insertHandler = new InsertHandler(curTime, accelerometerMatrix, gyroscopeMatrix,
                        gravityMatrix, magneticMatrix, rotationMatrix);
                executor.execute(insertHandler);
            } catch (SQLException e) {
                Log.e(TAG, "insertData: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        dbHelper = DBHelper.getInstance(getApplicationContext());

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(MainActivity.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(MainActivity.TYPE_GYROSCOPE);
        gravity = sensorManager.getDefaultSensor(MainActivity.TYPE_GRAVITY);
        magnetic = sensorManager.getDefaultSensor(MainActivity.TYPE_MAGNETIC);
        step = sensorManager.getDefaultSensor(MainActivity.TYPE_STEP_DETECTOR);

        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        registerReceiver(receiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        //Executor service for DB inserts
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        startForeground(Process.myPid(), new Notification());
        registerListener();
        wakeLock.acquire();

        //Message handler for progress dialog
        Bundle extras = intent.getExtras();
        messageHandler = (Messenger) extras.get("MESSENGER");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //Show dialog
        sendMessage("SHOW");

        //Unregister receiver and listener prior to executor shutdown
        unregisterReceiver(receiver);
        unregisterListener();

        //Prevent new tasks from being added to thread
        executor.shutdown();
        Log.d(TAG, "Executor shutdown is called");

        //Create new thread to wait for executor to clear queue and wait for termination
        new Thread(new Runnable() {

            public void run() {
                try {
                    //Wait for all tasks to finish before we proceed
                    while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                        Log.i(TAG, "Waiting for current tasks to finish");
                    }
                    Log.i(TAG, "No queue to clear");
                } catch (InterruptedException e) {
                    Log.e(TAG, "Exception caught while waiting for finishing executor tasks");
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }

                if (executor.isTerminated()) {
                    //Stop everything else once the task queue is clear
                    wakeLock.release();
                    stopForeground(true);

                    //Dismiss progress dialog
                    sendMessage("HIDE");
                }
            }
        }).start();
    }

    class InsertHandler implements Runnable {
        final long curTime;
        final float[] accelerometerMatrix;
        final float[] gyroscopeMatrix;
        final float[] gravityMatrix;
        final float[] magneticMatrix;
        final float[] rotationMatrix;

        //Store the current sensor array values into THIS objects arrays, and db insert from this object
        public InsertHandler(long curTime, float[] accelerometerMatrix,
                             float[] gyroscopeMatrix, float[] gravityMatrix,
                             float[] magneticMatrix, float[] rotationMatrix) {
            this.curTime = curTime;
            this.accelerometerMatrix = accelerometerMatrix;
            this.gyroscopeMatrix = gyroscopeMatrix;
            this.gravityMatrix = gravityMatrix;
            this.magneticMatrix = magneticMatrix;
            this.rotationMatrix = rotationMatrix;
        }

        public void run() {
            dbHelper.insertDataTemp(Short.parseShort(dbHelper.getTempSubInfo("subNum")),
                    this.curTime,
                    this.accelerometerMatrix,
                    this.gyroscopeMatrix,
                    this.gravityMatrix,
                    this.magneticMatrix,
                    this.rotationMatrix);
        }
    }

    class InsertSteps implements Runnable {
        final long curTime;

        //Store the current sensor array values into THIS objects arrays, and db insert from this object
        public InsertSteps(long curTime) {
            this.curTime = curTime;
        }

        public void run() {
            dbHelper.insertStepsTemp(Short.parseShort(dbHelper.getTempSubInfo("subNum")), this.curTime);
        }
    }
}