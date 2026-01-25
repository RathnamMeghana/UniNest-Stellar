package com.example.uninest;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "user_session";
    private static final String KEY_USER_ID = "user_id"; // New: Store Firestore Document ID
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_HOUSE_CODE = "house_code";
    private static final String KEY_USER_NAME = "user_full_name"; // New: Store "John Doe"

    SharedPreferences pref;
    SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // Save Tenant Session (With Name and House Code)
    public void saveTenantSession(String userId, String email, String role, String houseCode, String fullName) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_HOUSE_CODE, houseCode);
        editor.putString(KEY_USER_NAME, fullName);
        editor.apply();
    }

    // Save Agent Session (Might not have house code/name in the same way)
    public void saveAgentSession(String email, String role) {
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }

    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "-1");
    }

    public String fetchHouseCode() {
        return pref.getString(KEY_HOUSE_CODE, null);
    }

    public String getUserFullName() {
        return pref.getString(KEY_USER_NAME, "Me");
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}