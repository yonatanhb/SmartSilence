package com.yet.smartsilence.activities;

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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.yet.smartsilence.R;
import com.yet.smartsilence.database.RuleDatabaseHelper;
import com.yet.smartsilence.database.models.RuleModel;
import com.yet.smartsilence.views.WeekDaysView;

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

    private EditText inputRuleName;
    private RadioGroup ruleTypeGroup;
    private RadioButton radioTime, radioLocation;
    private WeekDaysView weekDaysView;
    private Button btnTimeStart, btnTimeEnd, btnSave;
    private EditText inputLocationName, inputRadius;
    private CardView timeFieldsCard, locationFieldsCard;

    private RuleDatabaseHelper dbHelper;
    private String timeStart = "", timeEnd = "";
    private boolean isEditing = false;
    private int editingRuleId = -1;

    private GoogleMap map;
    private LatLng selectedLatLng;
    private Circle radiusCircle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_rule);

        dbHelper = new RuleDatabaseHelper(this);
        bindViews();
        setupMap();
        setupListeners();

        // Check if editing
        editingRuleId = getIntent().getIntExtra("ruleId", -1);
        isEditing = editingRuleId != -1;

        if (isEditing) {
            loadRule(editingRuleId);
        } else {
            showTypeFields("time"); // Default
            radioTime.setChecked(true);
        }
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(googleMap -> {
                map = googleMap;

                // Configure map UI
                map.getUiSettings().setZoomControlsEnabled(true);
                map.getUiSettings().setZoomGesturesEnabled(true);
                map.getUiSettings().setMapToolbarEnabled(false);

                if (isEditing && selectedLatLng != null) {
                    updateMapMarkerAndCircle();
                    int radius = 100;
                    try {
                        String radiusStr = inputRadius.getText().toString();
                        if (!radiusStr.isEmpty()) radius = Integer.parseInt(radiusStr);
                    } catch (Exception ignored) {}
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, getZoomLevel(radius)));
                } else {
                    // אחרת – תציג מרכז ישראל
                    LatLng israelCenter = new LatLng(31.0461, 34.8516);
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(israelCenter, 7));
                }
                // Enable location if permission granted
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    map.setMyLocationEnabled(true);
                }

                // Handle map clicks
                map.setOnMapClickListener(latLng -> {
                    selectedLatLng = latLng;
                    updateMapMarkerAndCircle();
                    updateLocationName(latLng);
                });
            });
        }
    }

    private void updateMapMarkerAndCircle() {
        if (map == null || selectedLatLng == null) return;

        // Clear previous markers and circles
        map.clear();

        // Add marker
        map.addMarker(new MarkerOptions()
                .position(selectedLatLng)
                .title("מיקום נבחר"));

        // Add radius circle
        updateRadiusCircle();
    }

    private void updateRadiusCircle() {
        if (map == null || selectedLatLng == null) return;

        // Remove previous circle
        if (radiusCircle != null) {
            radiusCircle.remove();
        }

        // Get radius from input
        int radius = 100; // Default
        try {
            String radiusText = inputRadius.getText().toString().trim();
            if (!radiusText.isEmpty()) {
                radius = Integer.parseInt(radiusText);
            }
        } catch (NumberFormatException e) {
            radius = 100;
        }

        // Create new circle
        radiusCircle = map.addCircle(new CircleOptions()
                .center(selectedLatLng)
                .radius(radius)
                .strokeColor(Color.parseColor("#4285F4"))
                .strokeWidth(3)
                .fillColor(Color.parseColor("#204285F4"))); // Semi-transparent blue

        // Adjust camera to show the circle
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng,
                getZoomLevel(radius)));
    }

    private float getZoomLevel(int radius) {
        // Calculate appropriate zoom level based on radius
        if (radius <= 50) return 17f;
        else if (radius <= 100) return 16f;
        else if (radius <= 250) return 15f;
        else if (radius <= 500) return 14f;
        else if (radius <= 1000) return 13f;
        else return 12f;
    }

    private void updateLocationName(LatLng latLng) {
        // Update location name using reverse geocoding
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String placeName = "";

                if (address.getFeatureName() != null) {
                    placeName = address.getFeatureName();
                } else if (address.getThoroughfare() != null) {
                    placeName = address.getThoroughfare();
                } else if (address.getLocality() != null) {
                    placeName = address.getLocality();
                } else {
                    placeName = "מיקום נבחר";
                }

                inputLocationName.setText(placeName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            inputLocationName.setText("מיקום נבחר");
        }
    }

    private void bindViews() {
        inputRuleName = findViewById(R.id.inputRuleName);
        ruleTypeGroup = findViewById(R.id.ruleTypeGroup);
        radioTime = findViewById(R.id.radioTime);
        radioLocation = findViewById(R.id.radioLocation);
        weekDaysView = findViewById(R.id.weekDaysView);
        btnTimeStart = findViewById(R.id.btnTimeStart);
        btnTimeEnd = findViewById(R.id.btnTimeEnd);
        btnSave = findViewById(R.id.btnSaveRule);
        inputLocationName = findViewById(R.id.inputLocationName);
        inputRadius = findViewById(R.id.inputRadius);
        timeFieldsCard = findViewById(R.id.timeFieldsCard);
        locationFieldsCard = findViewById(R.id.locationFieldsCard);
    }

    private void setupListeners() {
        weekDaysView.setSelectable(true);

        ruleTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioTime) {
                showTypeFields("time");
            } else if (checkedId == R.id.radioLocation) {
                showTypeFields("location");
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

    private void showTypeFields(String type) {
        if ("time".equals(type)) {
            animateCardVisibility(timeFieldsCard, true);
            animateCardVisibility(locationFieldsCard, false);
        } else {
            animateCardVisibility(timeFieldsCard, false);
            animateCardVisibility(locationFieldsCard, true);
        }
    }

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

    private void loadRule(int id) {
        for (RuleModel rule : dbHelper.getAllRules()) {
            if (rule.getId() == id) {
                inputRuleName.setText(rule.getRuleName());

                if ("time".equals(rule.getType())) {
                    radioTime.setChecked(true);
                    showTypeFields("time");

                    timeStart = rule.getTimeStart();
                    timeEnd = rule.getTimeEnd();
                    btnTimeStart.setText(timeStart);
                    btnTimeEnd.setText(timeEnd);
                    weekDaysView.setDaysMask(rule.getDaysMask());

                } else {
                    radioLocation.setChecked(true);
                    showTypeFields("location");

                    inputLocationName.setText(rule.getLocationName());
                    inputRadius.setText(String.valueOf(rule.getRadius()));

                    // Show location on map
                    selectedLatLng = new LatLng(rule.getLatitude(), rule.getLongitude());
                    if (map != null && selectedLatLng != null) {
                        updateMapMarkerAndCircle();
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng,
                                getZoomLevel(rule.getRadius())));
                    }
                }
                break;
            }
        }
    }

    private void saveRule() {
        String name = inputRuleName.getText().toString().trim();
        String type = radioTime.isChecked() ? "time" : "location";

        if (TextUtils.isEmpty(name)) {
            inputRuleName.setError("נא למלא שם חוק");
            inputRuleName.requestFocus();
            return;
        }

        RuleModel rule = new RuleModel();
        rule.setRuleName(name);
        rule.setType(type);
        rule.setActive(true);

        if ("time".equals(type)) {
            if (timeStart.isEmpty() || timeEnd.isEmpty()) {
                Toast.makeText(this, "יש לבחור שעות התחלה וסיום", Toast.LENGTH_SHORT).show();
                return;
            }
            rule.setTimeStart(timeStart);
            rule.setTimeEnd(timeEnd);
            rule.setDaysMask(weekDaysView.getDaysMask());
        } else {
            if (selectedLatLng == null) {
                Toast.makeText(this, "בחר מיקום במפה", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                rule.setLocationName(inputLocationName.getText().toString().trim());
                rule.setLatitude(selectedLatLng.latitude);
                rule.setLongitude(selectedLatLng.longitude);
                rule.setRadius(Integer.parseInt(inputRadius.getText().toString()));
            } catch (Exception e) {
                Toast.makeText(this, "שדות מיקום לא תקינים", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Animate save button
        animateSaveButton();

        if (isEditing) {
            rule.setId(editingRuleId);
            boolean updated = dbHelper.updateRuleById(rule);
            if (updated) {
                Toast.makeText(this, "החוק עודכן בהצלחה", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "עדכון נכשל", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "החוק נוסף בהצלחה", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

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