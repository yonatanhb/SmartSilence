package com.yet.smartsilence;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.yet.smartsilence.database.RuleDatabaseHelper;

public class MainActivity extends AppCompatActivity {

    private RuleDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            dbHelper = new RuleDatabaseHelper(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}