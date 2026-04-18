package com.example.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminDashboardActivity extends Activity {

    private static final String STATUS_ALL = "All";
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_SOLVED = "Solved";
    private static final String STATUS_UNSOLVED = "Unsolved";
    private ScrollView adminScrollView;
    private LinearLayout menuPanel;
    private LinearLayout sectionDashboard;
    private LinearLayout sectionViolationTypes;
    private LinearLayout sectionStudentRecords;
    private LinearLayout sectionDeleteHistory;
    private LinearLayout violationTypeContainer;
    private LinearLayout recordsContainer;
    private LinearLayout deletedRecordsContainer;
    private TextView tvAdminSubtitle;
    private TextView tvTotalReports;
    private TextView tvPendingReview;
    private TextView tvSolvedCases;
    private TextView tvUnsolvedCases;
    private TextView tvEmptyState;
    private TextView tvDeletedEmptyState;
    private TextView tvViolationTypeEmptyState;
    private EditText etSearchStudent;
    private MySQLiteHelper dbHelper;

    private final List<StudentRecord> allRecords = new ArrayList<>();
    private String currentStatusFilter = STATUS_ALL;
    private boolean sortLatestFirst = true;
    private int newEntryCount = 1;
    private boolean showingDashboard = true;
    private boolean showingViolationSection = false;
    private boolean showingDeleteHistory = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        dbHelper = new MySQLiteHelper(this);
        bindViews();
        setupHeader();
        loadRecordsFromDatabase();
        setupMenuActions();
        setupBottomNavigation();
        setupFilters();
        renderRecords();
        showDashboardSection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecordsFromDatabase();
        renderRecords();
    }

    private void bindViews() {
        adminScrollView = findViewById(R.id.adminScrollView);
        menuPanel = findViewById(R.id.menuPanel);
        sectionDashboard = findViewById(R.id.sectionDashboard);
        sectionViolationTypes = findViewById(R.id.sectionViolationTypes);
        sectionStudentRecords = findViewById(R.id.sectionStudentRecords);
        sectionDeleteHistory = findViewById(R.id.sectionDeleteHistory);
        violationTypeContainer = findViewById(R.id.violationTypeContainer);
        recordsContainer = findViewById(R.id.recordsContainer);
        deletedRecordsContainer = findViewById(R.id.deletedRecordsContainer);
        tvAdminSubtitle = findViewById(R.id.tvAdminSubtitle);
        tvTotalReports = findViewById(R.id.tvTotalReports);
        tvPendingReview = findViewById(R.id.tvPendingReview);
        tvSolvedCases = findViewById(R.id.tvSolvedCases);
        tvUnsolvedCases = findViewById(R.id.tvUnsolvedCases);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvDeletedEmptyState = findViewById(R.id.tvDeletedEmptyState);
        tvViolationTypeEmptyState = findViewById(R.id.tvViolationTypeEmptyState);
        etSearchStudent = findViewById(R.id.etSearchStudent);
    }

    private void setupHeader() {
        tvAdminSubtitle.setText("Signed in as administrator");
    }

    private void setupMenuActions() {
        findViewById(R.id.btnMenuToggle).setOnClickListener(v -> toggleMenu());

        findViewById(R.id.btnMenuDashboard).setOnClickListener(v -> {
            menuPanel.setVisibility(View.GONE);
            showDashboardSection();
        });

        findViewById(R.id.btnMenuStudents).setOnClickListener(v -> {
            menuPanel.setVisibility(View.GONE);
            openStudentRecordsActivity();
        });

        findViewById(R.id.btnMenuViolations).setOnClickListener(v -> {
            menuPanel.setVisibility(View.GONE);
            showViolationSection();
        });

        findViewById(R.id.btnMenuHistoryRecords).setOnClickListener(v -> {
            menuPanel.setVisibility(View.GONE);
            showHistoryRecordsSection();
        });

        findViewById(R.id.btnMenuDeleteHistory).setOnClickListener(v -> {
            menuPanel.setVisibility(View.GONE);
            showDeleteHistorySection();
        });

        findViewById(R.id.btnMenuLogout).setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, LandingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navDashboard).setOnClickListener(v -> showDashboardSection());
        findViewById(R.id.navViolations).setOnClickListener(v -> showViolationSection());
        findViewById(R.id.navStudents).setOnClickListener(v -> openStudentRecordsActivity());
    }

    private void setupFilters() {
        etSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                renderRecords();
            }
        });

        findViewById(R.id.btnStatusFilter).setOnClickListener(v -> {
            switch (currentStatusFilter) {
                case STATUS_ALL:
                    currentStatusFilter = STATUS_PENDING;
                    break;
                case STATUS_PENDING:
                    currentStatusFilter = STATUS_SOLVED;
                    break;
                case STATUS_SOLVED:
                    currentStatusFilter = STATUS_UNSOLVED;
                    break;
                default:
                    currentStatusFilter = STATUS_ALL;
                    break;
            }
            ((TextView) findViewById(R.id.btnStatusFilter)).setText(STATUS_ALL.equals(currentStatusFilter) ? "Status" : currentStatusFilter);
            renderRecords();
        });

        findViewById(R.id.btnDateSort).setOnClickListener(v -> {
            sortLatestFirst = !sortLatestFirst;
            ((TextView) findViewById(R.id.btnDateSort)).setText(sortLatestFirst ? "Date Range" : "Oldest First");
            renderRecords();
        });

        findViewById(R.id.btnBackToHistoryRecords).setOnClickListener(v -> showHistoryRecordsSection());
        findViewById(R.id.btnAddViolationType).setOnClickListener(v -> showAddViolationTypeDialog());
    }

    private void toggleMenu() {
        if (menuPanel.getVisibility() == View.VISIBLE) {
            menuPanel.setVisibility(View.GONE);
        } else {
            menuPanel.setVisibility(View.VISIBLE);
        }
    }

    private void scrollToSection(View section) {
        adminScrollView.post(() -> adminScrollView.smoothScrollTo(0, section.getTop()));
    }

    private void showDashboardSection() {
        showingDashboard = true;
        showingViolationSection = false;
        showingDeleteHistory = false;
        sectionDashboard.setVisibility(View.VISIBLE);
        sectionViolationTypes.setVisibility(View.GONE);
        sectionStudentRecords.setVisibility(View.GONE);
        sectionDeleteHistory.setVisibility(View.GONE);
        scrollToSection(sectionDashboard);
    }

    private void showViolationSection() {
        showingDashboard = false;
        showingViolationSection = true;
        showingDeleteHistory = false;
        sectionDashboard.setVisibility(View.GONE);
        sectionViolationTypes.setVisibility(View.VISIBLE);
        sectionStudentRecords.setVisibility(View.GONE);
        sectionDeleteHistory.setVisibility(View.GONE);
        scrollToSection(sectionViolationTypes);
    }

    private void showHistoryRecordsSection() {
        showingDashboard = false;
        showingViolationSection = false;
        showingDeleteHistory = false;
        sectionDashboard.setVisibility(View.GONE);
        sectionViolationTypes.setVisibility(View.GONE);
        sectionStudentRecords.setVisibility(View.VISIBLE);
        sectionDeleteHistory.setVisibility(View.GONE);
        scrollToSection(sectionStudentRecords);
    }

    private void showDeleteHistorySection() {
        showingDashboard = false;
        showingViolationSection = false;
        showingDeleteHistory = true;
        sectionDashboard.setVisibility(View.GONE);
        sectionViolationTypes.setVisibility(View.GONE);
        sectionStudentRecords.setVisibility(View.GONE);
        sectionDeleteHistory.setVisibility(View.VISIBLE);
        scrollToSection(sectionDeleteHistory);
    }

    private void openStudentRecordsActivity() {
        Intent intent = new Intent(AdminDashboardActivity.this, StudentRecordActivity.class);
        startActivity(intent);
    }

    private void loadRecordsFromDatabase() {
        allRecords.clear();
        newEntryCount = 1;
        Cursor cursor = dbHelper.getAllViolationsForAdmin();
        try {
            while (cursor.moveToNext()) {
                long violationId = cursor.getLong(0);
                String fullName = cursor.getString(1);
                String studentId = cursor.getString(2);
                String violationTitle = cursor.getString(3);
                String studentStatus = cursor.getString(4);
                String month = cursor.getString(5);
                String day = cursor.getString(6);
                String year = cursor.getString(7);
                String time = cursor.getString(8);
                long sortKey = cursor.getLong(9);
                boolean isSoftDeleted = cursor.getInt(10) == 1;

                allRecords.add(new StudentRecord(
                        violationId,
                        fullName == null || fullName.trim().isEmpty() ? "STUDENT" : fullName.toUpperCase(Locale.ROOT),
                        "ID:" + studentId,
                        studentId,
                        violationTitle,
                        month + " " + day + ", " + year + "\n" + time,
                        isSoftDeleted ? " Deleted" : mapStatusForAdmin(studentStatus),
                        sortKey,
                        getAvatarResId(newEntryCount),
                        isSoftDeleted
                ));
                newEntryCount++;
            }
        } finally {
            cursor.close();
        }
    }

    private void renderRecords() {
        List<StudentRecord> visibleActiveRecords = getFilteredRecords(false);
        List<StudentRecord> visibleDeletedRecords = getFilteredRecords(true);
        recordsContainer.removeAllViews();
        deletedRecordsContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (StudentRecord record : visibleActiveRecords) {
            View itemView = createRecordItemView(inflater, recordsContainer, record);
            recordsContainer.addView(itemView);
        }

        for (StudentRecord record : visibleDeletedRecords) {
            View itemView = createRecordItemView(inflater, deletedRecordsContainer, record);
            deletedRecordsContainer.addView(itemView);
        }

        tvEmptyState.setVisibility(visibleActiveRecords.isEmpty() ? View.VISIBLE : View.GONE);
        tvDeletedEmptyState.setVisibility(visibleDeletedRecords.isEmpty() ? View.VISIBLE : View.GONE);
        updateMetricCards();
        renderViolationTypes();

        if (showingDashboard) {
            sectionDashboard.setVisibility(View.VISIBLE);
            sectionViolationTypes.setVisibility(View.GONE);
            sectionStudentRecords.setVisibility(View.GONE);
            sectionDeleteHistory.setVisibility(View.GONE);
        } else if (showingViolationSection) {
            sectionDashboard.setVisibility(View.GONE);
            sectionViolationTypes.setVisibility(View.VISIBLE);
            sectionStudentRecords.setVisibility(View.GONE);
            sectionDeleteHistory.setVisibility(View.GONE);
        } else if (showingDeleteHistory) {
            sectionDashboard.setVisibility(View.GONE);
            sectionViolationTypes.setVisibility(View.GONE);
            sectionStudentRecords.setVisibility(View.GONE);
            sectionDeleteHistory.setVisibility(View.VISIBLE);
        } else {
            sectionDashboard.setVisibility(View.GONE);
            sectionViolationTypes.setVisibility(View.GONE);
            sectionStudentRecords.setVisibility(View.VISIBLE);
            sectionDeleteHistory.setVisibility(View.GONE);
        }
    }

    private View createRecordItemView(LayoutInflater inflater, LinearLayout parent, StudentRecord record) {
        View itemView = inflater.inflate(R.layout.item_student_record, parent, false);

        View avatarView = itemView.findViewById(R.id.avatarView);
        TextView tvStudentName = itemView.findViewById(R.id.tvStudentName);
        TextView tvStudentId = itemView.findViewById(R.id.tvStudentId);
        TextView tvViolationType = itemView.findViewById(R.id.tvViolationType);
        TextView tvDate = itemView.findViewById(R.id.tvDate);
        TextView tvStatus = itemView.findViewById(R.id.tvStatus);
        TextView btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
        TextView btnSoftDelete = itemView.findViewById(R.id.btnSoftDelete);

        avatarView.setVisibility(View.GONE);
        tvStudentName.setText(record.studentName);
        tvStudentId.setText(record.studentId);
        tvViolationType.setText(record.violationType);
        tvDate.setText(record.dateLabel);
        tvStatus.setText(record.status);

        if (record.isSoftDeleted) {
            btnUpdateStatus.setText("Restore");
            btnSoftDelete.setText("Deleted");
            btnUpdateStatus.setEnabled(true);
            btnUpdateStatus.setAlpha(1f);
            btnSoftDelete.setEnabled(false);
            btnSoftDelete.setAlpha(0.5f);
        } else if (STATUS_SOLVED.equals(record.status) || STATUS_UNSOLVED.equals(record.status)) {
            btnUpdateStatus.setEnabled(false);
            btnUpdateStatus.setAlpha(0.5f);
            btnSoftDelete.setEnabled(true);
            btnSoftDelete.setAlpha(1f);
        } else {
            btnUpdateStatus.setEnabled(true);
            btnUpdateStatus.setAlpha(1f);
            btnSoftDelete.setEnabled(true);
            btnSoftDelete.setAlpha(1f);
        }

        btnUpdateStatus.setOnClickListener(v -> {
            if (record.isSoftDeleted) {
                showRestoreDialog(record);
            } else {
                showStatusPickerDialog(record);
            }
        });
        btnSoftDelete.setOnClickListener(v -> showSoftDeleteDialog(record));
        itemView.setOnClickListener(v -> showStudentViolationsDialog(record));
        return itemView;
    }

    private void renderViolationTypes() {
        violationTypeContainer.removeAllViews();
        Map<String, Integer> violationCounts = new LinkedHashMap<>();

        Cursor typeCursor = dbHelper.getAllViolationTypes();
        try {
            while (typeCursor.moveToNext()) {
                violationCounts.put(typeCursor.getString(0), 0);
            }
        } finally {
            typeCursor.close();
        }

        for (StudentRecord record : allRecords) {
            if (record.isSoftDeleted) {
                continue;
            }
            String key = normalizeViolationType(record.violationType);
            if (violationCounts.containsKey(key)) {
                violationCounts.put(key, violationCounts.get(key) + 1);
            }
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map.Entry<String, Integer> entry : violationCounts.entrySet()) {
            View row = inflater.inflate(R.layout.item_violation_type, violationTypeContainer, false);
            ((TextView) row.findViewById(R.id.tvViolationTypeName)).setText(entry.getKey());
            ((TextView) row.findViewById(R.id.tvViolationTypeCount)).setText(entry.getValue() + " record(s)");
            violationTypeContainer.addView(row);
        }

        tvViolationTypeEmptyState.setVisibility(violationCounts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private List<StudentRecord> getFilteredRecords(boolean showDeletedOnly) {
        String query = etSearchStudent.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<StudentRecord> filtered = new ArrayList<>();

        for (StudentRecord record : allRecords) {
            if (record.isSoftDeleted != showDeletedOnly) {
                continue;
            }

            boolean statusMatches = showDeletedOnly
                    || STATUS_ALL.equals(currentStatusFilter)
                    || currentStatusFilter.equals(record.status);
            boolean searchMatches = query.isEmpty()
                    || record.studentName.toLowerCase(Locale.ROOT).contains(query)
                    || record.studentId.toLowerCase(Locale.ROOT).contains(query)
                    || record.rawStudentId.toLowerCase(Locale.ROOT).contains(query)
                    || record.violationType.toLowerCase(Locale.ROOT).contains(query);

            if (statusMatches && searchMatches) {
                filtered.add(record);
            }
        }

        Collections.sort(filtered, new Comparator<StudentRecord>() {
            @Override
            public int compare(StudentRecord first, StudentRecord second) {
                return sortLatestFirst
                        ? Long.compare(second.sortKey, first.sortKey)
                        : Long.compare(first.sortKey, second.sortKey);
            }
        });
        return filtered;
    }

    private void updateMetricCards() {
        int total = 0;
        int pending = 0;
        int solved = 0;
        int unsolved = 0;

        for (StudentRecord record : allRecords) {
            if (record.isSoftDeleted) {
                continue;
            }

            total++;
            switch (record.status) {
                case STATUS_PENDING:
                    pending++;
                    break;
                case STATUS_SOLVED:
                    solved++;
                    break;
                case STATUS_UNSOLVED:
                    unsolved++;
                    break;
                default:
                    break;
            }
        }

        tvTotalReports.setText(String.valueOf(total));
        tvPendingReview.setText(String.valueOf(pending));
        tvSolvedCases.setText(String.valueOf(solved));
        tvUnsolvedCases.setText(String.valueOf(unsolved));
    }

    private int getAvatarResId(int index) {
        int remainder = index % 3;
        if (remainder == 1) {
            return R.drawable.admin_avatar_tint_one;
        } else if (remainder == 2) {
            return R.drawable.admin_avatar_tint_two;
        }
        return R.drawable.admin_avatar_tint_three;
    }

    private String mapStatusForAdmin(String studentStatus) {
        if ("RESOLVED".equalsIgnoreCase(studentStatus)) {
            return STATUS_SOLVED;
        }
        if ("IN PROGRESS".equalsIgnoreCase(studentStatus)) {
            return STATUS_UNSOLVED;
        }
        return STATUS_PENDING;
    }

    private String normalizeViolationType(String violationType) {
        return violationType == null ? "" : violationType.trim().toUpperCase(Locale.ROOT);
    }

    private String mapStatusForStudent(String adminStatus) {
        if (STATUS_SOLVED.equals(adminStatus)) {
            return "RESOLVED";
        }
        if (STATUS_UNSOLVED.equals(adminStatus)) {
            return "IN PROGRESS";
        }
        return "PENDING";
    }

    private void showAddViolationTypeDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter new violation type");
        input.setSingleLine();

        new AlertDialog.Builder(this)
                .setTitle("Add Violation Type")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(this, "Violation type is required.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean added = dbHelper.addViolationType(value);
                    if (!added) {
                        Toast.makeText(this, "Violation type already exists or is invalid.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Toast.makeText(this, "Violation type added.", Toast.LENGTH_SHORT).show();
                    renderRecords();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSoftDeleteDialog(StudentRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record")
                .setMessage("Delete this violation record for " + record.studentName + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    boolean deleted = dbHelper.softDeleteViolation(record.violationId);
                    if (deleted) {
                        loadRecordsFromDatabase();
                        renderRecords();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStatusPickerDialog(StudentRecord record) {
        if (STATUS_SOLVED.equals(record.status) || STATUS_UNSOLVED.equals(record.status)) {
            return;
        }

        String[] options;
        if (STATUS_PENDING.equals(record.status)) {
            options = new String[]{STATUS_PENDING, STATUS_SOLVED, STATUS_UNSOLVED};
        } else {
            options = new String[]{STATUS_SOLVED, STATUS_UNSOLVED};
        }

        int checkedItem = 0;
        if (STATUS_SOLVED.equals(record.status)) {
            checkedItem = options.length == 3 ? 1 : 0;
        } else if (STATUS_UNSOLVED.equals(record.status)) {
            checkedItem = options.length == 3 ? 2 : 1;
        }

        final int[] selectedIndex = {checkedItem};
        new AlertDialog.Builder(this)
                .setTitle("Update Status")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> selectedIndex[0] = which)
                .setPositiveButton("Save", (dialog, which) -> {
                    String selectedStatus = options[selectedIndex[0]];
                    boolean updated = dbHelper.updateViolationStatus(
                            record.violationId,
                            mapStatusForStudent(selectedStatus)
                    );
                    if (updated) {
                        loadRecordsFromDatabase();
                        renderRecords();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRestoreDialog(StudentRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("Restore Record")
                .setMessage("Restore this violation record for " + record.studentName + "?")
                .setPositiveButton("Restore", (dialog, which) -> {
                    boolean restored = dbHelper.restoreViolation(record.violationId);
                    if (restored) {
                        loadRecordsFromDatabase();
                        renderRecords();
                        showHistoryRecordsSection();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showStudentViolationsDialog(StudentRecord record) {
        Cursor cursor = dbHelper.getViolationsForAdminByStudent(record.rawStudentId, record.isSoftDeleted);
        StringBuilder message = new StringBuilder();
        try {
            int index = 1;
            while (cursor.moveToNext()) {
                String title = cursor.getString(0);
                String status = mapStatusForAdmin(cursor.getString(1));
                String month = cursor.getString(2);
                String day = cursor.getString(3);
                String year = cursor.getString(4);
                String time = cursor.getString(5);
                boolean isDeleted = cursor.getInt(6) == 1;

                message.append(index)
                        .append(". ")
                        .append(title)
                        .append(" - ")
                        .append(isDeleted ? "Soft Deleted" : status)
                        .append("\n")
                        .append(month).append(" ").append(day).append(", ").append(year).append(" ").append(time)
                        .append("\n\n");
                index++;
            }
        } finally {
            cursor.close();
        }

        if (message.length() == 0) {
            message.append("No violations found for this student.");
        }

        new AlertDialog.Builder(this)
                .setTitle((record.isSoftDeleted ? "Delete History: " : "History Records: ")
                        + record.studentName + " (" + record.studentId + ")")
                .setMessage(message.toString().trim())
                .setPositiveButton("Close", null)
                .show();
    }

    private static class StudentRecord {
        final long violationId;
        final String studentName;
        final String studentId;
        final String rawStudentId;
        final String violationType;
        final String dateLabel;
        final String status;
        final long sortKey;
        final int avatarResId;
        final boolean isSoftDeleted;

        StudentRecord(long violationId, String studentName, String studentId, String rawStudentId, String violationType,
                      String dateLabel, String status, long sortKey, int avatarResId, boolean isSoftDeleted) {
            this.violationId = violationId;
            this.studentName = studentName;
            this.studentId = studentId;
            this.rawStudentId = rawStudentId;
            this.violationType = violationType;
            this.dateLabel = dateLabel;
            this.status = status;
            this.sortKey = sortKey;
            this.avatarResId = avatarResId;
            this.isSoftDeleted = isSoftDeleted;
        }
    }
}
