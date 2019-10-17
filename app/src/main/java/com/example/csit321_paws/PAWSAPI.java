package com.example.csit321_paws;

import android.content.Context;
import android.graphics.drawable.Drawable;
final class PAWSAPI {
    private PAWSAPI() {}

    static final Double MS_TO_KMH = 3.6d;
    static final Double MS_TO_MPH = 2.237d;

    static Drawable getWeatherDrawable(Context ctx, String icon) {
        switch (icon) {
            case "01":
                return ctx.getDrawable(R.drawable.w01);
            case "01d":
                return ctx.getDrawable(R.drawable.w01d);
            case "01n":
                return ctx.getDrawable(R.drawable.w01n);
            case "02d":
                return ctx.getDrawable(R.drawable.w02d);
            case "02n":
                return ctx.getDrawable(R.drawable.w02n);
            case "03d":
                return ctx.getDrawable(R.drawable.w03d);
            case "03n":
                return ctx.getDrawable(R.drawable.w03n);
            case "04d":
                return ctx.getDrawable(R.drawable.w04d);
            case "04n":
                return ctx.getDrawable(R.drawable.w04n);
            case "9d":
                return ctx.getDrawable(R.drawable.w09d);
            case "09n":
                return ctx.getDrawable(R.drawable.w09n);
            case "10d":
                return ctx.getDrawable(R.drawable.w10d);
            case "10n":
                return ctx.getDrawable(R.drawable.w10n);
            case "11d":
                return ctx.getDrawable(R.drawable.w11d);
            case "11n":
                return ctx.getDrawable(R.drawable.w11n);
            case "13d":
                return ctx.getDrawable(R.drawable.w13d);
            case "13n":
                return ctx.getDrawable(R.drawable.w13n);
            case "50d":
                return ctx.getDrawable(R.drawable.w50d);
            case "50n":
                return ctx.getDrawable(R.drawable.w50n);
            default:
                return null;
        }
    }
}
