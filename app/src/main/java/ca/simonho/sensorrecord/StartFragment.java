package ca.simonho.sensorrecord;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class StartFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = "StartFragment";

    Button startButton;
    CoordinatorLayout coordinatorLayout;
    DBHelper dbHelper;
    TextView recordProgressMessage;
    static MainActivity mainActivity;
    static ProgressDialog stopDialog;

    public StartFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_start, container, false);

        coordinatorLayout = (CoordinatorLayout) getActivity().findViewById(R.id.coordinator_layout);

        //Set the nav drawer item highlight
        mainActivity = (MainActivity)getActivity();
        mainActivity.navigationView.setCheckedItem(R.id.nav_start);

        //Set actionbar title
        mainActivity.setTitle("Start");

        //DBHelper
        dbHelper = DBHelper.getInstance(getActivity());

        //Get form text view element and set
        recordProgressMessage = (TextView) view.findViewById(R.id.start_recording_progress);
        TextView subNum = (TextView) view.findViewById(R.id.start_value_subNum);
        subNum.setText(dbHelper.getTempSubInfo("subNum"));

        //Set onclick listener for save button
        startButton = (Button) view.findViewById(R.id.startButton);
        startButton.setOnClickListener(this);

        //Set button state depending on whether recording has been started and/or stopped
        if(MainActivity.dataRecordStarted){
            if(MainActivity.dataRecordCompleted){
                //started and completed: disable button completely
                startButton.setEnabled(false);
                startButton.setText(R.string.start_button_label_stop);
            } else {
                //started and not completed: enable STOP button
                startButton.setEnabled(true);
                startButton.setText(R.string.start_button_label_stop);
            }
        } else {
            //Haven't started: enable START button
            startButton.setEnabled(true);
            startButton.setText(R.string.start_button_label_start);
        }

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onClick(View v) {
        if (!MainActivity.dataRecordStarted){
            try{
                //Disable the hamburger, and swipes, while recording
                mainActivity.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                mainActivity.hamburger.setDrawerIndicatorEnabled(false);
                mainActivity.hamburger.setHomeAsUpIndicator(new DrawerArrowDrawable(getActivity()));
                mainActivity.hamburger.syncState();

                //Disable options menu items while recording
                mainActivity.optionsMenu.setGroupEnabled(0, false);

                //Set recording progress message
                recordProgressMessage.setText(R.string.start_recording_progress);
                MainActivity.dataRecordStarted = true;
                startButton.setText(R.string.start_button_label_stop);

                //Insert start time of recording
                dbHelper.setStartTime(Short.parseShort(dbHelper.getTempSubInfo("subNum")), System.currentTimeMillis());

                //Start the service
                Intent startService = new Intent(mainActivity, SensorService.class);
                startService.putExtra("MESSENGER", new Messenger(messageHandler));
                getContext().startService(startService);

                Snackbar.make(coordinatorLayout, R.string.start_recording, Snackbar.LENGTH_SHORT).show();

            } catch (SQLException e){
                mainActivity.logger.e(getActivity(),TAG, "SQL error insertSubject()", e);
            }
        } else {
            MainActivity.dataRecordCompleted = true;
            startButton.setEnabled(false);
            recordProgressMessage.setText("");

            //Stop the service
            mainActivity.stopService(new Intent(mainActivity, SensorService.class));

            //Re-enable the hamburger, and swipes, after recording
            mainActivity.drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            mainActivity.hamburger.setDrawerIndicatorEnabled(true);
            mainActivity.hamburger.syncState();

            //Re-enable options menu
            mainActivity.optionsMenu.setGroupEnabled(0, true);

            //Show snackbar message for recording complete
            Snackbar.make(coordinatorLayout, R.string.start_recording_complete, Snackbar.LENGTH_SHORT).show();
        }
    }

    //Message handler for service
    public static Handler messageHandler = new MessageHandler();

    public static class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            int state = message.arg1;
            switch (state) {
                case 0:
                    //Dismiss dialog
                    stopDialog.dismiss();
                    Log.d(TAG, "Stop dialog dismissed");
                    break;

                case 1:
                    //Show stop dialog
                    stopDialog = new ProgressDialog(mainActivity);
                    stopDialog.setTitle("Stopping sensors");
                    stopDialog.setMessage("Please wait...");
                    stopDialog.setProgressNumberFormat(null);
                    stopDialog.setCancelable(false);
                    stopDialog.setMax(100);
                    stopDialog.show();
                    Log.d(TAG, "Stop dialog displayed");
                    break;
            }
        }
    }
}
