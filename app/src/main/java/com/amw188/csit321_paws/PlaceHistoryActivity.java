package com.amw188.csit321_paws;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PlaceHistoryActivity extends BottomNavBarActivity {

	private static final String TAG = PrefConstValues.tag_prefix + "pha";
	private SharedPreferences mSharedPref;

	class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

		private JSONArray mHistoryJson;

		public PlaceAdapter(JSONArray historyJson) {
			mHistoryJson = historyJson;
		}

		class ViewHolder extends RecyclerView.ViewHolder {
			ImageView btnFavorite;
			TextView txtTitle;
			TextView txtSubtitle;
			ImageView btnOptions;

			ViewHolder(View view) {
				super(view);

				btnFavorite = view.findViewById(R.id.imgPlaceFavorite);
				txtTitle = view.findViewById(R.id.txtPlaceTitle);
				txtSubtitle = view.findViewById(R.id.txtPlaceSubtitle);
				btnOptions = view.findViewById(R.id.imgPlaceOptions);
			}
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			Context context = parent.getContext();
			LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(
					R.layout.view_placehistoryelement, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			try {
				JSONObject placeJson = mHistoryJson.getJSONObject(position);
				holder.txtTitle.setText();
				holder.txtSubtitle.setText();
				// todo: identify favourite in history data
				if ()
					toggleFavorite(holder.btnFavorite);
				holder.btnFavorite.setOnClickListener(this::toggleFavorite);
				holder.btnOptions.setOnClickListener(this::toggleOptions);
			} catch (JSONException ex) {
				Log.e(TAG, "Failed to parse place history JSON.");
			}
		}

		@Override
		public int getItemCount() {
			return mHistoryJson.length();
		}

		private void toggleFavorite(View view) {
			Log.d(TAG, "in toggleFavorite()");

			view.setBackgroundResource(R.drawable.ic_star);
			// todo: swap favourite in history data
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
		BottomNavigationView nav = findViewById(R.id.bottomNavigation);
		nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
		return true;
	}

	private boolean initRecyclerView() {
		try {
			JSONArray historyJson = new JSONArray(mSharedPref.getString(
					PrefKeys.position_history, PrefConstValues.empty_json_array));
			PlaceAdapter adapter = new PlaceAdapter(historyJson);
			LinearLayoutManager layoutManager = new LinearLayoutManager(this);

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
