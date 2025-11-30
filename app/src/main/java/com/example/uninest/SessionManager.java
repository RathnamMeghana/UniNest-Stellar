package com.example.uninest;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "user_session";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";

    SharedPreferences pref;
    SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // Save login session
    public void saveUserSession(String email, String role) {
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    // Get user email
    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }

    // Get user role
    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "-1"); // -1 means not logged in
    }

    // Clear session
    public void logout() {
        editor.clear();
        editor.apply();
    }
}
