package com.amw188.csit321_paws;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.renderer.CombinedChartRenderer;
import com.github.mikephil.charting.renderer.DataRenderer;
import com.github.mikephil.charting.renderer.LineChartRenderer;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class PlaceInfoActivity
        extends BottomNavBarActivity
        implements
        AddressHandler.AddressReceivedListener,
        WeatherHandler.WeatherReceivedListener,
        Preference.OnPreferenceChangeListener
{
    private static final String TAG = PrefConstValues.tag_prefix + "a_inf";

    private SharedPreferences mSharedPref;

    private String mNearbyPlace;
    private LatLng mPlaceLatLng;

    // todo: add uncertainty to precipitation measures to avoid 0.2mm showing as 'light rain'

    // todo: relocate weather calls to the foreground service

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_info);

        if (!init(savedInstanceState))
            Log.e(TAG, "Failed to initialise weather for place info.");
    }

    private boolean init(Bundle savedInstanceState) {
        if (initActivity() && initClickables()) {
            return initWeatherData(getTargetPositionFromExtras(savedInstanceState));
        }
        return false;
    }

    private boolean initClickables() {
        findViewById(R.id.layPlaceInfoHeader).setOnClickListener(this::redirectToMapsActivity);
        return true;
    }

    private boolean initActivity() {
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        return true;
    }

    private LatLng getTargetPositionFromExtras(Bundle savedInstanceState) {
        LatLng latLng = null;
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                latLng = extras.getParcelable(RequestCodes.EXTRA_LATLNG);
                mNearbyPlace = extras.getString(RequestCodes.EXTRA_PLACENAME);
            }
        }
        return latLng;
    }

    private boolean initWeatherData(LatLng latLng) {
        try {
            if (latLng == null) {
                JSONObject lastWeather = new JSONObject(
                        mSharedPref.getString(PrefKeys.last_weather_json,
                                PrefConstValues.empty_json_object));
                latLng = new LatLng(lastWeather.getJSONObject("city").getJSONObject("coord")
                        .getDouble("lat"),
                        lastWeather.getJSONObject("city").getJSONObject("coord")
                                .getDouble("lon"));
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }

        if (latLng == null)
            return false;

        // Fetch the address to try and add this place to history and fetch tidal data
        new AddressHandler(this, this).awaitAddress(latLng);

        // Call and await an update to the weather JSON string in shared prefs
        if (!new OpenWeatherHandler(this, this).awaitWeatherUpdate(latLng)) {
            // Initialise weather displays with last best values if none are being updated
			Log.d(TAG, "initWeatherDisplay from initWeatherData using lastWeatherJSON");
            return initWeatherDisplay(mSharedPref.getString(
                    PrefKeys.last_weather_json, PrefConstValues.empty_json_object));
        }
        return true;
    }

    /**
     * Open up MapsActivity focusing on the current place.
     */
    private void redirectToMapsActivity(View view) {
        Intent intent = new Intent(this, MapsActivity.class)
                .putExtra(RequestCodes.EXTRA_LATLNG, mPlaceLatLng);
        startActivity(intent);
    }

    @Override
    public void onAddressReceived(ArrayList<Address> addressResults) {
        PAWSAPI.addPlaceToHistory(this, addressResults);

        // Call and await an update to tidal info from WillyWeather
        new WillyWeatherHandler(this, this).awaitWeatherUpdate(
                WeatherHandler.REQUEST_WILLY_FORECAST, addressResults.get(0),
				"?days=2&forecasts=tides,swell");
    }

    @Override
    public void onWeatherReceived(int requestCode, String response) {
        try {
            if (requestCode == WeatherHandler.REQUEST_WILLY_FORECAST) {
                JSONObject forecastJSON = new JSONObject(response).getJSONObject("forecasts");
                if (forecastJSON.has("tides") && forecastJSON.has("swell"))
                    initWeatherCharts(new JSONObject(response));
                else
                    findViewById(R.id.layChart).setVisibility(View.GONE);
            } else if (requestCode == WeatherHandler.REQUEST_OPEN_WEATHER) {
				Log.d(TAG, "initWeatherDisplay from onWeatherReceived using response");
                initWeatherDisplay(response);
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "Preference " + preference.getKey() + " changing to " + newValue);
        return true;
    }

    private List<String> getAxisLabelValues(final JSONObject forecastJSON) {
        List<String> axisLabelValues = new ArrayList<>();
        try {
            JSONObject tidesJSON = forecastJSON.getJSONObject("forecasts").getJSONObject("tides");
            final int count = tidesJSON.getJSONArray("days").getJSONObject(0)
                    .getJSONArray("entries").length();
            for (int i = 0; i < count; ++i) {
                final JSONObject entry = tidesJSON.getJSONArray("days").getJSONObject(0)
                        .getJSONArray("entries").getJSONObject(i);
                final Date when = PAWSAPI.parseWillyTimestamp(entry.getString("dateTime"));
                if (when == null)
                    return null;
                axisLabelValues.add(PAWSAPI.getClockString(this, when.getTime(), false));
            }
            // Data continues to next day's first period
			axisLabelValues.add(axisLabelValues.get(0));
        } catch (JSONException ex) {
            ex.printStackTrace();
            return null;
        }

        //Log.d(TAG, "axisLabelValues: " + axisLabelValues.toString());

        return axisLabelValues;
    }

    private LineDataSet getTidalDataSet(final JSONObject forecastJSON) {
        List<Entry> entries = new ArrayList<>();
        try {
            JSONObject tidesJSON = forecastJSON.getJSONObject("forecasts").getJSONObject("tides");

            // Populate data for chart
            final int count = tidesJSON.getJSONArray("days").getJSONObject(0)
                    .getJSONArray("entries").length();
            for (int i = 0; i < count; ++i) {
                final JSONObject entry = tidesJSON.getJSONArray("days").getJSONObject(0)
                        .getJSONArray("entries").getJSONObject(i);
                entries.add(new Entry((float)i, (float)entry.getDouble("height")));
            }

            // Add a final entry to bring the data set up to the next morning
            final JSONObject entry = tidesJSON.getJSONArray("days").getJSONObject(1)
                    .getJSONArray("entries").getJSONObject(0);
            entries.add(new Entry((float) count, (float) entry.getDouble("height")));
        } catch (JSONException ex) {
            ex.printStackTrace();
            return null;
        }
        return new LineDataSet(entries, getString(R.string.wa_chart_tides_label));
    }

    private LineDataSet getSwellDataSet(final JSONObject forecastJSON) {
        List<Entry> entries = new ArrayList<>();
        try {
            JSONObject swellJSON = forecastJSON.getJSONObject("forecasts").getJSONObject("swell");

            int tideIndex = 0;

            // todo: resolve this data set having conflicting sample count with tidal data set

            // Populate data for chart
            final int count = swellJSON.getJSONArray("days").getJSONObject(0)
                    .getJSONArray("entries").length();
            for (int i = 0; i < count; ++i) {
                final JSONObject entry = swellJSON.getJSONArray("days").getJSONObject(0)
                        .getJSONArray("entries").getJSONObject(i);
                entries.add(new Entry((float)i, (float)entry.getDouble("height")));
            }

            final Date whenCompar = PAWSAPI.parseWillyTimestamp(forecastJSON.getJSONObject("forecasts")
                    .getJSONObject("tides")
                    .getJSONArray("days").getJSONObject(1)
                    .getJSONArray("entries").getJSONObject(0)
                    .getString("dateTime"));
            final String clockCompar = PAWSAPI.getClockString(this, whenCompar.getTime(), false);

            // Add enough entries from the next day to complete the chart, matching tide data window
            for (int i = 0; i < count; ++i) {
                JSONObject entry = swellJSON.getJSONArray("days").getJSONObject(1)
                        .getJSONArray("entries").getJSONObject(i);
                entries.add(new Entry((float) i, (float) entry.getDouble("height")));

                final Date when = PAWSAPI.parseWillyTimestamp(entry.getString("dateTime"));
                final String clock = PAWSAPI.getClockString(this, when.getTime(), false);

                if (clock.equals(clockCompar)) {
                    break;
                }
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
            return null;
        }
        return new LineDataSet(entries, getString(R.string.wa_chart_swell_label));
    }

	static class CustomLineChartRenderer extends LineChartRenderer {
		CustomLineChartRenderer(
				LineDataProvider chart, ChartAnimator animator, ViewPortHandler viewPortHandler) {
			super(chart, animator, viewPortHandler);
		}

		@Override
		public void drawValue(Canvas c, String valueText, float x, float y, int color) {
			mValuePaint.setColor(color);

			//final int index =
			//float value = -1f;
			//if (SwellDirections.containsKey(entry) && SwellDirections.get(entry) != null)
			//	value = SwellDirections.get(entry);

			//Log.d(TAG, String.format("Value of (%s) (color: %s) x%s y%s = %s", valueText, color, x, y, value));

			c.save();
			//if (value != -1f)
			//	c.rotate(value, x, y);

			c.drawText(valueText, x, y, mValuePaint);
			//c.drawText("⬆", x, y, mValuePaint);
			c.restore();
		}
	}

	public static class CustomBarChartRenderer extends BarChartRenderer {

		private List<Float> SwellDirections = new ArrayList<>();
		private int Index;

		CustomBarChartRenderer(
				BarDataProvider chart, ChartAnimator animator, ViewPortHandler viewPortHandler,
				List<Float> swellDirections) {
			super(chart, animator, viewPortHandler);
			SwellDirections = swellDirections;
			Index = 0;
		}

		@Override
		public void drawValue(Canvas c, String valueText, float x, float y, int color) {
			mValuePaint.setColor(color);

			float value = SwellDirections.get(Index);
			//Log.d(TAG, String.format("Value of (%s) (index: %s) x%s y%s = %s", valueText, Index, x, y, value));

			c.save();
			c.rotate(value, x, y);

			//c.drawText(valueText, x, y, mValuePaint);
			//c.drawText("⬆", x, y, mValuePaint);
			c.drawText("\uD83D\uDD3C", x, y, mValuePaint);
			c.restore();

			Index = ++Index % SwellDirections.size();
		}
	}

    private LineDataSet applyStyleToDataSet(LineDataSet dataSet) {
        final int fillAlpha = 80;
        final float lineRigidity = 0.2f;
        final float lineWidth = 3f;
        final float circleRadius = 7f;
        final float circleHoleRadius = circleRadius * 0.25f;

        dataSet.setLineWidth(lineWidth);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(fillAlpha);
        dataSet.setCircleRadius(circleRadius);
        dataSet.setCircleHoleRadius(circleHoleRadius);
        dataSet.setDrawValues(true);

        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(lineRigidity);

        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);

        return dataSet;
    }

    private boolean initWeatherCharts(final JSONObject forecastJSON) {
        final boolean isMetric = PAWSAPI.preferredMetric(mSharedPref);

        LinearLayout parentLayout = findViewById(R.id.layChart);
        parentLayout.setVisibility(View.VISIBLE);
        CombinedChart chart = findViewById(R.id.chartTidesSwell);

        // Parse data from forecast JSON
        //List<ILineDataSet> dataSetList = new ArrayList<>();
        LineDataSet tideDataSet
                //= getTidalDataSet(forecastJSON)
                ;
        BarDataSet swellDataSet
                //= getSwellDataSet(forecastJSON)
                ;

        // todo: return solution to method
        Random random = new Random();
        List<Entry> tideEntries = new ArrayList<>();
        List<BarEntry> swellEntries = new ArrayList<>();
		List<Float> swellDirections = new ArrayList<>();
        try {
			JSONObject swellJSON = forecastJSON.getJSONObject("forecasts").getJSONObject("swell");
            JSONObject tidesJSON = forecastJSON.getJSONObject("forecasts").getJSONObject("tides");

            // Populate data for chart
			final int swellCount = swellJSON.getJSONArray("days").getJSONObject(0)
					.getJSONArray("entries").length();
            final int tidesCount = tidesJSON.getJSONArray("days").getJSONObject(0)
                    .getJSONArray("entries").length();

            //Log.d(TAG, String.format("tidesCount: %s swellCount: %s", tidesCount, swellCount));

            for (int i = 0; i < tidesCount; ++i) {
                // Populate each data set to match their entry counts
				final int swellIndex = i * swellCount / tidesCount;
				final JSONObject tideEntryJSON = tidesJSON.getJSONArray("days").getJSONObject(0)
						.getJSONArray("entries").getJSONObject(i);
				final JSONObject swellEntryJSON = swellJSON.getJSONArray("days").getJSONObject(0)
						.getJSONArray("entries").getJSONObject(swellIndex);
				final Entry tideEntry = new Entry(
						(float) i,
						(float) tideEntryJSON.getDouble("height"));
				final BarEntry swellEntry = new BarEntry(
						(float) i,
						(float) swellEntryJSON.getDouble("height"));
				tideEntries.add(tideEntry);
				swellEntries.add(swellEntry);
				swellDirections.add((float) swellEntryJSON.getDouble("direction"));
				//Log.d(TAG, String.format("Adding entry to tides/swell: i=%s swellIndex=%s tide=%s swell=%s", i, swellIndex, tideEntry.getY(), swellEntry.getY()));
            }

            // Add a final entry to bring the data set up to the next morning
            final JSONObject tideEntryJSON = tidesJSON.getJSONArray("days").getJSONObject(1)
                    .getJSONArray("entries").getJSONObject(0);
            final JSONObject swellEntryJSON = swellJSON.getJSONArray("days").getJSONObject(0)
                    .getJSONArray("entries").getJSONObject(0);
            final Entry tideEntry = new Entry((float) tidesCount, (float) tideEntryJSON.getDouble("height"));
			final BarEntry swellEntry = new BarEntry((float) tidesCount, (float) swellEntryJSON.getDouble("height"));
			final float value = (float) swellEntryJSON.getDouble("direction");
			tideEntries.add(tideEntry);
            swellEntries.add(swellEntry);
            swellDirections.add(value);
			//Log.d(TAG, String.format("Adding entry to tides/swell: i=%s swellIndex=%s tide=%s swell=%s", 0, 0, tideEntry.getY(), swellEntry.getY()));
        } catch (JSONException ex) {
            ex.printStackTrace();
            //return null;
        }
        //return new LineDataSet(entries, getString(R.string.wa_chart_tides_label));
        tideDataSet = new LineDataSet(tideEntries, getString(R.string.wa_chart_tides_label));
        swellDataSet = new BarDataSet(swellEntries, getString(R.string.wa_chart_swell_label));
        // todo: return solution to method

        // Style data
		final float barValueSize = 20f;

        tideDataSet = applyStyleToDataSet(tideDataSet);
       	//swellDataSet = applyStyleToDataSet(swellDataSet);

        swellDataSet.setDrawIcons(true);

        tideDataSet.setColor(ContextCompat.getColor(this, R.color.color_accent_alt));
        tideDataSet.setFillColor(ContextCompat.getColor(this, R.color.color_primary));
        tideDataSet.setCircleColor(ContextCompat.getColor(this, R.color.color_accent_alt));

        swellDataSet.setColor(ContextCompat.getColor(this, R.color.color_error));
        swellDataSet.setValueTextSize(barValueSize);
        //swellDataSet.setFillColor(ContextCompat.getColor(this, R.color.color_error));
        //swellDataSet.setCircleColor(ContextCompat.getColor(this, R.color.color_error));

        // Add both data sets as lines to the chart
        //dataSetList.add(tideDataSet);
        //dataSetList.add(swellDataSet);
        //LineData lineData = new LineData(dataSetList);
        LineData lineData = new LineData(tideDataSet);
        lineData.setDrawValues(false);
        BarData barData = new BarData(swellDataSet);
        CombinedData combinedData = new CombinedData();
        combinedData.setData(lineData);
        combinedData.setData(barData);
		combinedData.setHighlightEnabled(false);
        chart.setData(combinedData);
        chart.setNoDataText(getString(R.string.wa_chart_no_data));

        List<String> axisLabelValues = getAxisLabelValues(forecastJSON);
		ValueFormatter formatterX = new ValueFormatter() {
			@Override
			public String getAxisLabel(float value, AxisBase axis) {
				return axisLabelValues.get((int) value);
			}
		};
        ValueFormatter formatterY = new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return PAWSAPI.getShortDistanceString(isMetric, value);
            }
        };

        // Style chart axes
        final int textColor = R.color.color_accent;
        final float labelTextSize = 14f;

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(labelTextSize);
        xAxis.setTextColor(textColor);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(formatterX);

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setTextSize(labelTextSize);
        xAxis.setTextColor(textColor);
        yAxis.setDrawGridLines(false);
        yAxis.setValueFormatter(formatterY);

        chart.getAxisRight().setEnabled(false);

		CombinedChartRenderer combinedRenderer = new CombinedChartRenderer(
				chart, chart.getAnimator(), chart.getViewPortHandler());
		CustomLineChartRenderer lineRenderer = new CustomLineChartRenderer(
				chart, chart.getAnimator(), chart.getViewPortHandler());
		CustomBarChartRenderer barRenderer = new CustomBarChartRenderer(
				chart, chart.getAnimator(), chart.getViewPortHandler(),
				swellDirections);
		List<DataRenderer> subRenderers = new ArrayList<>();
		subRenderers.add(barRenderer);
		subRenderers.add(lineRenderer);
		combinedRenderer.setSubRenderers(subRenderers);
		chart.setRenderer(combinedRenderer);

        chart.getXAxis().setLabelCount(axisLabelValues.size());
        chart.getLegend().setTextSize(labelTextSize);

        // Replace default chart label
        final String chartLabel = getString(R.string.wa_chart_title);
        ((TextView)findViewById(R.id.txtChartTitle)).setText(chartLabel);
        Description desc = new Description();
        desc.setEnabled(false);
        chart.setDescription(desc);

        // Update chart display
		final float pad = 15f;
		final float ratio = 0.5f;
        /*
        chart.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height));
        chart.setPadding(pad, pad, pad, pad);
        */

        // Fit chart to screen, and fit data to chart
        chart.getXAxis().setAxisMinimum(-ratio);
        chart.getXAxis().setAxisMaximum(chart.getXAxis().mAxisMaximum + ratio);
        chart.setExtraOffsets(pad, pad, pad, pad);
        chart.setDoubleTapToZoomEnabled(false);

        chart.invalidate();

        return true;
    }

    private boolean initCurrentWeather(final JSONObject forecastJSON) {
        try {
            String str;
            Double dbl;

            final boolean isMetric = PAWSAPI.preferredMetric(mSharedPref);

            // Fetch the latest weather forecast for the provided location
            final JSONObject currentWeatherJson = forecastJSON.getJSONArray("list")
                    .getJSONObject(0);

            // Update LatLng value for returning to MapsActivity with the weather data location
            final LatLng latLng;
            if (forecastJSON.has("lat_lng")) {
            	Log.d(TAG, "LatLng data for forecast identified.");
				latLng = new LatLng(
						forecastJSON.getJSONObject("lat_lng").getDouble("latitude"),
						forecastJSON.getJSONObject("lat_lng").getDouble("longitude"));
			}
            else {
				Log.d(TAG, "No existing LatLng data for forecast.");
            	final Location lastBestLocation = PAWSAPI.getLastBestLocation(mSharedPref);
            	latLng = new LatLng(
            			lastBestLocation.getLatitude(),
						lastBestLocation.getLongitude());
			}
            mPlaceLatLng = latLng;

            // Weather title -- City of forecast
            str = forecastJSON.getJSONObject("city").getString("name");
            ((TextView)findViewById(R.id.txtWeatherCity)).setText(str);

            // Weather subtitle -- Nearby place from maps marker
            if (mNearbyPlace != null && !mNearbyPlace.equals("") && !mNearbyPlace.equals(str)) {
                ((TextView)findViewById(R.id.txtWeatherNearby)).setText(
                        getString(R.string.wa_nearby) + ' ' + mNearbyPlace);
                findViewById(R.id.txtWeatherNearby).setVisibility(View.VISIBLE);
            } else {
                ((TextView)findViewById(R.id.txtWeatherNearby)).setText("");
                findViewById(R.id.txtWeatherNearby).setVisibility(View.GONE);
            }

            // Weather description
            str = currentWeatherJson.getJSONArray("weather").getJSONObject(0)
                    .getString("description");
            ((TextView)findViewById(R.id.txtWeatherDescription)).setText(str);

            // Weather icon
            str = currentWeatherJson.getJSONArray("weather").getJSONObject(0)
                    .getString("icon");
            ((ImageView)findViewById(R.id.imgWeatherIcon)).setImageDrawable(
                    PAWSAPI.getWeatherDrawable(this, str));

            // Current temperature
            dbl = currentWeatherJson.getJSONObject("main").getDouble("temp");
            str = PAWSAPI.getTemperatureString(dbl, isMetric);
            ((TextView)findViewById(R.id.txtTempCurrent)).setText(str);

            // Current wind
            dbl = currentWeatherJson.getJSONObject("wind").getDouble("speed");
            str = PAWSAPI.getWindSpeedString(dbl, isMetric);
            ((TextView)findViewById(R.id.txtWindCurrent)).setText(str);

            // Current precipitation
            dbl = 0.0d;
            if (currentWeatherJson.has("rain"))
                if (currentWeatherJson.getJSONObject("rain").has("3h"))
                    dbl = currentWeatherJson.getJSONObject("rain").getDouble("3h");
            str = PAWSAPI.getPrecipitationString(isMetric, dbl);
            ((TextView)findViewById(R.id.txtPrecipCurrent)).setText(str);

            // Current humidity
            dbl = (double)currentWeatherJson.getJSONObject("main").getInt("humidity");
            str = PAWSAPI.getSimplePercentageString(dbl);
            ((TextView)findViewById(R.id.txtHumidityCurrent)).setText(str);
        } catch (JSONException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean initTodaysWeather(final JSONObject forecastJSON) {
        try {
            String str;
            Double dbl;

            final int elemsPerDay = 24 / 3;
            final int pad = Math.round(getResources().getDimension(R.dimen.app_spacing_medium));

            final boolean isMetric = PAWSAPI.preferredMetric(mSharedPref);

            // todo: prevent today's weather from accumulating
            LinearLayout layParent = findViewById(R.id.layWeatherToday);
            for (int elem = 0; elem < elemsPerDay + 1; elem++) {
                final JSONObject periodicWeatherJson = forecastJSON
                        .getJSONArray("list").getJSONObject(elem);

                // Create a new LinearLayout child
                LinearLayout.LayoutParams params;
                LinearLayout layout = new LinearLayout(this);
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layout.setLayoutParams(params);
                layout.setOrientation(LinearLayout.VERTICAL);

                TextView txt;
                ImageView img;

                // Write the time of the forecast sample
                str = PAWSAPI.getShortClockString(
                        periodicWeatherJson.getLong("dt") * 1000);
                txt = new TextView(this);
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                txt.setLayoutParams(params);
                txt.setText(str);
                txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                txt.setTextAppearance(this, R.style.TextAppearance_Paws_Medium);
                txt.setTextColor(ContextCompat.getColor(this, R.color.color_accent_alt));
                txt.setPadding(pad, 0, pad, 0);
                layout.addView(txt);

                // Create a weather icon
                str = periodicWeatherJson.getJSONArray("weather").getJSONObject(0)
                        .getString("icon");
                img = new ImageView(this);
                img.setImageDrawable(PAWSAPI.getWeatherDrawable(this, str));
                params = new LinearLayout.LayoutParams(
                        Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)),
                        Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)));
                params.gravity = Gravity.CENTER;
                img.setLayoutParams(params);
                layout.addView(img);

                // Add any predicted precipitation
                int id = periodicWeatherJson.getJSONArray("weather").getJSONObject(0)
                        .getInt("id");
                if (id < 800) {
                    if (id > 100) {
                        // Rainy weather, measurements as periodic volume in millimetres
                        dbl = periodicWeatherJson.getJSONObject("rain").getDouble("3h");
                        str = PAWSAPI.getPrecipitationString(isMetric, dbl);
                    }
                } else if (id == 800) {
                    // Clear weather, no notable measurements
                    str = "";
                } else {
                    // Cloudy weather, measurements in percentage coverage
                    dbl = periodicWeatherJson.getJSONObject("clouds").getDouble("all");
                    str = PAWSAPI.getSimplePercentageString(dbl);
                }

                txt = new TextView(this);
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                txt.setLayoutParams(params);
                txt.setText(str);
                txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                txt.setTextAppearance(this, R.style.TextAppearance_Paws_Tiny);
                txt.setTextColor(ContextCompat.getColor(this, R.color.color_on_background));
                layout.addView(txt);

                // Add the predicted temperature
                dbl = periodicWeatherJson.getJSONObject("main").getDouble("temp");
                str = PAWSAPI.getTemperatureString(dbl);
                txt = new TextView(this);
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                txt.setLayoutParams(params);
                txt.setText(str);
                txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                txt.setTextAppearance(this, R.style.TextAppearance_Paws_Medium);
                txt.setTextColor(ContextCompat.getColor(this, R.color.color_accent_alt));
                layout.addView(txt);

                // Add the child to the hierarchy
                layParent.addView(layout);
            }

        } catch (JSONException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean initWeeklyWeather(final JSONObject forecastJSON) {
        try {
            // todo: prevent the 5-day forecast from accumulating

            // todo: rewrite the 4 (!) different loops over the week in every iteration of this loop

            String str;
            Double dbl;

            final int elemsPerDay = 24 / 3;
            final int pad = Math.round(getResources().getDimension(R.dimen.app_spacing_medium));

            final boolean isMetric = PAWSAPI.preferredMetric(mSharedPref);

            double tempHigh = forecastJSON.getJSONArray("list").getJSONObject(elemsPerDay)
                    .getJSONObject("main")
                    .getDouble("temp");
            double tempLow = tempHigh;
            String icon = forecastJSON.getJSONArray("list").getJSONObject(elemsPerDay)
                    .getJSONArray("weather").getJSONObject(0)
                    .getString("icon");
            String icon2 = icon;
            int weatherId1 = forecastJSON.getJSONArray("list").getJSONObject(elemsPerDay)
                    .getJSONArray("weather").getJSONObject(0)
                    .getInt("id");
            int weatherId2 = weatherId1;

            LinearLayout layParent = findViewById(R.id.layWeatherWeekly);
            final int elemCount = forecastJSON.getJSONArray("list").length();
            for (int elem = elemsPerDay; elem < elemCount; ++elem) {
                final JSONObject periodicWeatherJson = forecastJSON
                        .getJSONArray("list").getJSONObject(elem);

                if ((elem + 1) % elemsPerDay == 0) {
                    LinearLayout.LayoutParams params;

                    // Create a vertical divider between children
                    if (elem / elemsPerDay > 1) {
                        View view = new View(this);
                        params = new LinearLayout.LayoutParams(
                                1,
                                LinearLayout.LayoutParams.MATCH_PARENT);
                        view.setLayoutParams(params);
                        view.setBackgroundColor(ContextCompat.getColor(
                                this, R.color.color_primary));
                        view.setAlpha(0.75f);
                        layParent.addView(view);
                    }

                    // Create a new LinearLayout child
                    LinearLayout layout = new LinearLayout(this);
                    params = new LinearLayout.LayoutParams(
                            250,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    layout.setLayoutParams(params);
                    layout.setOrientation(LinearLayout.VERTICAL);

                    TextView txt;
                    ImageView img;

                    // Write the day's title
                    str = DateFormat.format("E",
                            forecastJSON.getJSONArray("list")
                                    .getJSONObject(elem - 8)
                                    .getLong("dt") * 1000).toString();
                    txt = new TextView(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.CENTER;
                    txt.setLayoutParams(params);
                    txt.setText(str);
                    txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    txt.setTextAppearance(this, R.style.TextAppearance_Paws_Medium);
                    txt.setTextColor(ContextCompat.getColor(
                            this, R.color.color_on_background));
                    txt.setPadding(0, pad / 3, 0, pad);
                    layout.addView(txt);

                    // Create a new layout child for weather icons
                    LinearLayout layTemp = new LinearLayout(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    layTemp.setLayoutParams(params);
                    layTemp.setOrientation(LinearLayout.HORIZONTAL);

                    // Create primary weather icon
                    img = new ImageView(this);
                    img.setImageDrawable(PAWSAPI.getWeatherDrawable(this, icon));
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)),
                            1);
                    img.setLayoutParams(params);
                    layTemp.addView(img);

                    // Create secondary weather icon
                    img = new ImageView(this);
                    img.setImageDrawable(PAWSAPI.getWeatherDrawable(this, icon2));
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)),
                            1);
                    img.setLayoutParams(params);
                    layTemp.addView(img);

                    // Add the icon layout to the child
                    layout.addView(layTemp);

                    // Create a new layout child for the daily temperature range
                    layTemp = new LinearLayout(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    layTemp.setLayoutParams(params);
                    layTemp.setOrientation(LinearLayout.HORIZONTAL);

                    // Add the predicted high temperature
                    str = PAWSAPI.getTemperatureString(tempHigh);
                    txt = new TextView(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1);
                    txt.setLayoutParams(params);
                    txt.setText(str);
                    txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    txt.setTextAppearance(this, R.style.TextAppearance_Paws_Medium);
                    txt.setTextColor(ContextCompat.getColor(
                            this, R.color.color_accent_alt));
                    layTemp.addView(txt);

                    // Add the predicted low temperature
                    str = PAWSAPI.getTemperatureString(tempLow);
                    txt = new TextView(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1);
                    txt.setLayoutParams(params);
                    txt.setText(str);
                    txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    txt.setTextAppearance(this, R.style.TextAppearance_Paws_Medium);
                    txt.setTextColor(ContextCompat.getColor(
                            this, R.color.color_on_background));
                    layTemp.addView(txt);

                    // Add the temperature layout to the child
                    layout.addView(layTemp);

                    // Create a wind bearing icon
                    dbl = 0d;
                    for (int i = elem - elemsPerDay; i < elem; i++)
                        dbl += forecastJSON.getJSONArray("list").getJSONObject(i)
                                .getJSONObject("wind")
                                .getDouble("deg");
                    dbl /= elemsPerDay;
                    img = new ImageView(this);
                    img.setImageDrawable(getDrawable(R.drawable.ic_navigation));
                    img.setColorFilter(ContextCompat.getColor(
                            this, R.color.color_on_background));
                    img.setRotation(dbl.floatValue());
                    params = new LinearLayout.LayoutParams(
                            Math.round(getResources().getDimension(R.dimen.dimen_icon_small)),
                            Math.round(getResources().getDimension(R.dimen.dimen_icon_small)));
                    params.gravity = Gravity.CENTER;
                    params.topMargin = pad;
                    params.bottomMargin = pad;
                    img.setLayoutParams(params);
                    layout.addView(img);

                    // Add the predicted average wind speed
                    dbl = 0d;
                    for (int i = elem - elemsPerDay - 1; i < elem; i++)
                        dbl += forecastJSON.getJSONArray("list").getJSONObject(i)
                                .getJSONObject("wind").getDouble("speed");
                    str = PAWSAPI.getWindSpeedString(dbl / elemsPerDay, isMetric);
                    txt = new TextView(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.CENTER;
                    txt.setLayoutParams(params);
                    txt.setText(str);
                    txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    txt.setTextAppearance(this, R.style.TextAppearance_Paws_Small);
                    txt.setTextColor(ContextCompat.getColor(
                            this, R.color.color_on_background));
                    layout.addView(txt);

                    // Add any precipitation for the lowest-tier weather effects for the day
                    str = "";
                    if (weatherId1 < 800) {
                        if (weatherId1 > 100) {
                            // Rainy weather, measurements as daily total volume in millimetres
                            dbl = 0d;
                            for (int i = elem - elemsPerDay - 1; i < elem; i++) {
                                if (forecastJSON.getJSONArray("list")
                                        .getJSONObject(i).has("rain")) {
                                    if (forecastJSON.getJSONArray("list")
                                            .getJSONObject(i).getJSONObject("rain")
                                            .has("3h")) {
                                        Log.d(TAG,
                                                "Sampling rain/3h from element " + i + ". ("
                                                        + forecastJSON.getJSONArray("list")
                                                        .getJSONObject(i).getJSONObject("rain")
                                                        .getDouble("3h") + ")");
                                        dbl += forecastJSON.getJSONArray("list")
                                                .getJSONObject(i)
                                                .getJSONObject("rain").getDouble("3h");
                                    }
                                }
                            }
                            if (dbl > 0d)
                                str = PAWSAPI.getPrecipitationString(isMetric, dbl);
                        }
                    } else if (weatherId1 == 800) {
                        // Clear skies, no notable measurements
                    } else {
                        // Cloudy weather, measurements in percentage coverage
                        dbl = 0d;
                        for (int i = elem - elemsPerDay - 1; i < elem; i++) {
                            Log.d(TAG, "Sampling clouds/all from element " + i + ". ("
                                    + forecastJSON.getJSONArray("list")
                                    .getJSONObject(i).getJSONObject("clouds")
                                    .getInt("all") + ")");
                            dbl += Double.parseDouble(
                                    forecastJSON.getJSONArray("list")
                                            .getJSONObject(i).getJSONObject("clouds")
                                            .getString("all"));
                        }
                        str = PAWSAPI.getSimplePercentageString(dbl / elemsPerDay);
                    }

                    if (!(str.equals(""))) {
                        Log.d(TAG, "Adding additional weather data ("
                                + str + ") for day " + elem / elemsPerDay);

                        // Create a weather icon
                        img = new ImageView(this);
                        img.setImageDrawable(PAWSAPI.getWeatherDrawable(this, icon));
                        params = new LinearLayout.LayoutParams(
                                Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)),
                                Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)));
                        params.gravity = Gravity.CENTER;
                        params.topMargin = pad;
                        img.setLayoutParams(params);
                        layout.addView(img);

                        // Add the additional information
                        txt = new TextView(this);
                        params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.gravity = Gravity.CENTER;
                        txt.setLayoutParams(params);
                        txt.setText(str);
                        txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        txt.setTextAppearance(this, R.style.TextAppearance_Paws_Tiny);
                        txt.setTextColor(ContextCompat.getColor(
                                this, R.color.color_on_background));
                        layout.addView(txt);
                    }

                    // Add the daily weather child to the hierarchy
                    layParent.addView(layout);

                    // After each 24-hour cluster of samples, publish the data as a new day
                    double temp = periodicWeatherJson.getJSONObject("main").getDouble("temp");
                    tempHigh = temp;
                    tempLow = temp;
                }

                // Compare temperatures sampled per 3 hours to identify highs and lows for the day
                double temp = periodicWeatherJson.getJSONObject("main").getDouble("temp");
                Log.d(TAG, "Sampling temperature/3h from element "
                        + elem + ". (" + temp + ")");

                if (tempHigh < temp)
                    tempHigh = temp;
                if (tempLow > temp)
                    tempLow = temp;

                // Compare weather IDs to bring notable weather events to attention
                // Note:
                // Cloud > Clear = 800 > Atmospherics > Snow > Rain > Drizzle > Thunderstorm
                int elemWeatherId = periodicWeatherJson.getJSONArray("weather")
                        .getJSONObject(0).getInt("id");
                if (elemWeatherId < weatherId1) {
                    weatherId1 = elemWeatherId;
                    icon = periodicWeatherJson.getJSONArray("weather")
                            .getJSONObject(0).getString("icon");
                } else if (elemWeatherId < weatherId2) {
                    weatherId2 = elemWeatherId;
                    icon = periodicWeatherJson.getJSONArray("weather")
                            .getJSONObject(0).getString("icon");
                }
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean initWeatherDisplay(final String response) {
        try {
            final JSONObject forecastJSON = new JSONObject(response);

            if (!initCurrentWeather(forecastJSON)
                    || !initTodaysWeather(forecastJSON)
                    || !initWeeklyWeather(forecastJSON)) {
                Log.e(TAG, "Failed to initialise the place info weather display.");
                return false;
            }

            /* Fill in other weather details */

            // TODO : Guarantee sunrise/sunset uses local timezone rather than system timezone.

            String str;

            // Sunrise and sunset
            str = PAWSAPI.getClockString(this,
                    forecastJSON.getJSONObject("city").getLong("sunrise") * 1000,
                    true);
            ((TextView)findViewById(R.id.txtSunriseTime)).setText(str);

            str = PAWSAPI.getClockString(this,
                    forecastJSON.getJSONObject("city").getLong("sunset") * 1000,
                    true);
            ((TextView)findViewById(R.id.txtSunsetTime)).setText(str);

            // Timestamp for current weather sample
            final int whichTime = PAWSAPI.getWeatherJsonIndexForTime(
                    forecastJSON.getJSONArray("list"), System.currentTimeMillis());
            if (whichTime >= 0) {
                str = PAWSAPI.getWeatherTimestampString(this,
                        forecastJSON.getJSONArray("list").getJSONObject(whichTime)
                                .getLong("dt") * 1000);
                ((TextView)findViewById(R.id.txtWeatherTimestamp)).setText(str);
            } else {
                Log.e(TAG, "Invalid or obsolete weather JSON.");
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
}
