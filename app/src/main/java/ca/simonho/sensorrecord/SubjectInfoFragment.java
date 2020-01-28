package ca.simonho.sensorrecord;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class SubjectInfoFragment extends Fragment implements View.OnClickListener{

    public static final String TAG = "SubjectInfoFragment";

    MainActivity mainActivity;
    Button deleteButton;
    TextView deleteMessage;
    DBHelper dbHelper;
    ProgressDialog dialog;
    CoordinatorLayout coordinatorLayout;

    public SubjectInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_subject_info, container, false);

        coordinatorLayout = getActivity().findViewById(R.id.coordinator_layout);

        //Set the nav drawer item highlight
        mainActivity = (MainActivity) getActivity();
        mainActivity.navigationView.setCheckedItem(R.id.nav_new);

        //Set actionbar title
        mainActivity.setTitle("Subject Information");

        //Listener for delete button
        deleteButton = view.findViewById(R.id.subInfo_button_delete);
        deleteButton.setOnClickListener(this);
        deleteMessage = view.findViewById(R.id.subInfo_delete_message);

        //Set state of delete button depending on whether recording is ongoing
        if(MainActivity.dataRecordStarted & !MainActivity.dataRecordCompleted){
            deleteButton.setEnabled(false);
            deleteMessage.setText(R.string.subInfo_message_recording);
        } else {
            deleteButton.setEnabled(true);
            deleteMessage.setText("");
        }

        //DBHelper
        dbHelper = DBHelper.getInstance(getActivity());

        //Get form text view elements
        TextView date = view.findViewById(R.id.subInfo_value_date);
        TextView ra = view.findViewById(R.id.subInfo_value_ra);
        TextView subNum = view.findViewById(R.id.subInfo_value_subNum);
        TextView condition = view.findViewById(R.id.subInfo_value_condition);
        TextView age = view.findViewById(R.id.subInfo_value_age);
        TextView sex = view.findViewById(R.id.subInfo_value_sex);
        TextView height = view.findViewById(R.id.subInfo_value_height);

        //Set the text view elements in layout to subject info from temp table
        date.setText(dbHelper.getTempSubInfo("date"));
        ra.setText(dbHelper.getTempSubInfo("ra"));
        subNum.setText(dbHelper.getTempSubInfo("subNum"));
        condition.setText(dbHelper.getTempSubInfo("condition"));
        age.setText(dbHelper.getTempSubInfo("age"));
        sex.setText(dbHelper.getTempSubInfo("sex"));
        height.setText(dbHelper.getTempSubInfo("height"));

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onClick(View v) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mainActivity);
        alertDialogBuilder.setTitle("Delete?");
        alertDialogBuilder.setMessage("Are you sure you want to delete the current subject?\n\n This action is irreversible.");

        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //Deleting a lot of sensor data might take a while, so use a background thread
                new DeleteSubjectTask().execute();
            }
        });

        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog deleteAlertDialog = alertDialogBuilder.create();
        deleteAlertDialog.show();
    }

    //Async class for subject delete
    public class DeleteSubjectTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(mainActivity);
            dialog.setTitle("Delete subject");
            dialog.setMessage("Please wait...");
            dialog.setCancelable(false);
            dialog.show();
        }

        protected Boolean doInBackground(final String... args) {
            try{
                mainActivity.logger.i(getActivity(), TAG, "Subject deleted: #" + dbHelper.getTempSubInfo("subNum"));

                //Clear the temp table of this users data
                dbHelper.deleteSubject();

                //Set subCreated flag to false
                MainActivity.subCreated = false;

                return true;
            } catch (SQLException e){
                mainActivity.logger.e(getActivity(), TAG, "SQL error deleteSubject()",e);

                Snackbar.make(coordinatorLayout, "Error: " + e.getMessage(), Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.snackbar_dismiss, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                            }
                        }).show();

                return false;
            }
        }

        protected void onPostExecute(final Boolean success) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (success) {
                //Restart the main activity
                Snackbar.make(coordinatorLayout, "Subject deleted", Snackbar.LENGTH_SHORT).show();
                mainActivity.recreate();
            }
        }
    }

}
