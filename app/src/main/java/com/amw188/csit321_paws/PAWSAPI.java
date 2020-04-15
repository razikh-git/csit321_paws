package com.amw188.csit321_paws;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

final class PAWSAPI {
    private PAWSAPI() {}

    private static final String TAG = PrefConstValues.tag_prefix + "api";

    private static double msToKilometresPerHour(final double ms) { return ms * 3.6d; }
    private static double millimetresToInches(final double mm) { return mm / 25.4d; }
    private static double kmToMiles(final double km) { return km * 0.62d; }
    private static double metersToFeet(final double m) { return m * 3.281; }

    static boolean preferredMetric(SharedPreferences sharedPref) {
        return sharedPref.getString(PrefKeys.units, PrefDefValues.units)
                .equals(PrefConstValues.units_metric);
    }

    private static boolean preferred24HourFormat(SharedPreferences sharedPref) {
        final String hourformat = sharedPref.getString(
                PrefKeys.hourformat, PrefDefValues.hourformat);
        Log.d(TAG, "Using " + hourformat + " hour time.");
        return hourformat.equals(PrefConstValues.hourformat_24);
    }

    /**
     * Formats a decimal value in metres to a short distance in kilometres, or converting to miles.
     * @param isMetric Whether to use metric or imperial units.
     * @param m Decimal value in metres.
     * @return Formatted distance string in km/mi.
     */
    static String getLongDistanceString(final boolean isMetric, final double m) {
    	final double km = m / 1000d;
    	return new DecimalFormat("#.##").format(isMetric ? km : kmToMiles(km))
				+ (isMetric ? "km" : "mi");
	}

	/**
	 * Formats a decimal value in metres to a short distance in metres, or converting to feet.
	 * @param isMetric Whether to use metric or imperial units.
	 * @param m Decimal value in metres.
	 * @return Formatted distance string in m/ft.
	 */
	static String getShortDistanceString(final boolean isMetric, final double m) {
    	return new DecimalFormat("#.#").format(isMetric ? m : metersToFeet(m))
			+ (isMetric ? "m" : "ft");
	}

