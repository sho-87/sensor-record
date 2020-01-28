package ca.simonho.sensorrecord;


import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class SaveFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "SaveFragment";

    boolean exportDataCSV = true; //flag for exporting full CSV of subjects data

    Button saveButton;
    TextView explanationText;
    MainActivity mainActivity;
    CoordinatorLayout coordinatorLayout;
    DBHelper dbHelper;
    Boolean subjectDataExists;
    MediaScanner mediaScanner;
    static ProgressDialog dialog;

    public SaveFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_save, container, false);
        coordinatorLayout = getActivity().findViewById(R.id.coordinator_layout);

        //Set the nav drawer item highlight
        mainActivity = (MainActivity) getActivity();
        mainActivity.navigationView.setCheckedItem(R.id.nav_save);

        //Set actionbar title
        mainActivity.setTitle("Save & Quit");

        //Get explanation text view
        explanationText = view.findViewById(R.id.saveExplanationText);

        //Get save button view
        saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(this);

        //Get DBHelper
        dbHelper = DBHelper.getInstance(getActivity(), new DatabaseHandler());

        //Set text and state of button depending on whether recording is in progress
        if(MainActivity.dataRecordStarted & !MainActivity.dataRecordCompleted){
            explanationText.setText(getResources().getString(R.string.save_message_recording));
            saveButton.setEnabled(false);
        } else {
            saveButton.setEnabled(true);

            subjectDataExists = dbHelper.checkSubjectDataExists(Short.parseShort(dbHelper.getTempSubInfo("subNum")));

            //only offer to save if sensor data exists, otherwise, just quit
            if (subjectDataExists) {
                explanationText.setText(getResources().getString(R.string.save_instruction_text));
                saveButton.setText(getResources().getString(R.string.save_button_text));
            } else {
                explanationText.setText(getResources().getString(R.string.save_no_data_text));
                saveButton.setText(getResources().getString(R.string.save_button_text_quit));
            }
        }

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onClick(View v) {
        //Alert dialog for saving/quitting
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mainActivity);

        if (subjectDataExists) {
            alertDialogBuilder.setTitle("Save and quit?");
            alertDialogBuilder.setMessage("Are you sure you want to save the data and quit the current session?");
        } else {
            alertDialogBuilder.setTitle("Quit?");
            alertDialogBuilder.setMessage("Are you sure you want to quit the current session? \n\n No data will be saved.");
        }

        alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //Save if sensor data exists, otherwise quit
                if (subjectDataExists) {
                    new ExportDatabaseCSVTask().execute();
                } else {
                    quitSession();
                }
            }
        });

        alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog quitAlertDialog = alertDialogBuilder.create();
        quitAlertDialog.show();
    }

    //Quit the current session and go back to login screen
    private void quitSession(){
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        getActivity().finishAffinity();
    }

    //Message handler class for database progress updates
    private static class DatabaseHandler extends Handler {
        @Override
        public void handleMessage (Message msg){
            Double progressPercent = (Double) msg.obj;

            Integer progressValue = 40 + (int) Math.ceil(progressPercent/2);

            dialog.setProgress(progressValue);
        }
    }

    //Async class for CSV export task
    public class ExportDatabaseCSVTask extends AsyncTask<String, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(mainActivity);
            dialog.setTitle("Saving data");
            dialog.setMessage("Please wait...");
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setProgressNumberFormat(null);
            dialog.setCancelable(false);
            dialog.setMax(100);
            dialog.show();
        }

        protected Boolean doInBackground(final String... args) {

            //Create directories for the output csv files
            String pathToExternalStorage = Environment.getExternalStorageDirectory().toString();
            File exportDir = new File(pathToExternalStorage, "/SensorRecord");
            File subjectDataDir = new File(exportDir, "/subjects");

            publishProgress(5);
            SystemClock.sleep(100);

            if (!exportDir.exists()) {
                Boolean created = exportDir.mkdirs();
                mainActivity.logger.i(getActivity(), TAG, "Export Dir created: " + created);
            }

            publishProgress(10);
            SystemClock.sleep(100);

            if (!subjectDataDir.exists()) {
                Boolean created = subjectDataDir.mkdirs();
                mainActivity.logger.i(getActivity(), TAG, "Subject Dir created: " + created);
            }

            publishProgress(15);
            SystemClock.sleep(100);

            //If all directories have been created successfully
            if (exportDir.exists() && subjectDataDir.exists()) {
                try {
                    //Copy temp subject and sensor data to persistent db tables
                    dbHelper.copyTempData();

                    publishProgress(20);
                    SystemClock.sleep(200);

                    //Backup the SQL DB file
                    File data = Environment.getDataDirectory();
                    String currentDBPath = "//data//ca.simonho.sensorrecord//databases//" + DBHelper.DATABASE_NAME;
                    File currentDB = new File(data, currentDBPath);
                    File destDB = new File(exportDir, DBHelper.DATABASE_NAME);

                    publishProgress(25);
                    SystemClock.sleep(100);

                    if (exportDir.canWrite()) {
                        if (currentDB.exists()) {
                            FileChannel src = new FileInputStream(currentDB).getChannel();
                            FileChannel dst = new FileOutputStream(destDB).getChannel();
                            dst.transferFrom(src, 0, src.size());
                            src.close();
                            dst.close();
                        }
                    }

                    publishProgress(35);
                    SystemClock.sleep(300);

                    //Export subjects table/tracking sheet
                    File trackingSheet = new File(exportDir, "trackingSheet.csv");

                    try {
                        dbHelper.exportTrackingSheet(trackingSheet);
                    } catch (SQLException | IOException e) {
                        mainActivity.logger.e(getActivity(), TAG, "exportTrackingSheet error", e);
                    }

                    publishProgress(40);
                    SystemClock.sleep(300);

                    if(exportDataCSV) {
                        //Export individual subject data
                        String subNum = dbHelper.getTempSubInfo("subNum");
                        File subjectFile = new File(subjectDataDir, subNum + ".csv");

                        try {
                            dbHelper.exportSubjectData(subjectFile, subNum);
                        } catch (SQLException | IOException e) {
                            mainActivity.logger.e(getActivity(), TAG, "exportSubjectData error", e);
                        }
                    }

                    publishProgress(90);
                    SystemClock.sleep(300);

                    //Scan all files for MTP
                    List<String> fileList = getListFiles(exportDir);
                    String[] allFiles = new String[fileList.size()];
                    allFiles = fileList.toArray(allFiles);

                    mediaScanner = new MediaScanner();

                    try{
                        mediaScanner.scanFile(getContext(), allFiles, null, mainActivity.logger);
                    } catch (Exception e) {
                        mainActivity.logger.e(getActivity(), TAG, "Media scanner exception", e);
                    }

                    publishProgress(100);
                    SystemClock.sleep(400);

                    return true;
                } catch (SQLException | IOException e) {
                    mainActivity.logger.e(getActivity(), TAG, "Save data exception", e);

                    Snackbar.make(coordinatorLayout, "Error: " + e.getMessage(), Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.snackbar_dismiss, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            }).show();
                    return false;
                }
            } else {
                //Directories don't exist
                if (!exportDir.exists()) {
                    mainActivity.logger.e(getActivity(), TAG, "Data directory not found");

                    Snackbar.make(coordinatorLayout, "Error: Data directory doesn't exist", Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.snackbar_dismiss, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            }).show();
                } else if (!subjectDataDir.exists()) {
                    mainActivity.logger.e(getActivity(), TAG, "Subject directory not found");

                    Snackbar.make(coordinatorLayout, "Error: Subject directory doesn't exist", Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.snackbar_dismiss, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                }
                            }).show();
                }

                return false;
            }
        }

        public void onProgressUpdate(Integer ... progress){
            dialog.setProgress(progress[0]);
            if (progress[0] == 100){
                dialog.setMessage("Quitting...");
            }
        }

        protected void onPostExecute(final Boolean success) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (success) {
                //Restart app and go back to login screen
                quitSession();
            }
        }

        //Recursive file lister for MTP
        private List<String> getListFiles(File parentDir) {
            ArrayList<String> inFiles = new ArrayList<>();
            File[] files = parentDir.listFiles();

            //Loop through everything in base directory, including folders
            for (File file : files) {
                if (file.isDirectory()) {
                    //Recursively add files from subdirectories
                    inFiles.addAll(getListFiles(file));
                } else {
                    inFiles.add(file.getAbsolutePath());
                }
            }
            return inFiles;
        }
    }
}

