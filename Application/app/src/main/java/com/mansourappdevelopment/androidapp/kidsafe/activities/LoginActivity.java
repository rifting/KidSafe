package com.mansourappdevelopment.androidapp.kidsafe.activities;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mansourappdevelopment.androidapp.kidsafe.interfaces.OnPasswordResetListener;
import com.mansourappdevelopment.androidapp.kidsafe.utils.LocaleUtils;
import com.mansourappdevelopment.androidapp.kidsafe.R;
import com.mansourappdevelopment.androidapp.kidsafe.fragments.LoadingDialogFragment;
import com.mansourappdevelopment.androidapp.kidsafe.fragments.RecoverPasswordDialogFragment;
import com.mansourappdevelopment.androidapp.kidsafe.utils.Constant;
import com.mansourappdevelopment.androidapp.kidsafe.utils.SharedPrefsUtils;

public class LoginActivity extends AppCompatActivity implements OnPasswordResetListener {
    private static final String TAG = "LoginActivityTAG";
    private EditText txtLogInEmail;
    private EditText txtLogInPassword;
    private Button btnLogin;
    private TextView txtSignUp;
    private Button btnGoogleSignUp;
    private TextView txtForgotPassword;
    private CheckBox checkBoxRememberMe;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FragmentManager fragmentManager;
    private String uid;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private String emailPrefs;
    private String passwordPrefs;
    private boolean autoLoginPrefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        fragmentManager = getSupportFragmentManager();
        LocaleUtils.setAppLanguage(this);


        //FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("users");


        txtLogInEmail = (EditText) findViewById(R.id.txtLogInEmail);
        txtLogInPassword = (EditText) findViewById(R.id.txtLogInPassword);
        txtForgotPassword = (TextView) findViewById(R.id.txtForgotPassword);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        checkBoxRememberMe = (CheckBox) findViewById(R.id.checkBoxRememberMe);
        //progressBar.setVisibility(View.GONE);

