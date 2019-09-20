package com.example.csit321_paws;

import android.location.Location;

import androidx.annotation.Nullable;

public class PAWSLocation extends Location {

    public String mLocName;
    public Long mTimeStamp;

    public PAWSLocation(Location loc, String locName, Long timeStamp) {
        super(loc);
        mLocName = locName;
        mTimeStamp = timeStamp;
    }

    public void set(Location loc, String locName, Long timeStamp) {
        set(loc);
        mLocName = locName;
        mTimeStamp = timeStamp;
    }

    public String name() {
        return mLocName;
    }

    public Long timestamp() {
        return mTimeStamp;
    }
}
