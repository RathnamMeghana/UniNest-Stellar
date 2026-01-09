package com.example.uninest;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "user_session";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_HOUSE_CODE = "house_code";

    SharedPreferences pref;
    SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // Save login session
    public void saveUserSession(String email, String role, String houseCode) {
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_HOUSE_CODE, houseCode);
        editor.apply();
    }

    public void saveUserSession(String email, String role) {
        saveUserSession(email, role, "");
    }

    // Get user email
    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }

    // Get user role
    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "-1"); // -1 means not logged in
    }

    public String fetchHouseCode() {
        return pref.getString(KEY_HOUSE_CODE, null);
    }

    // Clear session
    public void logout() {
        editor.clear();
        editor.apply();
    }
}
