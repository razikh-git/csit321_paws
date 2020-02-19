package com.amw188.csit321_paws;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

class DailyWeatherWorker extends Worker {
    public DailyWeatherWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {



        return Result.success();
    }
}
