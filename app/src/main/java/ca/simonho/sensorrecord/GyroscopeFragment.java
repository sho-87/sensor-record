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
import android.widget.RadioButton;
import android.widget.TextView;

import com.androidplot.xy.XYPlot;

public class GyroscopeFragment extends Fragment implements SensorEventListener {

    SensorManager sensorManager;
    Sensor sensor;
    Sensor gyroscope;
    Handler handler;
    Runnable runnable;
    TextView textViewXAxis;
    TextView textViewYAxis;
    TextView textViewZAxis;
    RadioButton gyroCalibrated;
    RadioButton gyroUncalibrated;

    float[] gyroData;

    XYPlot plot;
    DynamicLinePlot dynamicPlot;

    public GyroscopeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_gyroscope, container, false);

        //Set the nav drawer item highlight
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.navigationView.setCheckedItem(R.id.nav_gyroscope);

        //Set actionbar title
        mainActivity.setTitle("Gyroscope");

        //Get text views
        textViewXAxis = view.findViewById(R.id.value_x_axis);
        textViewYAxis = view.findViewById(R.id.value_y_axis);
        textViewZAxis = view.findViewById(R.id.value_z_axis);

        //Get radio buttons
        gyroCalibrated = view.findViewById(R.id.gyro_select_calibrated);
        gyroUncalibrated = view.findViewById(R.id.gyro_select_uncalibrated);

        gyroCalibrated.setChecked(true);

        //Sensor manager
        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        gyroscope = sensorManager.getDefaultSensor(MainActivity.TYPE_GYROSCOPE);

        //Create graph
        gyroData = new float[3];

        plot = view.findViewById(R.id.plot_sensor);
        dynamicPlot = new DynamicLinePlot(plot, getContext(), "Rotation (rad/sec)");
        dynamicPlot.setMaxRange(10);
        dynamicPlot.setMinRange(-10);
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
                updateGyroText();
            }
        };

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED), SensorManager.SENSOR_DELAY_FASTEST);

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

        if(gyroCalibrated.isChecked() & i == MainActivity.TYPE_GYROSCOPE){
            gyroData = event.values;
        } else if(gyroUncalibrated.isChecked() & i == Sensor.TYPE_GYROSCOPE_UNCALIBRATED){
            gyroData = event.values;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Safe not to implement
    }

    private void plotData(){
        dynamicPlot.setData(gyroData[0], 0);
        dynamicPlot.setData(gyroData[1], 1);
        dynamicPlot.setData(gyroData[2], 2);

        dynamicPlot.draw();
    }

    protected void updateGyroText(){
        // Update the gyroscope data
        textViewXAxis.setText(String.format("%.2f", gyroData[0]));
        textViewYAxis.setText(String.format("%.2f", gyroData[1]));
        textViewZAxis.setText(String.format("%.2f", gyroData[2]));
    }

}
