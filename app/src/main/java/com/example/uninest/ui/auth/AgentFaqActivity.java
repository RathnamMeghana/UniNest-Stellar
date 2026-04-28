package com.example.uninest.ui.auth;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.uninest.R;

public class AgentFaqActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agent_faq);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