        btnLogin = (Button) findViewById(R.id.btnLogin);
        txtSignUp = (TextView) findViewById(R.id.txtSignUp);
        btnGoogleSignUp = (Button) findViewById(R.id.btnSignUpGoogle);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoLogin();
                String email = txtLogInEmail.getText().toString();
                String password = txtLogInPassword.getText().toString();
                login(email, password);
            }
        });

        txtSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSignUpActivity();
            }
        });

        txtForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recoverPassword();
            }
        });

        btnGoogleSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        autoLoginPrefs = SharedPrefsUtils.getBooleanPreference(this, Constant.AUTO_LOGIN, false);
        checkBoxRememberMe.setChecked(autoLoginPrefs);

        emailPrefs = SharedPrefsUtils.getStringPreference(this, Constant.EMAIL);
        passwordPrefs = SharedPrefsUtils.getStringPreference(this, Constant.PASSWORD);
        if (autoLoginPrefs) {
            txtLogInEmail.setText(emailPrefs);
            txtLogInPassword.setText(passwordPrefs);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (autoLoginPrefs) {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                String email = user.getEmail();
                checkMode(email);
            }
        }
    }

    private boolean validateForm() {
        //String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

        if (txtLogInEmail.getText().toString().equals("")) {
            txtLogInEmail.setError(getString(R.string.enter_your_email));
            return false;
        }
        if (txtLogInPassword.getText().toString().equals("")) {
            txtLogInPassword.setError(getString(R.string.enter_your_password));
        }
        /*if (!txtLogInEmail.getText().toString().trim().matches(emailPattern)) {
            txtLogInEmail.setError("Enter a valid email");
            return false;
        }*/

        if (txtLogInPassword.getText().toString().length() < 6) {
            txtLogInPassword.setError(getString(R.string.enter_valid_password));
            return false;
        }
        return true;
    }

    private void login(String email, String password) {
        if (validateForm()) {
            //progressBar.setVisibility(View.VISIBLE);//TODO:: validate to avoid exceptions
            final LoadingDialogFragment loadingDialogFragment = new LoadingDialogFragment();
            startLoadingFragment(loadingDialogFragment);
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            //progressBar.setVisibility(View.GONE);
                            stopLoadingFragment(loadingDialogFragment);
                            if (task.isSuccessful()) {
                                FirebaseUser user = auth.getCurrentUser();
                                String email = user.getEmail();
                                Toast.makeText(LoginActivity.this, getString(R.string.authentication_succeeded), Toast.LENGTH_SHORT).show();
                                //update ui -> go to signedIn activity, but verify which mode first
                                checkMode(email);
                            } else {
                                String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();
                                switch (errorCode) {
                                    case "ERROR_INVALID_EMAIL":
                                        txtLogInEmail.setError(getString(R.string.enter_valid_email));
                                        break;
                                    case "ERROR_USER_NOT_FOUND":
                                        txtLogInEmail.setError(getString(R.string.email_isnt_registered));
                                        break;
                                    case "ERROR_WRONG_PASSWORD":
                                        txtLogInPassword.setError(getString(R.string.wrong_password));
                                        break;
                                    default:
                                        Toast.makeText(LoginActivity.this, getString(R.string.authentication_failed), Toast.LENGTH_SHORT).show();
                                }
                                //TODO:: check if the google play services exists or not.
                            }
                        }
                    });
        }
    }

    private void recoverPassword() {
        RecoverPasswordDialogFragment recoverPasswordDialogFragment = new RecoverPasswordDialogFragment();
        recoverPasswordDialogFragment.setCancelable(false);
        recoverPasswordDialogFragment.show(fragmentManager, Constant.RECOVER_PASSWORD_FRAGMENT);
    }

    private void startLoadingFragment(LoadingDialogFragment loadingDialogFragment) {
        loadingDialogFragment.setCancelable(false);
        loadingDialogFragment.show(fragmentManager, Constant.LOADING_FRAGMENT);
    }

    private void stopLoadingFragment(LoadingDialogFragment loadingDialogFragment) {
        loadingDialogFragment.dismiss();
    }

    private void startParentSignedInActivity() {
        Intent intent = new Intent(this, ParentSignedInActivity.class);
        startActivity(intent);
    }

    private void startChildSignedInActivity() {
        Intent intent = new Intent(this, ChildSignedInActivity.class);
        startActivity(intent);
    }

    private void startSignUpActivity() {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    private void checkMode(String email) {
        final LoadingDialogFragment loadingDialogFragment = new LoadingDialogFragment();
        startLoadingFragment(loadingDialogFragment);
        Query query = databaseReference.child("parents").orderByChild("email").equalTo(email);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                loadingDialogFragment.dismiss();
                if (dataSnapshot.exists()) {
                    startParentSignedInActivity();

                } else {
                    startChildSignedInActivity();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void autoLogin() {
        SharedPrefsUtils.setBooleanPreference(this, Constant.AUTO_LOGIN, checkBoxRememberMe.isChecked());
        SharedPrefsUtils.setStringPreference(this, Constant.EMAIL, txtLogInEmail.getText().toString());
        SharedPrefsUtils.setStringPreference(this, Constant.PASSWORD, txtLogInPassword.getText().toString());

    }

    private void signInWithGoogle() {
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, Constant.RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constant.RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(this, getString(R.string.authentication_failed), Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Google sign in failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
        AuthCredential authCredential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(authCredential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.i(TAG, "onComplete: Authentication Succeeded");
                            Toast.makeText(LoginActivity.this, getString(R.string.authentication_succeeded), Toast.LENGTH_SHORT).show();
                            FirebaseUser user = auth.getCurrentUser();
                            checkMode(user.getEmail());

                        }
                    }
                });
    }


    @Override
    public void onOkClicked(String email) {
        recoverPassword(email);
    }

    @Override
    public void onCancelClicked() {
        Toast.makeText(this, getString(R.string.canceled), Toast.LENGTH_SHORT).show();

    }


    private void recoverPassword(String email) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, getString(R.string.password_reset_email_sent), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }
}
