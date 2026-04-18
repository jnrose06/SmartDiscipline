package com.example.myapplication;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentRecordActivity extends Activity {

    private EditText etStudentName;
    private EditText etStudentId;
    private Spinner spinnerViolation;
    private Button btnSaveRecord;
    private Button btnCancelRecord;
    private MySQLiteHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_record);

        dbHelper = new MySQLiteHelper(this);
        bindViews();
        setupDropdowns();
        setupActions();
    }

    private void bindViews() {
        etStudentName = findViewById(R.id.etStudentName);
        etStudentId = findViewById(R.id.etStudentId);
        spinnerViolation = findViewById(R.id.spinnerViolation);
        btnSaveRecord = findViewById(R.id.btnSaveRecord);
        btnCancelRecord = findViewById(R.id.btnCancelRecord);
    }

    private void setupDropdowns() {
        List<String> violationTypes = new ArrayList<>();
        Cursor cursor = dbHelper.getAllViolationTypes();
        try {
            while (cursor.moveToNext()) {
                violationTypes.add(cursor.getString(0));
            }
        } finally {
            cursor.close();
        }

        ArrayAdapter<String> violationAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                violationTypes
        );
        violationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerViolation.setAdapter(violationAdapter);
    }

    private void setupActions() {
        btnSaveRecord.setOnClickListener(v -> saveRecord());
        btnCancelRecord.setOnClickListener(v -> finish());
    }

    private void saveRecord() {
        String studentName = etStudentName.getText().toString().trim();
        String studentId = normalizeStudentId(etStudentId.getText().toString());
        String violation = String.valueOf(spinnerViolation.getSelectedItem());

        if (studentName.isEmpty() || studentId.isEmpty()) {
            Toast.makeText(this, "Please complete all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!dbHelper.isStudentIdExists(studentId)) {
            Toast.makeText(this, "Student ID not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        String normalizedStatus = "PENDING";
        long timestamp = System.currentTimeMillis();
        String month = new SimpleDateFormat("MMMM", Locale.ROOT).format(new Date(timestamp)).toUpperCase(Locale.ROOT);
        String day = new SimpleDateFormat("d", Locale.ROOT).format(new Date(timestamp));
        String year = new SimpleDateFormat("yyyy", Locale.ROOT).format(new Date(timestamp));
        String time = new SimpleDateFormat("hh:mm a", Locale.ROOT).format(new Date(timestamp));

        dbHelper.updateUserFullName(studentId, studentName);
        boolean inserted = dbHelper.addViolationForStudent(
                studentId, violation, normalizedStatus, month, day, year, time, timestamp
        );

        if (!inserted) {
            Toast.makeText(this, "Duplicate violation is not allowed for this student.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Student record saved.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String normalizeStudentId(String studentId) {
        return studentId == null ? "" : studentId.trim().replaceAll("\\s+", "");
    }
}
