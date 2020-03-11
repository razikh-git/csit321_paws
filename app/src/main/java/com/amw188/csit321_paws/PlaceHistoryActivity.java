package com.amw188.csit321_paws;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PlaceHistoryActivity extends BottomNavBarActivity {

	private static final String TAG = PrefConstValues.tag_prefix + "a_his";

	private static final int TAG_FAVORITE_FALSE = 40;
	private static final int TAG_FAVORITE_TRUE = 41;

	private SharedPreferences mSharedPref;
	private SharedPreferences.Editor mSharedEditor;

	class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

		private JSONArray mHistoryJson;

		PlaceAdapter(JSONArray historyJson) {
			mHistoryJson = historyJson;
		}

		class ViewHolder extends RecyclerView.ViewHolder {
			View heldView;
			ImageView btnFavorite;
			LinearLayout btnPlace;
			TextView txtTitle;
			TextView txtSubtitle;
			ImageView btnOptions;

			ViewHolder(View view) {
				super(view);

				heldView = view;
				btnFavorite = view.findViewById(R.id.imgPlaceFavorite);
				btnPlace = view.findViewById(R.id.layPlaceSummary);
				txtTitle = view.findViewById(R.id.txtPlaceTitle);
				txtSubtitle = view.findViewById(R.id.txtPlaceSubtitle);
				btnOptions = view.findViewById(R.id.imgPlaceOptions);
				btnFavorite.setTag(TAG_FAVORITE_FALSE);
			}
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			Context context = parent.getContext();
			LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(
					R.layout.view_historyelement, parent, false);
			return new ViewHolder(view);
		}

		/**
		 * Called per element added to the recyc view, this method initialises them with
		 * data appropriate to the reference by passing their index in the recycler.
		 * @param holder Element view being initialised.
		 * @param position Index in the recycler.
		 */
		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			try {
				JSONObject placeJson = mHistoryJson.getJSONObject(position);

				Log.d(TAG, "placeJson: " + placeJson.toString());

				// Initialise the buttons and displays for this view
				holder.txtTitle.setText(placeJson.getString("title"));
				holder.txtSubtitle.setText(placeJson.getString("subtitle"));

				// Initialise clickables
				holder.btnPlace.setOnClickListener(this::redirectPlaceInfo);
				holder.btnFavorite.setOnClickListener(this::toggleFavorite);
				holder.btnOptions.setOnClickListener(this::toggleOptions);
				if (placeJson.getBoolean("favorite"))
					toggleFavorite(holder.btnFavorite);

				// Pin the index in the history JSON to the PlaceInfo redirect
				holder.btnPlace.setTag(position);
			} catch (JSONException ex) {
				Log.e(TAG, "Failed to parse place history JSON.");
				ex.printStackTrace();
			}
		}

		@Override
		public int getItemCount() {
			return mHistoryJson.length();
		}

		private void redirectPlaceInfo(View view) {
			try {
				final JSONObject placeJson = mHistoryJson.getJSONObject((int)view.getTag());

				Log.d(TAG, "placeJson: " + placeJson.toString());

				final LatLng latLng = new LatLng(
						placeJson.getJSONObject("lat_lng").getDouble("latitude"),
						placeJson.getJSONObject("lat_lng").getDouble("longitude"));
				final Intent intent = new Intent(getApplicationContext(), PlaceInfoActivity.class);
				intent.putExtra(RequestCodes.EXTRA_LATLNG, latLng);
				startActivity(intent);
			} catch (JSONException ex) {
				Log.e(TAG, "Failed to parse history JSON with index: " + (int)view.getTag());
				ex.printStackTrace();
			}
		}

		private void toggleFavorite(View view) {
			Log.d(TAG, "in toggleFavorite()");

			// todo: reflect changes to sharedprefs live history json
			int tag = TAG_FAVORITE_FALSE;
			int drawable = R.drawable.ic_star_outline;

			if ((int)view.getTag() == TAG_FAVORITE_FALSE) {
				drawable = R.drawable.ic_star;
				tag = TAG_FAVORITE_TRUE;
			}

			view.setTag(tag);
			((ImageView)view).setImageDrawable(getDrawable(drawable));
		}

		private void toggleOptions(View view) {
			Log.d(TAG, "in toggleOptions()");

		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_place_history);
		if (!init()) {
			Log.e(TAG, "Failed to initialise PlaceHistory activity with recycler.");
		}
	}

	private boolean init() {
		return initActivity() && initRecyclerView();
	}

	private boolean initActivity() {
		mSharedPref = this.getSharedPreferences(
				PrefKeys.app_global_preferences, Context.MODE_PRIVATE);
		mSharedEditor = mSharedPref.edit();
		BottomNavigationView nav = findViewById(R.id.bottomNavigation);
		nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
		return true;
	}

	private boolean initRecyclerView() {
		try {
			// Populate the recyc view
			JSONArray historyJson = new JSONArray(mSharedPref.getString(
					PrefKeys.position_history, PrefConstValues.empty_json_array));
			PlaceAdapter adapter = new PlaceAdapter(historyJson);
			LinearLayoutManager layoutManager = new LinearLayoutManager(this);

			// Initialise view
			RecyclerView recyclerView = findViewById(R.id.recycPlaceHistoryList);
			recyclerView.setAdapter(adapter);
			recyclerView.setLayoutManager(layoutManager);
		} catch (JSONException ex) {
			Log.e(TAG, "Failed to initialise Recycler from history JSON.");
			return false;
		}
		return true;
	}

	private void redirectPlaceInfo() {

	}
}
