package ca.simonho.sensorrecord;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {

    EditText usernameText;
    EditText passwordText;
    Button loginButton;

    final String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WAKE_LOCK
                                };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Check if this is a fresh app launch, or a launcher click after app minimize
        if (!isTaskRoot()
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
                && getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_MAIN)) {

            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        //get fields and set username by default
        usernameText = (EditText) findViewById(R.id.input_user);
        passwordText = (EditText) findViewById(R.id.input_password);
        loginButton = (Button) findViewById(R.id.btn_login);
        showItems(true);

        //If any of the permission have not been granted, request
        if (!hasPermission(PERMISSIONS[0]) || !hasPermission(PERMISSIONS[1]) ||
                !hasPermission(PERMISSIONS[2])){

            ActivityCompat.requestPermissions(this, PERMISSIONS, 10);
        }
    }

    public boolean hasPermission(String permission){
        return (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //If at least one permission has been denied, show snackbar
        if (grantResults.length == 0 || !arePermissionsGranted(grantResults)){
            Snackbar.make(findViewById(android.R.id.content), R.string.permission_explain, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.snackbar_request_permission, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(LoginActivity.this, PERMISSIONS, 10);
                        }
                    })
                    .show();

            showItems(false);

        } else {
            //If all permissions have been granted
            showItems(true);
        }
    }

    public void showItems(Boolean show){
        if (show){
            usernameText.setEnabled(true);
            passwordText.setEnabled(true);
            loginButton.setEnabled(true);
            loginButton.setAlpha(1);
            usernameText.setText("username");
        } else {
            usernameText.setEnabled(false);
            passwordText.setEnabled(false);
            loginButton.setEnabled(false);
            loginButton.setAlpha(0.3f);
            usernameText.setText("");
        }
    }

    public boolean arePermissionsGranted(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void authenticateUser(View view) {

        EditText username = (EditText) findViewById(R.id.input_user);
        EditText password = (EditText) findViewById(R.id.input_password);

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}