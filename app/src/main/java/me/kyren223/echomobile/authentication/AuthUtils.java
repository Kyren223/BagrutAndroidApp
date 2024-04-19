package me.kyren223.echomobile.authentication;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.kyren223.echomobile.R;

public class AuthUtils {

    public static final int MAX_LENGTH = 32;

    public static void setError(Activity activity, TextView textView, String error) {
        clearError(activity, textView);
        textView.setTextColor(getColorFromAttr(activity, R.attr.errorColor));

        CharSequence text = textView.getText() + " - " + error;
        SpannableString spannableString = new SpannableString(text);
        int start = spannableString.toString().indexOf(error);
        int end = start + error.length();
        spannableString.setSpan(new AbsoluteSizeSpan(48), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getColorFromAttr(activity, R.attr.errorColor)), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannableString);
    }

    public static void clearError(Activity activity, TextView textView) {
        textView.setTextColor(getColorFromAttr(activity, R.attr.primaryTextColor));
        if (textView.getId() == R.id.emailLabel) textView.setText(R.string.email);
        else if (textView.getId() == R.id.passwordLabel) textView.setText(R.string.password);
        else if (textView.getId() == R.id.displayNameLabel) textView.setText(R.string.display_name);
    }

    public static int getColorFromAttr(Activity activity, int attr) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = activity.getTheme();
        theme.resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    public static String validateEmail(String email) {
        if (email == null || email.isEmpty()) return "Required";
        if (!email.contains("@")) return "Missing @";
        if (!email.contains(".")) return "Missing .";

        if (email.length() > MAX_LENGTH) return "Too long";
        if (email.length() < 8) return "Too short";

        return null;
    }

    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) return "Required";

        Pattern uppercasePattern = Pattern.compile("[A-Z]");
        Pattern lowercasePattern = Pattern.compile("[a-z]");
        Pattern digitPattern = Pattern.compile("[0-9]");
        Pattern specialCharPattern = Pattern.compile("[!@#$%^&*]");

        if (!containsPattern(password, uppercasePattern)) return "Missing uppercase letter";
        if (!containsPattern(password, lowercasePattern)) return "Missing lowercase letter";
        if (!containsPattern(password, digitPattern)) return "Missing number";
        if (!containsPattern(password, specialCharPattern)) return "Missing special character";

        Pattern invalidCharPattern = Pattern.compile("[^A-Za-z0-9!@#$%^&*]");
        Matcher invalidCharMatcher = invalidCharPattern.matcher(password);
        if (invalidCharMatcher.find()) return "Cannot contain: " + invalidCharMatcher.group();

        if (password.length() > MAX_LENGTH) return "Too long";
        if (password.length() < 8) return "Too short";

        return null;
    }

    public static String validateDisplayName(String displayName) {
        if (displayName == null || displayName.isEmpty()) return "Required";
        if (displayName.length() > MAX_LENGTH) return "Too long";
        return null;
    }

    private static boolean containsPattern(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find();
    }


    private static boolean passwordVisible = false;
    public static void togglePasswordVisibility(Activity activity, EditText passwordField) {
        passwordVisible = !passwordVisible;
        Drawable[] drawables = passwordField.getCompoundDrawablesRelative();
        if (passwordVisible) {
            passwordField.setTransformationMethod(new HideReturnsTransformationMethod());
            drawables[2] = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.ic_visibility, activity.getTheme());
        } else {
            passwordField.setTransformationMethod(new PasswordTransformationMethod());
            drawables[2] = ResourcesCompat.getDrawable(activity.getResources(), R.drawable.ic_visibility_off, activity.getTheme());
        }
        passwordField.setTypeface(Typeface.DEFAULT);
        passwordField.setCompoundDrawablesRelativeWithIntrinsicBounds(drawables[0], drawables[1], drawables[2], drawables[3]);
    }
}
