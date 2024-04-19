package me.kyren223.echomobile.authentication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import me.kyren223.echomobile.R;
import me.kyren223.echomobile.core.MainActivity;
import me.kyren223.echomobile.core.MasterActivity;

public class SignInActivity extends MasterActivity {

    private Button signInButton;
    private ProgressBar progressBar;

    private EditText emailField;
    private EditText passwordField;

    private TextView emailLabel;
    private TextView passwordLabel;

    private TextView moveToSignUp;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        signInButton = findViewById(R.id.loginButton);
        signInButton.setOnClickListener(this::onLoginButtonClicked);

        progressBar = findViewById(R.id.progressBar);

        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        passwordField.setOnTouchListener((v, event) -> {
            if (event.getAction() != MotionEvent.ACTION_UP) return false;
            if (event.getRawX() < (passwordField.getRight() - passwordField.getCompoundDrawables()[2].getBounds().width())) return false;
            AuthUtils.togglePasswordVisibility(this, passwordField);
            return true;
        });

        emailLabel = findViewById(R.id.emailLabel);
        passwordLabel = findViewById(R.id.passwordLabel);

        moveToSignUp = findViewById(R.id.moveToSignUp);
        moveToSignUp.setOnClickListener(this::onMoveToSignUpClicked);
        configureMoveToSignUp();

        TextView forgotPassword = findViewById(R.id.forgotPassword);
        forgotPassword.setOnClickListener(this::onForgotPasswordClicked);


        Intent intent = getIntent();
        if (intent != null) {
            String email = intent.getStringExtra("email");
            String password = intent.getStringExtra("password");
            if (email != null) emailField.setText(email);
            if (password != null) passwordField.setText(password);
        }
    }

    private void onForgotPasswordClicked(View view) {

    }

    private void configureMoveToSignUp() {
        SpannableString spannableString = new SpannableString(moveToSignUp.getText());
        int start = spannableString.toString().indexOf("Register");
        int end = start + "Register".length();

        //spannableString.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(AuthUtils.getColorFromAttr(this, R.attr.highlightedTextColor)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        moveToSignUp.setText(spannableString);
    }

    private void onLoginButtonClicked(View view) {
        AuthUtils.clearError(this, emailLabel);
        AuthUtils.clearError(this, passwordLabel);

        boolean error = false;
        String email = emailField.getText().toString();
        String password = passwordField.getText().toString();

        String emailError = AuthUtils.validateEmail(email);
        String passwordError = AuthUtils.validatePassword(password);

        if (emailError != null) {
            AuthUtils.setError(this, emailLabel, emailError);
            error = true;
        }

        if (passwordError != null) {
            AuthUtils.setError(this, passwordLabel, passwordError);
            error = true;
        }

        if (error) return;

        startWait();

        db.signIn(email, password, (task) -> {
            finishWait();
            if (task) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                AuthUtils.setError(this, emailLabel, "Invalid email or password");
                AuthUtils.setError(this, passwordLabel, "Invalid email or password");
            }
        });
    }

    private void onMoveToSignUpClicked(View view) {
        Intent intent = new Intent(this, SignUpActivity.class);
        intent.putExtra("email", emailField.getText().toString());
        intent.putExtra("password", passwordField.getText().toString());
        startActivity(intent);
        finish();
    }

    private void startWait() {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        signInButton.setEnabled(false);
        signInButton.setText("");
        signInButton.setBackgroundTintList(ColorStateList.valueOf(
                AuthUtils.getColorFromAttr(this, R.attr.primaryButtonPressedColor)));
    }

    private void finishWait() {
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        signInButton.setEnabled(true);
        signInButton.setText(R.string.sign_in);
        signInButton.setBackgroundTintList(ColorStateList.valueOf(
                AuthUtils.getColorFromAttr(this, R.attr.primaryButtonColor)));
    }
}