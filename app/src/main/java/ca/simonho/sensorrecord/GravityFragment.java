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
import android.widget.TextView;

import com.androidplot.xy.XYPlot;

public class GravityFragment extends Fragment implements SensorEventListener {

    SensorManager sensorManager;
    Sensor gravity;
    Handler handler;
    Runnable runnable;
    TextView textViewXAxis;
    TextView textViewYAxis;
    TextView textViewZAxis;

    float[] gravData;

    XYPlot plot;
    DynamicLinePlot dynamicPlot;

    public GravityFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_gravity, container, false);

        //Set the nav drawer item highlight
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.navigationView.setCheckedItem(R.id.nav_gravity);

        //Set actionbar title
        mainActivity.setTitle("Gravity");

        //Get text views
        textViewXAxis = view.findViewById(R.id.value_x_axis);
        textViewYAxis = view.findViewById(R.id.value_y_axis);
        textViewZAxis = view.findViewById(R.id.value_z_axis);

        //Sensor manager
        sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        gravity = sensorManager.getDefaultSensor(MainActivity.TYPE_GRAVITY);

        //Create graph
        gravData = new float[3];

        plot = view.findViewById(R.id.plot_sensor);
        dynamicPlot = new DynamicLinePlot(plot, getContext(), "Gravity (m/s^2)");
        dynamicPlot.setMaxRange(12);
        dynamicPlot.setMinRange(-12);
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
                updateGravText();
            }
        };

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
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
        gravData = event.values;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Safe not to implement
    }

    private void plotData(){
        dynamicPlot.setData(gravData[0], 0);
        dynamicPlot.setData(gravData[1], 1);
        dynamicPlot.setData(gravData[2], 2);

        dynamicPlot.draw();
    }

    protected void updateGravText(){
        // Update the gravity data
        textViewXAxis.setText(String.format("%.2f", gravData[0]));
        textViewYAxis.setText(String.format("%.2f", gravData[1]));
        textViewZAxis.setText(String.format("%.2f", gravData[2]));
    }

}
