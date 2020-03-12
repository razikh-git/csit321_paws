package com.amw188.csit321_paws;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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

	private SharedPreferences mSharedPref;
	private SharedPreferences.Editor mSharedEditor;

	class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

		private JSONArray mHistoryJson;

		PlaceAdapter(JSONArray historyJson) {
			mHistoryJson = historyJson;
		}

		class ViewHolder extends RecyclerView.ViewHolder {
			View heldView;
			LinearLayout layFavorite;
			LinearLayout layPlace;
			TextView txtTitle;
			TextView txtSubtitle;
			LinearLayout layOptions;

			ViewHolder(View view) {
				super(view);

				heldView = view;
				layFavorite = view.findViewById(R.id.layPlaceFavorite);
				layPlace = view.findViewById(R.id.layPlaceSummary);
				txtTitle = view.findViewById(R.id.txtPlaceTitle);
				txtSubtitle = view.findViewById(R.id.txtPlaceSubtitle);
				layOptions = view.findViewById(R.id.layPlaceOptions);
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

				// Pin the index in the history JSON to the PlaceInfo redirect
				holder.layPlace.setTag(position);
				holder.layFavorite.setTag(position);

				// Initialise clickables
				holder.layPlace.setOnClickListener(this::redirectPlaceInfo);
				holder.layFavorite.setOnClickListener(this::toggleFavorite);
				holder.layOptions.setOnClickListener(this::toggleOptions);
				if (placeJson.getBoolean("favorite"))
					((ImageView)holder.layFavorite.findViewById(R.id.imgPlaceFavorite))
							.setImageDrawable(getDrawable(R.drawable.ic_star));
			} catch (JSONException ex) {
				Log.e(TAG, "Failed to parse place history JSON for bind.");
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
				final Intent intent = new Intent(
						getApplicationContext(), PlaceInfoActivity.class);
				intent.putExtra(RequestCodes.EXTRA_LATLNG, latLng);
				startActivity(intent);
			} catch (JSONException ex) {
				Log.e(TAG, "Failed to parse history JSON for redirect.");
				ex.printStackTrace();
			}
		}

		private void toggleFavorite(View view) {
			Log.d(TAG, "in toggleFavorite(" + view.getTag() + ")");

			try {
				JSONObject placeJson = mHistoryJson.getJSONObject((int)view.getTag());
				final boolean isFav = !placeJson.getBoolean("favorite");

				// Update the recyc view element
				((ImageView)view.findViewById(R.id.imgPlaceFavorite)).setImageDrawable(getDrawable(
						isFav ? R.drawable.ic_star : R.drawable.ic_star_outline));

				// Toggle the favorite value for this place
				placeJson.put("favorite", isFav);

				// Update live history JSON
				mHistoryJson.put((int)view.getTag(), placeJson);
				mSharedEditor.putString(PrefKeys.position_history, mHistoryJson.toString());
				mSharedEditor.apply();

			} catch (JSONException ex) {
				Log.e(TAG, "Failed to parse history JSON for favorite.");
				ex.printStackTrace();
			}
		}

		private void toggleOptions(View view) {
			Log.d(TAG, "in toggleOptions()");

			//todo: add options button functionalities:
			// remove
			// ignore
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
		mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
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
}
