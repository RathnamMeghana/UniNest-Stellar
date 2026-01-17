package com.example.uninest.model;

import java.util.Date;

public class DateUtils {

    public static long toSeconds(Date date) {
        return date.getTime() / 1000;
    }
}
