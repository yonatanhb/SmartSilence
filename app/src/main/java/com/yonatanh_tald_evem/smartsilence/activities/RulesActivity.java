package com.yonatanh_tald_evem.smartsilence.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yonatanh_tald_evem.smartsilence.R;
import com.yonatanh_tald_evem.smartsilence.database.RuleDatabaseHelper;
import com.yonatanh_tald_evem.smartsilence.database.models.RuleModel;

import java.util.List;

public class RulesActivity extends AppCompatActivity {

    private RuleDatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private RulesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rules);

        dbHelper = new RuleDatabaseHelper(this);
        recyclerView = findViewById(R.id.rulesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load rules from DB in background thread
        new Thread(() -> {
            List<RuleModel> rules = dbHelper.getAllRules();

            // Update UI on the main thread
            runOnUiThread(() -> {
                adapter = new RulesAdapter(rules);
                recyclerView.setAdapter(adapter);
            });
        }).start();

        // Button to add a new rule
        FloatingActionButton addBtn = findViewById(R.id.addRuleButton);
        addBtn.setOnClickListener(v -> startActivity(new Intent(RulesActivity.this, AddEditRuleActivity.class)));
    }

    // Reload rules every time the activity is resumed
    @Override
    protected void onResume() {
        super.onResume();
        loadRules();
    }

    // Loads all rules from the DB and updates the adapter
    private void loadRules() {
        new Thread(() -> {
            List<RuleModel> rules = dbHelper.getAllRules();

            runOnUiThread(() -> {
                if (adapter == null) {
                    adapter = new RulesAdapter(rules);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.rules.clear();
                    adapter.rules.addAll(rules);
                    adapter.notifyDataSetChanged();
                }
            });
        }).start();
    }

    // Adapter class to manage RecyclerView list of rules
    class RulesAdapter extends RecyclerView.Adapter<RulesAdapter.RuleViewHolder> {

        public final List<RuleModel> rules;

        RulesAdapter(List<RuleModel> rules) {
            this.rules = rules;
        }

        @NonNull
        @Override
        public RuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rule, parent, false);
            return new RuleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RuleViewHolder holder, int position) {
            RuleModel rule = rules.get(position);

            // Set rule name
            String name = rule.getRuleName() != null ? rule.getRuleName() : getString(R.string.unnamed_rule);
            holder.ruleText.setText(name);

            // Show rule details depending on type
            String details;
            if ("time".equals(rule.getType())) {
                details = getString(R.string.time_label, rule.getTimeStart(), rule.getTimeEnd());
            } else {
                details = getString(R.string.location_label, rule.getLocationName());
            }
            holder.ruleDetails.setText(details);

            // Show active days
            String daysText = dbHelper.getDaysString(rule.getDaysMask());
            holder.ruleDays.setText(daysText);

            // Delete rule button
            holder.deleteBtn.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition == RecyclerView.NO_POSITION) {
                    Log.w("SmartSilence", getString(R.string.delete_invalid_position));
                    return;
                }

                RuleModel ruleToDelete = rules.get(currentPosition);

                new AlertDialog.Builder(holder.itemView.getContext())
                        .setTitle(getString(R.string.delete_confirmation_title))
                        .setMessage(getString(R.string.delete_confirmation_message, ruleToDelete.getRuleName()))
                        .setPositiveButton(getString(R.string.delete_button), (dialog, which) -> {

                            View dialogView = LayoutInflater.from(holder.itemView.getContext())
                                    .inflate(R.layout.dialog_loading, null);

                            // Show loading dialog with progress
                            AlertDialog loadingDialog = new AlertDialog.Builder(holder.itemView.getContext())
                                    .setView(dialogView)
                                    .setCancelable(false)
                                    .create();
                            loadingDialog.show();

                            // Simulate delete delay
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                try {
                                    boolean deleted = dbHelper.deleteRuleById(ruleToDelete.getId());

                                    if (deleted) {
                                        int safePos = holder.getAdapterPosition();
                                        if (safePos != RecyclerView.NO_POSITION && safePos < rules.size()) {
                                            rules.remove(safePos);
                                            notifyItemRemoved(safePos);
                                            Log.d("SmartSilence", getString(R.string.rule_deleted_log, ruleToDelete.getRuleName()));

                                            Context context = holder.itemView.getContext();

                                            context.startService(new Intent(context, com.yonatanh_tald_evem.smartsilence.services.TimeSchedulerService.class));

                                            com.yonatanh_tald_evem.smartsilence.services.TimeSchedulerService.scheduleImmediateCheck(context);

                                            Intent locationIntent = new Intent(context, com.yonatanh_tald_evem.smartsilence.services.LocationMonitorService.class);
                                            locationIntent.putExtra("forceRefresh", true);
                                            androidx.core.content.ContextCompat.startForegroundService(context, locationIntent);

                                        }
                                    } else {
                                        Toast.makeText(holder.itemView.getContext(), holder.itemView.getContext().getString(R.string.delete_failed), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    Log.e("SmartSilence", getString(R.string.delete_error) + ": " + e.getMessage(), e);
                                    Toast.makeText(holder.itemView.getContext(), holder.itemView.getContext().getString(R.string.delete_error), Toast.LENGTH_SHORT).show();
                                } finally {
                                    loadingDialog.dismiss();
                                }
                            }, 500);
                        })
                        .setNegativeButton(getString(R.string.cancel_button), null)
                        .show();
            });

            // Edit rule button
            holder.editBtn.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), AddEditRuleActivity.class);
                intent.putExtra("ruleId", rule.getId());
                holder.itemView.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return rules.size();
        }

        // ViewHolder for each rule item
        class RuleViewHolder extends RecyclerView.ViewHolder {
            TextView ruleText, ruleDetails, ruleDays;
            ImageButton editBtn, deleteBtn;

            RuleViewHolder(@NonNull View itemView) {
                super(itemView);
                ruleText = itemView.findViewById(R.id.ruleText);
                ruleDetails = itemView.findViewById(R.id.ruleDetails);
                editBtn = itemView.findViewById(R.id.btnEdit);
                deleteBtn = itemView.findViewById(R.id.btnDelete);
                ruleDays = itemView.findViewById(R.id.ruleDays);
            }
        }
    }
}