	static Location getLastBestLocation(SharedPreferences sharedPref) {
        Location location = null;
        try {
            JSONObject lastBestLatLng = new JSONObject(sharedPref.getString(
                    PrefKeys.last_best_lat_lng, PrefConstValues.empty_json_object));
            if (lastBestLatLng.length() > 0) {
                location = new Location(LocationManager.GPS_PROVIDER);
                location.setLatitude(lastBestLatLng.getDouble("latitude"));
                location.setLongitude(lastBestLatLng.getDouble("longitude"));
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Failed to parse last best location.");
            ex.printStackTrace();
        }
        return location;
    }

    static Date parseWillyTimestamp(String timestamp) {
		try {
			final SimpleDateFormat inFormat = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss", Locale.getDefault());
			return inFormat.parse(timestamp);
		} catch (ParseException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	// todo: resolve 12-hour time not taking effect

    /**
     * Returns the index of the weather sample covering a given point in time.
     * @param weatherArray Array of weather samples.
     * @param when Target time.
     * @return Index of a 3-hour periodic sample covering the given time. -1 if not found.
     */
	static int getWeatherJsonIndexForTime(final JSONArray weatherArray, final long when) {
    	try {
    	    final int end = weatherArray.length();
			for (int whichTime = 0; whichTime < end; ++whichTime) {
				Log.d(TAG, String.format("when (%s), whichTime (%s = %s), end (%s = %s)",
						when,
						whichTime, weatherArray.getJSONObject(whichTime).getLong("dt") * 1000,
						end, weatherArray.getJSONObject(end - 1).getLong("dt") * 1000));
				if ((weatherArray.getJSONObject(whichTime).getLong("dt") * 1000)
						- (1000 * 60 * 60 * (24/8)) <= when) {
					final long whichTimeMs = weatherArray.getJSONObject(whichTime).getLong("dt") * 1000 - (1000 * 60 * 60 * (24/8));
					Log.d(TAG, String.format("Returning %s (which: %s - when: %s = difference: %s)",
							whichTime,
							whichTimeMs,
							when,
							whichTimeMs - when));
					return whichTime;
				}
			}
		} catch (JSONException ex) {
    		ex.printStackTrace();
		}
    	Log.e(TAG, String.format("Index for current time not found in weatherArray length=%s when=%s.",
				weatherArray.length(), when));
        return -1;
	}

    /**
     * Formats a time into an abbreviated 12-hour string for weather timestamps.
     * @param ms Raw timestamp in milliseconds.
     * @return Formatted weather timestamp string.
     */
	static String getShortClockString(final long ms) {
        return new SimpleDateFormat("h a", Locale.getDefault()).format(ms);
    }

    /**
     * Formats a time into some simple style for weather timestamps.
     * @param context Context.
     * @param ms Raw timestamp in milliseconds.
     * @param showMinutes Show or hide minutes in formatted time.
     * @return Formatted weather timestamp string.
     */
	static String getClockString(final Context context, final long ms, final boolean showMinutes) {
        final boolean is24hr = PAWSAPI.preferred24HourFormat(
        		PreferenceManager.getDefaultSharedPreferences(context));
        final String pattern = String.format(
                "%s%s%s",
                is24hr ? "HH" : "h",
                showMinutes ? ":mm" : "",
                is24hr ? "" : " a");
	    return new SimpleDateFormat(pattern, Locale.getDefault()).format(ms);
    }

    /**
     * Formats a time into some consistent style for detailed weather timestamps.
     * @param context Context.
     * @param ms Raw timestamp in milliseconds.
     * @return Formatted weather timestamp string.
     */
	static String getWeatherTimestampString(final Context context, final long ms) {
	    final boolean is24hr = PAWSAPI.preferred24HourFormat(
	    		PreferenceManager.getDefaultSharedPreferences(context));
	    final String pattern = String.format("dd/MM %s:mm %s",
                is24hr ? "HH" : "hh",
                is24hr ? "" : "a");
		return context.getString(R.string.home_weather_timestamp)
                + " " + new SimpleDateFormat(pattern, Locale.getDefault()).format(
                        ms - 1000 * 60 * 60 * (24 / 8));
	}

    /**
     * Formats a time into a detailed timestamp including year and date.
     * @param context Context.
     * @param ms Raw timestamp in milliseconds.
     * @param newline Whether to split time and date into separate lines.
     * @return Formatted timestamp string.
     */
	static String getDateTimestampString(final Context context, final long ms, boolean newline) {
	    final boolean is24hr = PAWSAPI.preferred24HourFormat(
	    		PreferenceManager.getDefaultSharedPreferences(context));
	    final String pattern = String.format(
	            "%s:mm %s%sdd/MM/yyyy",
                is24hr ? "HH" : "hh",
                is24hr ? "" : "a",
                newline ? '\n' : ' ');
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(ms);
    }

	/**
	 * Rounds a decimal to the nearest multiple of 10, for simplifying percentages.
	 * @param dbl Decimal number.
	 * @return Rounded integer.
	 */
	static int roundToTen(final double dbl) { return (int)(10 * Math.round(dbl / 10)); }

    /**
     * Formats a decimal value as a percentage rounded to the nearest 10% out of 100.
     * @param dbl Decimal value.
     * @return Formatted percentage string.
     */
	static String getSimplePercentageString(final double dbl) { return roundToTen(dbl) + "%"; }

    /**
     * Fetches the complete personality survey file as a JSON.
     * @param context Context.
     * @return Survey as a JSON object.
     */
	static JSONObject getSurveyJson(final Context context) {
        try {
            InputStream in = context.getResources().openRawResource(R.raw.paws_survey_json);
            byte[] b = new byte[in.available()];
            in.read(b);
            return new JSONObject(new String(b));
        } catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Reads the number of questions in the survey JSON.
     * @param context Context.
     * @return Question count.
     */
	static int getSurveyQuestionCount(final Context context) {
        try {
            return getSurveyJson(context).getJSONArray("questions").length();
        } catch (NullPointerException | JSONException ex) {
        	ex.printStackTrace();
            return -1;
        }
    }

    /**
     * Formats a string for some precipitation depth in appropriate units.
     * @param isMetric Value conversion and units to display for metric or non-metric preferences.
     * @param mm Value to show and/or convert.
     * @return Formatted string.
     */
    static String getPrecipitationString(final boolean isMetric, final double mm) {
            return isMetric
                ? new DecimalFormat("#.#").format(mm) + "mm"
                : new DecimalFormat("#.#").format(millimetresToInches(mm)) + "in";
    }

    /**
     * Formats a string for some wind speed in appropriate units.
     * @param isMetric Value conversion and units to display for metric or non-metric preferences.
     * @param speed Value to show and convert.
     * @return Formatted string.
     */
    static String getWindSpeedString(final double speed, final boolean isMetric) {
        return isMetric
                ? new DecimalFormat("#").format(msToKilometresPerHour(speed)) + " km/h"
                : new DecimalFormat("#").format(speed) + " mph";
    }

    /**
     * Fetches a cardinal direction for some angle in degrees.
     * @param bearing Wind bearing value in degrees.
     * @return Cardinal direction string.
     */
    static String getWindBearingString(final Context context, final double bearing) {
        int id = R.string.wa_north;
        if (bearing < 135)
            id = R.string.wa_west;
        else if (bearing < 225)
            id = R.string.wa_south;
        else if (bearing < 315)
            id = R.string.wa_east;
        return context.getString(id);
    }

    /**
     * Fetches a cardinal direction for some angle in degrees.
     * @param bearing Wind bearing value in degrees.
     * @param isVerbose Whether to return the full name or an abbreviation.
     * @return Cardinal direction string.
     */
    static String getWindBearingString(final Context context,
                                       final double bearing, final boolean isVerbose) {
        final String str = getWindBearingString(context, bearing);
        return isVerbose ? str : str.substring(0, 1).toUpperCase();
    }

    /**
     * Formats a string for some temperature without units.
     * @param temperature Value to show.
     * @return Formatted string.
     */
    static String getTemperatureString(final double temperature) {
        return new DecimalFormat("#").format(temperature) + "°";
    }

    /**
     * Formats a string for some temperature with appropriate units.
     * @param temperature Value to show.
     * @return Formatted string.
     */
    static String getTemperatureString(final double temperature, final boolean isMetric) {
        return new DecimalFormat("#").format(temperature) + (isMetric ? "°C" : "°F");
    }

    /**
     * Fetches the drawable weather icon ID appropriate to the given OpenWeatherMaps icon ID.
     * @param icon OWM icon ID.
     * @return Drawable weather icon ID.
     */
    static int getWeatherIconId(final String icon) {
        int id = -1;
        switch (icon) {
            case "01":
                id = R.drawable.w01;
                break;
            case "01d":
                id = R.drawable.w01d;
                break;
            case "01n":
                id = R.drawable.w01n;
                break;
            case "02d":
                id = R.drawable.w02d;
                break;
            case "02n":
                id = R.drawable.w02n;
                break;
            case "03d":
                id = R.drawable.w03d;
                break;
            case "03n":
                id = R.drawable.w03n;
                break;
            case "04d":
                id = R.drawable.w04d;
                break;
            case "04n":
                id = R.drawable.w04n;
                break;
            case "9d":
                id = R.drawable.w09d;
                break;
            case "09n":
                id = R.drawable.w09n;
                break;
            case "10d":
                id = R.drawable.w10d;
                break;
            case "10n":
                id = R.drawable.w10n;
                break;
            case "11d":
                id = R.drawable.w11d;
                break;
            case "11n":
                id = R.drawable.w11n;
                break;
            case "13d":
                id = R.drawable.w13d;
                break;
            case "13n":
                id = R.drawable.w13n;
                break;
            case "50d":
                id = R.drawable.w50d;
                break;
            case "50n":
                id = R.drawable.w50n;
                break;
        }
        return id;
    }

    /**
     * Fetches a bitmap image for a weather icon code provided by OpenWeatherMaps.
     * @param ctx Context.
     * @param icon OWM icon code.
     * @return Drawable for weather icon code.
     */
    static Bitmap getWeatherBitmap(final Context ctx, final String icon) {
        return BitmapFactory.decodeResource(ctx.getResources(),
                getWeatherIconId(icon));
    }

    /**
     * Fetches a drawable for a weather icon code provided by OpenWeatherMaps.
     * @param ctx Context.
     * @param icon OWM icon code.
     * @return Drawable for weather icon code.
     */
    static Drawable getWeatherDrawable(final Context ctx, final String icon) {
        return ctx.getDrawable(getWeatherIconId(icon));
    }

    /**
     * Value conversion rounding milliseconds down to decimal hours.
     * Replace with DateFormat.format("h") ?
     * @param milliseconds Duration to round down.
     * @return Decimal hour value.
     */
    static double msToHours(final long milliseconds) {
        final double hours = (double) milliseconds / 1000 / 60;
        return (hours / 60) % hours;
    }

    /**
     * Value conversion fetching minutes from an hour's decimal remainder.
     * Replace with DateFormat.format("m") ?
     * @param hour Hour with minutes as a floating point remainder.
     * @return Minutes extra to the hour.
     */
    static int minuteOfHour(final double hour)
    { return (int)((hour - (int)Math.floor(hour)) * 60); }

	/**
	 * Gives the time in milliseconds, to the nearest minute, from some starting time
	 * until the given hour and minute on a 24 hour clock.
	 * @param from Starting instance in time.
	 * @param hour Hour of the day.
	 * @param minute Minute of the hour.
	 * @return Milliseconds between starting time and today's instance of this hour and minute.
	 */
	static long getTimeUntil(final long from, final int hour, final int minute) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(from);
		return (hour - calendar.get(Calendar.HOUR_OF_DAY)) * 1000 * 60 * 60
				+ (minute - calendar.get(Calendar.MINUTE)) * 1000 * 60;
	}

	/**
	 * Gives the time in milliseconds, to the nearest minute, from some starting time
	 * until the next given hour and minute on a 24 hour clock.
	 * @param from Starting instance in time.
	 * @param hour Hour of the day.
	 * @param minute Minute of the hour.
	 * @return Milliseconds between starting time and the next instance of this hour and minute.
	 */
	static long getTimeUntilNext(final long from, final int hour, final int minute) {
		long time = getTimeUntil(from, hour, minute);
		return time > 1 ? time : time + 1000 * 60 * 60 * 24;
	}

	/**
     * Compiles the temperature values from a range of weather samples between some starting time
     * until the next day begins at midnight.
     * @param startTime Starting instance in time.
     * @param weatherJSON JSON array containing a list of weather samples to iterate through.
     * @return ArrayList containing all temperature samples for the day.
     */
    static ArrayList<Double> getDailyTemperatures(final long startTime,
                                                  final JSONArray weatherJSON) {
        Log.d(TAG, "Fetching new daily temperatures.");
        ArrayList<Double> tempList = new ArrayList<>();
        try {
            // Fetches the temperatures at each 3hr interval until the next day
            for (int i = 0; i < weatherJSON.length(); ++i) {
                final long sampleTime = weatherJSON.getJSONObject(i).getLong("dt") * 1000;
                if (sampleTime >= startTime) {
                    Log.d(TAG, "Fetching...");
                    int elemsPerDay = (int)Math.floor((24 - msToHours(startTime)) / 3);
                    if (elemsPerDay < 1)
                        elemsPerDay = 24 / 3;
                    final int end = Math.min(weatherJSON.length(), i + elemsPerDay);
                    for (int j = i; j < end; ++j)
                        tempList.add(weatherJSON.getJSONObject(j)
                                .getJSONObject("main")
                                .getDouble("temp"));
                    break;
                }
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Failed to read weather JSON.");
            ex.printStackTrace();
        }
        return tempList;
    }

    static String getLatLngJsonObjectString(final double latitude, final double longitude) {
		return String.format(
				"{\"latitude\":\"%s\",\"longitude\":\"%s\"}",
				latitude, longitude);
	}

	static int getPlaceIndexInHistory(final JSONArray historyJson, final String title) {
		int index = -1;
		try {
			for (int i = 0; i < historyJson.length(); ++i) {
				if (historyJson.getJSONObject(i).getString("title").equals(title)) {
					index = i;
					break;
				}
			}
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
		return index;
	}

	static JSONObject getMostVisitedPlaceFromHistory(final Context context) {
		JSONObject locationJson = null;
		try {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			final JSONArray historyJson = new JSONArray(sharedPref.getString(
					PrefKeys.position_history, PrefConstValues.empty_json_array));
			locationJson = historyJson.getJSONObject(0);
			for (int i = 0; i < historyJson.length(); ++i) {
				if (historyJson.getJSONObject(i).getInt("weight")
						> locationJson.getInt("weight"))
					locationJson = historyJson.getJSONObject(i);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return locationJson;
	}

    static boolean addPlaceToHistory(final Context context,
									 final ArrayList<Address> addressResults) {
		try {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			SharedPreferences.Editor sharedEditor = sharedPref.edit();

			JSONArray historyJSON = new JSONArray(sharedPref.getString(
					PrefKeys.position_history, PrefConstValues.empty_json_array));

			final Address address = addressResults.get(0);
			final String title = AddressHandler.getBestAddressTitle(address);
			final int index = getPlaceIndexInHistory(historyJSON,
					title);
			if (index >= 0) {
				// Positions with a match in the device history add to their weighting,
				// reflecting either their amount of visits or the duration spent there
				int weight = 1;
				if (historyJSON.getJSONObject(index).has("weight"))
					weight += historyJSON.getJSONObject(index).getInt("weight");
				historyJSON.getJSONObject(index).put("weight", weight);
				Log.d(TAG, "Matched coordinates: "
						+ "Location revisited, weighted at " + weight
						+ ", favourited: " + historyJSON.getJSONObject(index).get("favorite"));
				sharedEditor.putString(PrefKeys.position_history, historyJSON.toString());
				sharedEditor.apply();
			} else {
				// Ignored positions are not added again
				JSONArray ignoredJSON = new JSONArray(sharedPref.getString(
						PrefKeys.position_ignored, PrefConstValues.empty_json_array));
				for (int i = 0; i < ignoredJSON.length(); ++i) {
					if (ignoredJSON.getString(i).equals(title)) {

						Log.d(TAG, "Revisited blocked location (" + title + "), ignoring.");

						return false;
					}
				}

				// New positions are added to the device history:

				// Add new element
				final int i = historyJSON.length();
				historyJSON.put(i, new JSONObject());

				Log.d(TAG, "Adding newly visited location to element " + i + ".");

				// Add values
				historyJSON.getJSONObject(i).put("title",
						title);
				historyJSON.getJSONObject(i).put("subtitle",
						String.format("%s %s",
								AddressHandler.getAustralianStateCode(address),
								address.getPostalCode()));
				historyJSON.getJSONObject(i).put("lat_lng",
						new JSONObject(getLatLngJsonObjectString(
								address.getLatitude(), address.getLongitude())));
				historyJSON.getJSONObject(i).put("favorite",
						false);
				historyJSON.getJSONObject(i).put("weight",
						1);

				// Apply changes to live JSON in preferences
				sharedEditor.putString(PrefKeys.position_history, historyJSON.toString());
				sharedEditor.apply();
				Log.d(TAG, "Added: " + historyJSON.getJSONObject(i).toString());
			}
			return true;
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
		return false;
	}

	/**
	 * Attempts to remove a place from history by title match, if found.
	 * @param context Context.
	 * @param title Best available place title.
	 * @param ignored Whether to add the place title to the place ignore list,
	 *                   preventing it from being re-added to history.
	 * @return Whether the place was found and successfully removed.
	 */
	static boolean removePlaceFromHistory(final Context context,
										  final String title, final boolean ignored) {
		try {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			SharedPreferences.Editor sharedEditor = sharedPref.edit();

			JSONArray historyJSON = new JSONArray(sharedPref.getString(
				 PrefKeys.position_history, PrefConstValues.empty_json_array));

			for (int i = 0; i < historyJSON.length(); ++i) {
				if (historyJSON.getJSONObject(i).getString("title").equals(title)) {

					// Attempt to block ignored places
					boolean found = false;
					JSONArray ignoredJSON = new JSONArray(sharedPref.getString(
							PrefKeys.position_ignored, PrefConstValues.empty_json_array));
					if (ignored) {
						for (int j = 0; j < ignoredJSON.length(); ++j) {
							if (ignoredJSON.getString(i).equals(title)) {
								found = true;
								break;
							}
						}
						if (!found)
							ignoredJSON.put(title);
					}

					historyJSON.remove(i);
					sharedEditor.putString(PrefKeys.position_history, historyJSON.toString());
					sharedEditor.putString(PrefKeys.position_ignored, ignoredJSON.toString());
					sharedEditor.apply();

					Toast.makeText(context,
							String.format("%s %s from your places.",
									ignored ? "Blocked" : "Removed",
									title),
							Toast.LENGTH_LONG).show();

					return true;
				}
			}

		} catch (JSONException ex) {
			ex.printStackTrace();
		}
		return false;
	}

    /**
     * Wipes all data created by the app, including survey and personalisation data.
     * @param context Context.
     */
    static void resetAppData(final Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();
        sharedEditor.putBoolean(PrefKeys.app_init, false);

        // Notifications
        sharedEditor.putString(PrefKeys.weather_notif_time_start,
                PrefDefValues.weather_notif_time_start);
        sharedEditor.putString(PrefKeys.weather_notif_time_end,
                PrefDefValues.weather_notif_time_end);
        sharedEditor.putString(PrefKeys.weather_notif_interval,
                PrefDefValues.weather_notif_interval);
		// Locations
		PAWSAPI.resetLocationData(context);
        // Survey
        PAWSAPI.resetProfileData(context);

        sharedEditor.apply();

        Toast.makeText(context,
                R.string.app_reset_survey,
                Toast.LENGTH_LONG).show();
    }

    static void resetLocationData(final Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor sharedEditor = sharedPref.edit();

		// Clear last location and full location history
		sharedEditor.putString(PrefKeys.last_best_lat_lng,
				PrefConstValues.empty_json_object);
		sharedEditor.putString(PrefKeys.position_history,
				PrefConstValues.empty_json_array);
		
		sharedEditor.apply();
		
		Toast.makeText(context,
				R.string.app_reset_location,
				Toast.LENGTH_LONG).show();
	}

    /**
     * Wipes all data related to the personalisation features and personality survey.
     * @param context Context.
     */
    static boolean resetProfileData(final Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();

        // Reset all survey profile data
        final int end = getSurveyQuestionCount(context);
        for (int i = 0; i < end; ++i)
            sharedEditor.putInt(PrefKeys.survey_answer_ + i, 0);
        sharedEditor.putInt(PrefKeys.survey_last_question, 0);
        sharedEditor.putLong(PrefKeys.survey_time_completed, 0);
        sharedEditor.apply();

        Toast.makeText(context,
                R.string.app_reset_survey,
                Toast.LENGTH_LONG).show();

        return true;
    }
}
