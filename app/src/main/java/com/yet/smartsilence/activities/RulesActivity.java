package com.yet.smartsilence.activities;

import android.app.ProgressDialog;
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
import com.yet.smartsilence.R;
import com.yet.smartsilence.database.RuleDatabaseHelper;
import com.yet.smartsilence.database.models.RuleModel;

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

        List<RuleModel> rules = dbHelper.getAllRules();
        adapter = new RulesAdapter(rules);
        recyclerView.setAdapter(adapter);

        FloatingActionButton addBtn = findViewById(R.id.addRuleButton);
//        addBtn.setOnClickListener(v -> {
//            startActivity(new Intent(RulesActivity.this, AddEditRuleActivity.class));
//        });
    }

    class RulesAdapter extends RecyclerView.Adapter<RulesAdapter.RuleViewHolder> {

        private final List<RuleModel> rules;

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

            // שם החוק
            String name = rule.getRuleName() != null ? rule.getRuleName() : "(ללא שם)";
            holder.ruleText.setText(name);

            // תיאור לפי סוג החוק
            String details;
            if ("time".equals(rule.getType())) {
                details = "זמן: " + rule.getTimeStart() + "–" + rule.getTimeEnd();
            } else {
                details = "מיקום: " + rule.getLocationName();
            }
            holder.ruleDetails.setText(details);

            String daysText = dbHelper.getDaysString(rule.getDaysMask());
            holder.ruleDays.setText(daysText);

            // לחצן מחיקה
            holder.deleteBtn.setOnClickListener(v -> {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition == RecyclerView.NO_POSITION) {
                    Log.w("SmartSilence", "מיקום לא תקף למחיקה");
                    return;
                }

                RuleModel ruleToDelete = rules.get(currentPosition);

                new AlertDialog.Builder(holder.itemView.getContext())
                        .setTitle("אישור מחיקה")
                        .setMessage("האם למחוק את החוק \"" + ruleToDelete.getRuleName() + "\"?")
                        .setPositiveButton("מחק", (dialog, which) -> {

                            // הצגת דיאלוג עם ProgressBar
                            View dialogView = LayoutInflater.from(holder.itemView.getContext())
                                    .inflate(R.layout.dialog_loading, null);

                            AlertDialog loadingDialog = new AlertDialog.Builder(holder.itemView.getContext())
                                    .setView(dialogView)
                                    .setCancelable(false)
                                    .create();
                            loadingDialog.show();

                            // דחיית מחיקה עם סימולציה של זמן פעולה
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                try {
                                    boolean deleted = dbHelper.deleteRuleById(ruleToDelete.getId());

                                    if (deleted) {
                                        int safePos = holder.getAdapterPosition();
                                        if (safePos != RecyclerView.NO_POSITION && safePos < rules.size()) {
                                            rules.remove(safePos);
                                            notifyItemRemoved(safePos);
                                            Log.d("SmartSilence", "חוק נמחק: " + ruleToDelete.getRuleName());
                                        }
                                    } else {
                                        Toast.makeText(holder.itemView.getContext(), "המחיקה נכשלה", Toast.LENGTH_SHORT).show();
                                    }
                                } catch (Exception e) {
                                    Log.e("SmartSilence", "שגיאה במחיקה: " + e.getMessage(), e);
                                    Toast.makeText(holder.itemView.getContext(), "שגיאה במחיקה", Toast.LENGTH_SHORT).show();
                                } finally {
                                    loadingDialog.dismiss();
                                }
                            }, 500);
                        })
                        .setNegativeButton("ביטול", null)
                        .show();
            });

        }


        @Override
        public int getItemCount() {
            return rules.size();
        }

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
