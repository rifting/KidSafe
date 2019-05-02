package com.mansourappdevelopment.androidapp.kidsafe.activities;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mansourappdevelopment.androidapp.kidsafe.broadcasts.AdminReceiver;
import com.mansourappdevelopment.androidapp.kidsafe.R;
import com.mansourappdevelopment.androidapp.kidsafe.services.MainForegroundService;
import com.mansourappdevelopment.androidapp.kidsafe.utils.Constant;

public class ChildSignedInActivity extends AppCompatActivity {
    private static final String TAG = "ChildSignedInTAG";
    public static final int JOB_ID = 38;
    public static final String CHILD_EMAIL = "childEmail";
    private FirebaseAuth auth;
    private FirebaseUser user;
    private ImageButton btnBack;
    private ImageButton btnSettings;
    private TextView txtTitle;
    private FrameLayout toolbar;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_signed_in);

        startActivity(new Intent(this, PermissionsActivity.class));


        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        String email = user.getEmail();
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString(CHILD_EMAIL, email);

        toolbar = (FrameLayout) findViewById(R.id.toolbar);
        btnBack = (ImageButton) findViewById(R.id.btnBack);
        btnBack.setImageDrawable(getResources().getDrawable(R.drawable.ic_home_));
        btnSettings = (ImageButton) findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChildSignedInActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
        txtTitle = (TextView) findViewById(R.id.txtTitle);
        txtTitle.setText(getString(R.string.home));

        //schedualJob(bundle);
        startMainForegroundService(email);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.DEVICE_ADMIN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "onActivityResult: DONE");
            }
        }
    }

    /*@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void schedualJob(PersistableBundle bundle) {
        ComponentName componentName = new ComponentName(this, UploadAppsService.class);
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(15 * 60 * 1000)
                .setExtras(bundle)
                .build();
        JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = jobScheduler.schedule(jobInfo);

        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            //Success
        } else {
            //Failure
        }
    }*/

    /*@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void cancelJob() {
        JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(JOB_ID);
        //Job cancelled
    }*/

    private void startMainForegroundService(String email) {
        Intent intent = new Intent(this, MainForegroundService.class);
        intent.putExtra(CHILD_EMAIL, email);
        ContextCompat.startForegroundService(this, intent);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
