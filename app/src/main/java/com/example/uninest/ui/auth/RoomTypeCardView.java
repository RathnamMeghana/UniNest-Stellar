package com.example.uninest.ui.auth;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.uninest.R;

public class RoomTypeCardView extends FrameLayout {

    private TextView tvRoomTypeName;
    private ImageView imgChevron;


    public RoomTypeCardView(Context context) {
        super(context);
        init(context);
    }

    public RoomTypeCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RoomTypeCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_room_type_card, this, true);

        tvRoomTypeName = findViewById(R.id.tvRoomTypeName);
        imgChevron = findViewById(R.id.imgChevron);
    }

    public void setRoomTypeName(String name) {
        tvRoomTypeName.setText(name);
    }

    public String getRoomTypeName() {
        return tvRoomTypeName.getText().toString();
    }
}
