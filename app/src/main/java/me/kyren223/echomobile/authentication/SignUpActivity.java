package me.kyren223.echomobile.authentication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import me.kyren223.echomobile.R;
import me.kyren223.echomobile.core.MainActivity;
import me.kyren223.echomobile.core.MasterActivity;

public class SignUpActivity extends MasterActivity {

    private Button signUpButton;
    private ProgressBar progressBar;

    private EditText emailField;
    private EditText passwordField;
    private EditText displayNameField;

    private TextView emailLabel;
    private TextView passwordLabel;
    private TextView displayNameLabel;
    private TextView moveToSignIn;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        signUpButton = findViewById(R.id.registerButton);
        signUpButton.setOnClickListener(this::onRegisterButtonClicked);

        progressBar = findViewById(R.id.progressBar);

        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        passwordField.setOnTouchListener((v, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP) return false;
            if (event.getRawX() < (passwordField.getRight() - passwordField.getCompoundDrawables()[2].getBounds().width())) return false;
            AuthUtils.togglePasswordVisibility(this, passwordField);
            return true;
        });
        displayNameField = findViewById(R.id.displayNameField);

        emailLabel = findViewById(R.id.emailLabel);
        passwordLabel = findViewById(R.id.passwordLabel);
        displayNameLabel = findViewById(R.id.displayNameLabel);

        moveToSignIn = findViewById(R.id.moveToSignIn);
        moveToSignIn.setOnClickListener(this::onMoveToSignInClicked);

        Intent intent = getIntent();
        if (intent != null) {
            String email = intent.getStringExtra("email");
            String password = intent.getStringExtra("password");
            if (email != null) emailField.setText(email);
            if (password != null) passwordField.setText(password);
        }
    }

    private void onRegisterButtonClicked(View view) {
        AuthUtils.clearError(this, emailLabel);
        AuthUtils.clearError(this, passwordLabel);
        AuthUtils.clearError(this, displayNameLabel);

        boolean error = false;
        String email = emailField.getText().toString();
        String password = passwordField.getText().toString();
        String displayName = displayNameField.getText().toString();

        String emailError = AuthUtils.validateEmail(email);
        String passwordError = AuthUtils.validatePassword(password);
        String displayNameError = AuthUtils.validateDisplayName(displayName);

        if (emailError != null) {
            AuthUtils.setError(this, emailLabel, emailError);
            error = true;
        }

        if (passwordError != null) {
            AuthUtils.setError(this, passwordLabel, passwordError);
            error = true;
        }

        if (displayNameError != null) {
            AuthUtils.setError(this, displayNameLabel, displayNameError);
            error = true;
        }

        if (error) return;

        startWait();

        db.signUp(email, password, displayName, (successful) -> {
            finishWait();
            if (successful) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                AuthUtils.setError(this, emailLabel, "Email already in use");
            }
        });
    }

    private void onMoveToSignInClicked(View view) {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.putExtra("email", emailField.getText().toString());
        intent.putExtra("password", passwordField.getText().toString());
        startActivity(intent);
        finish();
    }

    private void startWait() {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        signUpButton.setEnabled(false);
        signUpButton.setText("");
        signUpButton.setBackgroundTintList(ColorStateList.valueOf(AuthUtils.getColorFromAttr(this, R.attr.primaryButtonPressedColor)));
    }

    private void finishWait() {
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        signUpButton.setEnabled(true);
        signUpButton.setText(R.string.sign_up);
        signUpButton.setBackgroundTintList(ColorStateList.valueOf(AuthUtils.getColorFromAttr(this, R.attr.primaryButtonColor)));
    }
}