package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends Activity {

    private ScrollView studentScrollView;
    private LinearLayout studentMenuPanel;
    private View sectionStudentDashboard;
    private View sectionStudentRecords;
    private LinearLayout studentRecordList;
    private TextView tvWelcome;
    private TextView tvStudentSubtitle;
    private TextView tvStudentId;
    private TextView tvUserTotalViolations;
    private TextView tvUserInProgress;
    private TextView tvUserResolved;
    private TextView tvUserUnresolved;
    private TextView tvEmptyState;

    private String currentStudentId;
    private String currentFullName;
    private final List<IncidentRecord> incidentRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bindViews();
        loadStudentIdentity();
        loadStudentViolations();
        setupMenu();
        renderStudentRecords();
        updateSummaryCards();
        showDashboardSection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        incidentRecords.clear();
        loadStudentViolations();
        renderStudentRecords();
        updateSummaryCards();
    }

    private void bindViews() {
        studentScrollView = findViewById(R.id.studentScrollView);
        studentMenuPanel = findViewById(R.id.studentMenuPanel);
        sectionStudentDashboard = findViewById(R.id.sectionStudentDashboard);
        sectionStudentRecords = findViewById(R.id.sectionStudentRecords);
        studentRecordList = findViewById(R.id.studentRecordList);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvStudentSubtitle = findViewById(R.id.tvStudentSubtitle);
        tvStudentId = findViewById(R.id.tvStudentId);
        tvUserTotalViolations = findViewById(R.id.tvUserTotalViolations);
        tvUserInProgress = findViewById(R.id.tvUserInProgress);
        tvUserResolved = findViewById(R.id.tvUserResolved);
        tvUserUnresolved = findViewById(R.id.tvUserUnresolved);
        tvEmptyState = findViewById(R.id.tvEmptyState);
    }

    private void loadStudentIdentity() {
        currentStudentId = getIntent().getStringExtra("student_id");
        currentFullName = getIntent().getStringExtra("full_name");

        if (currentStudentId == null || currentStudentId.trim().isEmpty()) {
            currentStudentId = "N/A";
        }
        if (currentFullName == null || currentFullName.trim().isEmpty()) {
            currentFullName = "Student";
        }

        tvWelcome.setText("Welcome, " + currentFullName);
        tvStudentSubtitle.setText("Track your discipline history and stay updated on every report.");
        tvStudentId.setText("Student ID: " + currentStudentId);
    }

    private void setupMenu() {
        findViewById(R.id.btnStudentMenuToggle).setOnClickListener(v -> toggleMenu());

        findViewById(R.id.btnStudentMenuDashboard).setOnClickListener(v -> {
            studentMenuPanel.setVisibility(View.GONE);
            showDashboardSection();
        });

        findViewById(R.id.btnStudentMenuRecords).setOnClickListener(v -> {
            studentMenuPanel.setVisibility(View.GONE);
            showRecordsSection();
        });

        findViewById(R.id.btnStudentLogout).setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btnBackToDashboard).setOnClickListener(v -> showDashboardSection());
    }

    private void toggleMenu() {
        studentMenuPanel.setVisibility(
                studentMenuPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
        );
    }

    private void scrollToSection(View section) {
        studentScrollView.post(() -> studentScrollView.smoothScrollTo(0, section.getTop()));
    }

    private void showDashboardSection() {
        sectionStudentDashboard.setVisibility(View.VISIBLE);
        sectionStudentRecords.setVisibility(View.GONE);
        scrollToSection(sectionStudentDashboard);
    }

    private void showRecordsSection() {
        sectionStudentDashboard.setVisibility(View.GONE);
        sectionStudentRecords.setVisibility(View.VISIBLE);
        scrollToSection(sectionStudentRecords);
    }

    private void loadStudentViolations() {
        incidentRecords.clear();
        MySQLiteHelper dbHelper = new MySQLiteHelper(this);

        Cursor cursor = dbHelper.getViolationsForStudent(currentStudentId);
        try {
            while (cursor.moveToNext()) {
                boolean isDeleted = cursor.getInt(7) == 1;
                incidentRecords.add(new IncidentRecord(
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        isDeleted ? "DELETED BY ADMIN" : cursor.getString(6)
                ));
            }
        } finally {
            cursor.close();
        }
    }

    private void renderStudentRecords() {
        studentRecordList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (IncidentRecord record : incidentRecords) {
            View row = inflater.inflate(R.layout.item_user_record_detail, studentRecordList, false);
            ((TextView) row.findViewById(R.id.tvDetailMonth)).setText(record.month);
            ((TextView) row.findViewById(R.id.tvDetailDay)).setText(record.day);
            ((TextView) row.findViewById(R.id.tvDetailTitle)).setText(record.title);
            ((TextView) row.findViewById(R.id.tvDetailDate)).setText(
                    record.month + " " + record.day + ", " + record.year + " at " + record.time
            );

            TextView tvStatus = row.findViewById(R.id.tvDetailStatus);
            tvStatus.setText(record.status);
            tvStatus.setBackgroundResource(getStatusBackground(record.status));

            studentRecordList.addView(row);
        }

        tvEmptyState.setVisibility(incidentRecords.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateSummaryCards() {
        int total = incidentRecords.size();
        int inProgress = 0;
        int resolved = 0;
        int unresolved = 0;

        for (IncidentRecord record : incidentRecords) {
            if ("IN PROGRESS".equals(record.status) || "PENDING".equals(record.status)) {
                inProgress++;
            }
            if ("RESOLVED".equals(record.status)) {
                resolved++;
            } else if (!"DELETED BY ADMIN".equals(record.status)) {
                unresolved++;
            }
        }

        tvUserTotalViolations.setText(formatCount(total));
        tvUserInProgress.setText(formatCount(inProgress));
        tvUserResolved.setText(formatCount(resolved));
        tvUserUnresolved.setText(formatCount(unresolved));
    }

    private int getStatusBackground(String status) {
        if ("RESOLVED".equals(status)) {
            return R.drawable.user_status_resolved_background;
        }
        if ("PENDING".equals(status)) {
            return R.drawable.user_status_pending_background;
        }
        if ("DELETED BY ADMIN".equals(status)) {
            return R.drawable.user_status_pending_background;
        }
        return R.drawable.user_status_progress_background;
    }

    private String formatCount(int count) {
        return count < 10 ? "0" + count : String.valueOf(count);
    }

    private static class IncidentRecord {
        final String month;
        final String day;
        final String year;
        final String time;
        final String title;
        final String status;

        IncidentRecord(String month, String day, String year, String time, String title, String status) {
            this.month = month;
            this.day = day;
            this.year = year;
            this.time = time;
            this.title = title;
            this.status = status;
        }
    }
}
