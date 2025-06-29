
package com.yonatanh_tald_eveb.smartsilence.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.TimePicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.yonatanh_tald_eveb.smartsilence.R;
import com.yonatanh_tald_eveb.smartsilence.database.RuleDatabaseHelper;
import com.yonatanh_tald_eveb.smartsilence.database.models.RuleModel;
import com.yonatanh_tald_eveb.smartsilence.views.WeekDaysView;

import java.util.Calendar;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;

import android.location.Address;
import android.location.Geocoder;

import java.util.List;
import java.util.Locale;
import java.io.IOException;

public class AddEditRuleActivity extends AppCompatActivity {
    // UI components
    private RadioGroup ruleTypeGroup;
    private RadioButton radioTime, radioLocation;
    private WeekDaysView weekDaysView;
    private Button btnTimeStart, btnTimeEnd, btnSave;
    private TextView labelLocationName;
    private EditText inputRadius;
    private EditText inputRuleName;
    private CardView timeFieldsCard, locationFieldsCard;

    // Database helper
    private RuleDatabaseHelper dbHelper;

    // State variables
    private String timeStart = "", timeEnd = "";
    private boolean isEditing = false;
    private int editingRuleId = -1;

    // Google Maps
    private GoogleMap map;
    private LatLng selectedLatLng;
    private Circle radiusCircle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_rule);

        dbHelper = new RuleDatabaseHelper(this);
        bindViews();

        TextView textTitle = findViewById(R.id.textTitle);

        // Check if we are editing an existing rule
        editingRuleId = getIntent().getIntExtra("ruleId", -1);
        isEditing = editingRuleId != -1;

        if (isEditing) {
            textTitle.setText(R.string.edit_rule_title);
            loadRule(editingRuleId);
        } else {
            textTitle.setText(R.string.create_rule_title);
            showTypeFields(getString(R.string.rule_type_time));
            radioTime.setChecked(true);
        }

        setupMap();
        setupListeners();
    }

    // Initializes and configures the Google Map
    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                map = googleMap;

                // Enable map controls
                map.getUiSettings().setZoomControlsEnabled(true);
                map.getUiSettings().setZoomGesturesEnabled(true);
                map.getUiSettings().setMapToolbarEnabled(false);

                // Center map according to existing or default location
                if (isEditing && selectedLatLng != null) {
                    updateMapMarkerAndCircle();
                    int radius = 100;
                    try {
                        String radiusStr = inputRadius.getText().toString();
                        if (!radiusStr.isEmpty()) radius = Integer.parseInt(radiusStr);
                    } catch (Exception ignored) {}
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, getZoomLevel(radius)));
                } else {
                    LatLng israelCenter = new LatLng(31.0461, 34.8516);
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(israelCenter, 7));
                }
                // Enable location if permission granted
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    map.setMyLocationEnabled(true);
                }

                // Allow user to select location by tapping on map
                map.setOnMapClickListener(latLng -> {
                    selectedLatLng = latLng;
                    updateMapMarkerAndCircle();
                    updateLocationName(latLng);
                });
            });
        }
    }

    // Places marker and circle on selected location
    private void updateMapMarkerAndCircle() {
        if (map == null || selectedLatLng == null) return;
        map.clear();
        map.addMarker(new MarkerOptions().position(selectedLatLng).title(getString(R.string.selected_location)));
        updateRadiusCircle();
    }

    // Draws the radius circle on the map
    private void updateRadiusCircle() {
        if (map == null || selectedLatLng == null) return;

        if (radiusCircle != null) radiusCircle.remove();

        int radius = 100;
        try {
            String radiusText = inputRadius.getText().toString().trim();
            if (!radiusText.isEmpty()) {
                radius = Integer.parseInt(radiusText);
            }
        } catch (NumberFormatException e) {
            Log.e("LocationService", "Number Format Exception", e);
        }

        radiusCircle = map.addCircle(new CircleOptions()
                .center(selectedLatLng)
                .radius(radius)
                .strokeColor(Color.parseColor("#4285F4"))
                .strokeWidth(3)
                .fillColor(Color.parseColor("#204285F4")));

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, getZoomLevel(radius)));
    }

    // Calculates map zoom level based on radius
    private float getZoomLevel(int radius) {
        // Calculate appropriate zoom level based on radius
        if (radius <= 50) return 17f;
        else if (radius <= 100) return 16f;
        else if (radius <= 250) return 15f;
        else if (radius <= 500) return 14f;
        else if (radius <= 1000) return 13f;
        else return 12f;
    }

    // Uses reverse geocoding to get a human-readable address for a location
    private void updateLocationName(LatLng latLng) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            String placeName = getString(R.string.selected_location);
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);

                    StringBuilder sb = new StringBuilder();
                    if (address.getThoroughfare() != null) {
                        sb.append(address.getThoroughfare()); // street
                    }
                    if (address.getSubThoroughfare() != null) {
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(address.getSubThoroughfare()); // number
                    }
                    if (address.getLocality() != null) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(address.getLocality()); // city
                    }
                    if (address.getCountryName() != null) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(address.getCountryName()); // country
                    }
                    if (sb.length() > 0) {
                        placeName = sb.toString();
                    }
                }
            } catch (IOException e) {
                Log.e("LocationService", "Error during location update", e);
            }

            final String finalPlaceName = placeName;
            runOnUiThread(() -> labelLocationName.setText(finalPlaceName));
        }).start();
    }

    // Binds all views from XML layout to variables
    private void bindViews() {
        inputRuleName = findViewById(R.id.inputRuleName);
        ruleTypeGroup = findViewById(R.id.ruleTypeGroup);
        radioTime = findViewById(R.id.radioTime);
        radioLocation = findViewById(R.id.radioLocation);
        weekDaysView = findViewById(R.id.weekDaysView);
        btnTimeStart = findViewById(R.id.btnTimeStart);
        btnTimeEnd = findViewById(R.id.btnTimeEnd);
        btnSave = findViewById(R.id.btnSaveRule);
        labelLocationName = findViewById(R.id.labelLocationName);
        inputRadius = findViewById(R.id.inputRadius);
        timeFieldsCard = findViewById(R.id.timeFieldsCard);
        locationFieldsCard = findViewById(R.id.locationFieldsCard);
    }

    // Sets up button click listeners and other UI events
    private void setupListeners() {
        weekDaysView.setSelectable(true);

        ruleTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioTime) {
                showTypeFields("time");
            } else if (checkedId == R.id.radioLocation) {
                showTypeFields(getString(R.string.rule_type_location));
            }
        });

        btnTimeStart.setOnClickListener(v -> showTimePicker(true));
        btnTimeEnd.setOnClickListener(v -> showTimePicker(false));
        btnSave.setOnClickListener(v -> saveRule());

        // Add radius change listener to update circle in real time
        inputRadius.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateRadiusCircle();
            }
        });
    }

    // Displays the appropriate input fields based on the selected rule type (time or location)
    private void showTypeFields(String type) {
        if ("time".equals(type)) {
            animateCardVisibility(timeFieldsCard, true);
            animateCardVisibility(locationFieldsCard, false);
        } else {
            animateCardVisibility(timeFieldsCard, false);
            animateCardVisibility(locationFieldsCard, true);
            if (selectedLatLng == null) {
                labelLocationName.setText(R.string.select_location_from_map);
            }
        }
    }

    // Animates showing or hiding a CardView with fade and scale effects
    private void animateCardVisibility(CardView card, boolean show) {
        if (show && card.getVisibility() == View.VISIBLE) return;
        if (!show && card.getVisibility() == View.GONE) return;

        if (show) {
            card.setVisibility(View.VISIBLE);
            card.setAlpha(0f);
            card.setScaleY(0.8f);

            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f);
            ObjectAnimator scaleIn = ObjectAnimator.ofFloat(card, "scaleY", 0.8f, 1f);

            fadeIn.setDuration(300);
            scaleIn.setDuration(300);

            fadeIn.start();
            scaleIn.start();
        } else {
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(card, "alpha", 1f, 0f);
            ObjectAnimator scaleOut = ObjectAnimator.ofFloat(card, "scaleY", 1f, 0.8f);

            fadeOut.setDuration(200);
            scaleOut.setDuration(200);

            fadeOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    card.setVisibility(View.GONE);
                }
            });

            fadeOut.start();
            scaleOut.start();
        }
    }

    // Opens a time picker dialog and updates the start or end time based on user input
    private void showTimePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(this,
                (TimePicker view, int h, int m) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                    if (isStart) {
                        timeStart = time;
                        btnTimeStart.setText(time);
                    } else {
                        timeEnd = time;
                        btnTimeEnd.setText(time);
                    }
                }, hour, minute, true);

        dialog.show();
    }

    // Loads a rule from the database and populates the UI with its data
    private void loadRule(int id) {
        RuleModel rule = dbHelper.getRuleById(id);

        if (rule == null) {
            Toast.makeText(this, R.string.rule_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        inputRuleName.setText(rule.getRuleName());

        if ("time".equals(rule.getType())) {
            radioTime.setChecked(true);
            showTypeFields(getString(R.string.rule_type_time));

            timeStart = rule.getTimeStart();
            timeEnd = rule.getTimeEnd();
            btnTimeStart.setText(timeStart);
            btnTimeEnd.setText(timeEnd);
            weekDaysView.setDaysMask(rule.getDaysMask());

        } else if ("location".equals(rule.getType())) {
            radioLocation.setChecked(true);
            showTypeFields(getString(R.string.rule_type_location));

            labelLocationName.setText(rule.getLocationName());
            inputRadius.setText(String.valueOf(rule.getRadius()));

            // Show location on map
            selectedLatLng = new LatLng(rule.getLatitude(), rule.getLongitude());
            if (map != null) {
                updateMapMarkerAndCircle();
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng,
                        getZoomLevel(rule.getRadius())));
            }
        }
    }

    // Validates and saves a new or edited rule to the database
    private void saveRule() {
        String name = inputRuleName.getText().toString().trim();
        String type = radioTime.isChecked() ? getString(R.string.rule_type_time) : getString(R.string.rule_type_location);

        if (TextUtils.isEmpty(name)) {
            inputRuleName.setError(getString(R.string.error_rule_name_required));
            inputRuleName.requestFocus();
            return;
        }

        RuleModel rule = new RuleModel();
        rule.setRuleName(name);
        rule.setType(type);
        rule.setActive(true);

        if ("time".equals(type)) {
            if (timeStart.isEmpty() || timeEnd.isEmpty()) {
                Toast.makeText(this, R.string.error_select_time_range, Toast.LENGTH_SHORT).show();
                return;
            }
            rule.setTimeStart(timeStart);
            rule.setTimeEnd(timeEnd);
            rule.setDaysMask(weekDaysView.getDaysMask());
        } else {
            if (selectedLatLng == null) {
                Toast.makeText(this, R.string.error_select_location, Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                rule.setLocationName(labelLocationName.getText().toString().trim());
                rule.setLatitude(selectedLatLng.latitude);
                rule.setLongitude(selectedLatLng.longitude);
                rule.setRadius(Integer.parseInt(inputRadius.getText().toString()));
            } catch (Exception e) {
                Toast.makeText(this, R.string.error_invalid_location_fields, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        animateSaveButton();

        if (isEditing) {
            rule.setId(editingRuleId);
            boolean updated = dbHelper.updateRuleById(rule);
            if (updated) {
                Toast.makeText(this, R.string.rule_updated_success, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.rule_update_failed, Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if ("time".equals(type)) {
                dbHelper.insertManualTimeRule(rule);
            } else {
                dbHelper.insertLocationRule(
                        rule.getRuleName(),
                        rule.getLocationName(),
                        rule.getLatitude(),
                        rule.getLongitude(),
                        rule.getRadius(),
                        true
                );
            }
            Toast.makeText(this, R.string.rule_added_successfully, Toast.LENGTH_SHORT).show();
        }

        startService(new Intent(this, com.yonatanh_tald_eveb.smartsilence.services.TimeSchedulerService.class));
        Intent locationIntent = new Intent(this, com.yonatanh_tald_eveb.smartsilence.services.LocationMonitorService.class);
        locationIntent.putExtra("forceRefresh", true);
        androidx.core.content.ContextCompat.startForegroundService(this, locationIntent);

        com.yonatanh_tald_eveb.smartsilence.services.TimeSchedulerService.scheduleImmediateCheck(this);

        setResult(RESULT_OK);
        finish();
    }


    // Animates a click effect on the save button to provide visual feedback
    private void animateSaveButton() {
        btnSave.setEnabled(false);

        ObjectAnimator scaleDown = ObjectAnimator.ofFloat(btnSave, "scaleX", 1f, 0.95f);
        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(btnSave, "scaleX", 0.95f, 1f);

        scaleDown.setDuration(100);
        scaleUp.setDuration(100);

        scaleDown.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                scaleUp.start();
            }
        });

        scaleUp.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                btnSave.setEnabled(true);
            }
        });

        scaleDown.start();
    }
}