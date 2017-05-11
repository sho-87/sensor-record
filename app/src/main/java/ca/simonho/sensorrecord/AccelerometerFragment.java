package ca.simonho.sensorrecord;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.androidplot.xy.XYPlot;

public class AccelerometerFragment extends Fragment implements SensorEventListener {

    SensorManager sensorManager;
    Sensor sensor;
    Sensor accelerometer;
    Sensor magnetic;
    Sensor gravity;
    Handler handler;
    Runnable runnable;
    TextView textViewXAxis;
    TextView textViewYAxis;
    TextView textViewZAxis;
    CheckBox linearCheckbox;
    CheckBox worldCheckbox;

    float[] accData;
    float[] gravData;
    float[] magData;
    float[] plotData;

    XYPlot plot;
    DynamicLinePlot dynamicPlot;

    public AccelerometerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_accelerometer, container, false);

        //Set the nav drawer item highlight
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.navigationView.setCheckedItem(R.id.nav_accelerometer);

        //Set actionbar title
        mainActivity.setTitle("Accelerometer");

        //Get text views
        textViewXAxis = (TextView) view.findViewById(R.id.value_x_axis);
        textViewYAxis = (TextView) view.findViewById(R.id.value_y_axis);
        textViewZAxis = (TextView) view.findViewById(R.id.value_z_axis);

        //Get checkboxes
        linearCheckbox = (CheckBox) view.findViewById(R.id.linearCheckbox);
        worldCheckbox = (CheckBox) view.findViewById(R.id.worldCheckbox);

        //Sensor manager
        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(MainActivity.TYPE_ACCELEROMETER);
        gravity = sensorManager.getDefaultSensor(MainActivity.TYPE_GRAVITY);
        magnetic = sensorManager.getDefaultSensor(MainActivity.TYPE_MAGNETIC);

        //Create graph
        accData = new float[3];
        gravData = new float[3];
        magData = new float[3];
        plotData = new float[3];

        plot = (XYPlot) view.findViewById(R.id.plot_sensor);
        dynamicPlot = new DynamicLinePlot(plot, getContext(), "Acceleration (m/s^2)");
        dynamicPlot.setMaxRange(18);
        dynamicPlot.setMinRange(-18);
        dynamicPlot.addSeriesPlot("X", 0, ContextCompat.getColor(getContext(), R.color.graphX));
        dynamicPlot.addSeriesPlot("Y", 1, ContextCompat.getColor(getContext(), R.color.graphY));
        dynamicPlot.addSeriesPlot("Z", 2, ContextCompat.getColor(getContext(), R.color.graphZ));

        //Handler for graph plotting on background thread
        handler = new Handler();

        //Runnable for background plotting
        runnable = new Runnable()
        {
            @Override
            public void run() {
                handler.postDelayed(this, 10);
                plotData();
                updateAccelerationText();
            }
        };

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_FASTEST);
        handler.post(runnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        handler.removeCallbacks(runnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        sensor = event.sensor;

        int i = sensor.getType();

        if (i == MainActivity.TYPE_ACCELEROMETER) {
            accData = event.values;
        } else if (i == MainActivity.TYPE_GRAVITY) {
            gravData = event.values;
        } else if (i == MainActivity.TYPE_MAGNETIC) {
            magData = event.values;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Safe not to implement
    }

    private float[] calcLinearAcc(float[] accData, float[] gravData){
        float[] data = new float[3];

        data[0] = accData[0] - gravData[0];
        data[1] = accData[1] - gravData[1];
        data[2] = accData[2] - gravData[2];

        return data;
    }

    private float[] calcWorldAcc(float[] accData, float[] magData, float[] gravData){
        float[] data = new float[3];
        float[] rotationMatrix = new float[9];

        SensorManager.getRotationMatrix(rotationMatrix, null, gravData, magData);

        data[0] = rotationMatrix[0] * accData[0] + rotationMatrix[1] * accData[1] + rotationMatrix[2] * accData[2];
        data[1] = rotationMatrix[3] * accData[0] + rotationMatrix[4] * accData[1] + rotationMatrix[5] * accData[2];
        data[2] = rotationMatrix[6] * accData[0] + rotationMatrix[7] * accData[1] + rotationMatrix[8] * accData[2];

        return data;
    }

    private void plotData(){
        if(linearCheckbox.isChecked() & worldCheckbox.isChecked()){
            plotData = calcWorldAcc(calcLinearAcc(accData, gravData), magData, gravData);
        } else if(linearCheckbox.isChecked()){
            plotData = calcLinearAcc(accData, gravData);
        } else if(worldCheckbox.isChecked()){
            plotData = calcWorldAcc(accData, magData, gravData);
        } else {
            plotData = accData;
        }

        dynamicPlot.setData(plotData[0], 0);
        dynamicPlot.setData(plotData[1], 1);
        dynamicPlot.setData(plotData[2], 2);

        dynamicPlot.draw();
    }

    protected void updateAccelerationText(){
        // Update the acceleration data
        textViewXAxis.setText(String.format("%.2f", plotData[0]));
        textViewYAxis.setText(String.format("%.2f", plotData[1]));
        textViewZAxis.setText(String.format("%.2f", plotData[2]));
    }

}
