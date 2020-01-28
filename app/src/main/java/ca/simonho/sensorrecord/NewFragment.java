package ca.simonho.sensorrecord;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NewFragment extends Fragment implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    public static final String TAG = "NewFragment";

    MainActivity mainActivity;
    CoordinatorLayout coordinatorLayout;
    DBHelper dbHelper;

    TextInputLayout raWrapper;
    TextInputLayout subNumWrapper;
    TextInputLayout conditionWrapper;
    TextInputLayout ageWrapper;
    EditText heightCM;
    EditText heightFT;
    EditText heightIN;
    TextView heightFTSym;
    TextView heightINSym;

    Integer height;
    String heightUnit;
    Boolean heightEntered;
    String sex;
    RadioGroup sexGroup;
    Button createButton;

    public NewFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        coordinatorLayout = getActivity().findViewById(R.id.coordinator_layout);
        View view = inflater.inflate(R.layout.fragment_new, container, false);

        //Set the nav drawer item highlight
        mainActivity = (MainActivity) getActivity();
        mainActivity.navigationView.setCheckedItem(R.id.nav_new);

        //Set actionbar title
        mainActivity.setTitle("New Participant");

        //Get dbHelper
        dbHelper = DBHelper.getInstance(getActivity());

        //Height unit spinner
        String[] values = {"cm", "ft/in"};
        Spinner spinner = view.findViewById(R.id.input_height_spinner);
        ArrayAdapter<String> LTRadapter = new ArrayAdapter<>(this.getActivity(), android.R.layout.simple_spinner_item, values);
        LTRadapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(LTRadapter);

        //Set a flag for when the user manually chooses the spinner (for focus setting on height EditText)
        spinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                MainActivity.heightUnitSpinnerTouched = true;
                return false;
            }
        });

        //Set the listener for spinner selection events
        spinner.setOnItemSelectedListener(this);

        //Get all text fields
        raWrapper = view.findViewById(R.id.input_ra_wrapper);
        subNumWrapper = view.findViewById(R.id.input_subnum_wrapper);
        conditionWrapper = view.findViewById(R.id.input_condition_wrapper);
        ageWrapper = view.findViewById(R.id.input_age_wrapper);

        heightCM = view.findViewById(R.id.input_height_cm);
        heightFT = view.findViewById(R.id.input_height_ft);
        heightIN = view.findViewById(R.id.input_height_in);
        heightFTSym = view.findViewById(R.id.input_label_height_ft_symbol);
        heightINSym = view.findViewById(R.id.input_label_height_in_symbol);

        //Listener for create button
        createButton = view.findViewById(R.id.input_submit);
        createButton.setOnClickListener(this);

        // Inflate the layout for this fragment
        return view;
    }

    //Height unit spinner item selection
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        switch (position) {
            case 0:
                heightCM.setVisibility(View.VISIBLE);
                heightFT.setVisibility(View.INVISIBLE);
                heightIN.setVisibility(View.INVISIBLE);
                heightFTSym.setVisibility(View.INVISIBLE);
                heightINSym.setVisibility(View.INVISIBLE);
                heightUnit = "cm";

                //Only request focus if the user manually selected the spinner
                //Otherwise focus will be pulled on layout inflation
                if (MainActivity.heightUnitSpinnerTouched) {
                    heightCM.requestFocus();
                }
                break;
            case 1:
                heightCM.setVisibility(View.INVISIBLE);
                heightFT.setVisibility(View.VISIBLE);
                heightIN.setVisibility(View.VISIBLE);
                heightFTSym.setVisibility(View.VISIBLE);
                heightINSym.setVisibility(View.VISIBLE);
                heightUnit = "ft";

                //Only request focus if the user manually selected the spinner
                //Otherwise focus will be pulled on layout inflation
                if (MainActivity.heightUnitSpinnerTouched) {
                    heightFT.requestFocus();
                }
                break;
        }
    }

    //Height unit spinner item selection
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //Do nothing
    }

    //Create/submit button click
    @Override
    public void onClick(View v) {
        //Get input values
        String ra = raWrapper.getEditText().getText().toString();
        String subNum = subNumWrapper.getEditText().getText().toString();
        String condition = conditionWrapper.getEditText().getText().toString();
        String age = ageWrapper.getEditText().getText().toString();
        String heightCMValue = heightCM.getText().toString();
        String heightFTValue = heightFT.getText().toString();
        String heightINValue = heightIN.getText().toString();
        TextView sexLabel = mainActivity.findViewById(R.id.input_label_sex);
        TextView heightLabel = mainActivity.findViewById(R.id.input_label_height);

        sexGroup = mainActivity.findViewById(R.id.input_sex);
        int sexID = sexGroup.getCheckedRadioButtonId();

        if (sexID != -1) {
            View radioButton = sexGroup.findViewById(sexID);
            int radioId = sexGroup.indexOfChild(radioButton);
            RadioButton btn = (RadioButton) sexGroup.getChildAt(radioId);
            sex = (String) btn.getText();
            sexLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondaryText));
        }

        if (heightUnit.equals("cm")) {
            if (!isEmpty(heightCMValue)) {
                heightLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondaryText));
                height = Integer.parseInt(heightCM.getText().toString());
                heightEntered = true;
            } else {
                heightEntered = false;
            }
        } else if (heightUnit.equals("ft")) {
            if (!isEmpty(heightFTValue) & !isEmpty(heightINValue)) {
                heightLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondaryText));
                int feet = Integer.parseInt(heightFT.getText().toString());
                int inches = Integer.parseInt(heightIN.getText().toString());

                height = (int) ((feet * 30) + (inches * 2.54));
                heightEntered = true;
            } else {
                heightEntered = false;
            }
        }

        //If all the validation passes, submit the form. Else, show errors
        if (!isEmpty(ra) & !isEmpty(subNum) & !isEmpty(condition) & !isEmpty(age) & heightEntered & sexID != -1) {
            //Turn all errors off
            raWrapper.setError(null);
            subNumWrapper.setError(null);
            conditionWrapper.setError(null);
            ageWrapper.setError(null);

            //check if subject already exists in main persistent subject table
            if (!dbHelper.checkSubjectExists(Short.parseShort(subNum))) {
                //subject doesn't already exist

                //Insert subject into TEMP subject table
                MainActivity.subCreated = true;

                dbHelper.insertSubjectTemp(
                        Short.parseShort(subNum),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CANADA).format(new Date()),
                        ra,
                        Short.parseShort(condition),
                        Short.parseShort(age),
                        sex,
                        height.shortValue(),
                        0
                );

                //Hide the keyboard on click
                showKeyboard(false, mainActivity);

                //Enable additional menu items/fragments for recording and saving sensor data
                mainActivity.navigationView.getMenu().findItem(R.id.nav_start).setEnabled(true);
                mainActivity.navigationView.getMenu().findItem(R.id.nav_save).setEnabled(true);
                mainActivity.navigationView.getMenu().findItem(R.id.nav_new).setTitle("Subject Info");

                Snackbar.make(coordinatorLayout, "Subject created", Snackbar.LENGTH_SHORT).show();
                mainActivity.logger.i(getActivity(), TAG, "Subject #" + subNum + " created");

                //Change fragment to subject info screen. Do not add this fragment to the backstack
                mainActivity.addFragment(new SubjectInfoFragment(), false);
            } else {
                //subject exists. Set focus on subject number field
                Snackbar.make(coordinatorLayout, "Subject number already exists...", Snackbar.LENGTH_SHORT).show();
                subNumWrapper.requestFocus();
            }
        } else {
            if (isEmpty(ra)) {
                raWrapper.setError("Research assistant name required");
            } else {
                raWrapper.setError(null);
            }

            if (isEmpty(subNum)) {
                subNumWrapper.setError("Subject number required");
            } else {
                subNumWrapper.setError(null);
            }

            if (isEmpty(condition)) {
                conditionWrapper.setError("Condition required");
            } else {
                conditionWrapper.setError(null);
            }

            if (isEmpty(age)) {
                ageWrapper.setError("Age required");
            } else {
                ageWrapper.setError(null);
            }

            //If no radio button has been selected
            if (sexID == -1) {
                sexLabel.setTextColor(Color.RED);
            }

            //If the appropriate height fields are empty
            if (heightUnit.equals("cm")) {
                if (isEmpty(heightCMValue)) {
                    heightLabel.setTextColor(Color.RED);
                }
            } else if (heightUnit.equals("ft")) {
                if (isEmpty(heightFTValue) || isEmpty(heightINValue)) {
                    heightLabel.setTextColor(Color.RED);
                }
            }
        }
    }

    //Check if a string is empty
    public boolean isEmpty(String string) {
        return string.equals("");
    }

    public void showKeyboard(Boolean show, MainActivity mainActivity) {
        InputMethodManager imm = (InputMethodManager) mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (show) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        } else {
            // check if no view has focus before hiding keyboard
            View v = mainActivity.getCurrentFocus();
            if (v != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
    }

}
