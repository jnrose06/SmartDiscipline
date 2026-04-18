package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Locale;

public class MySQLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_USERS = "users";
    public static final String TABLE_VIOLATIONS = "violations";
    public static final String TABLE_VIOLATION_TYPES = "violation_types";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "full_name";
    public static final String COLUMN_STUDENT_ID = "student_id";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_ROLE = "role";
    public static final String COLUMN_VIOLATION_TITLE = "title";
    public static final String COLUMN_VIOLATION_STATUS = "status";
    public static final String COLUMN_VIOLATION_MONTH = "month_label";
    public static final String COLUMN_VIOLATION_DAY = "day_label";
    public static final String COLUMN_VIOLATION_YEAR = "year_label";
    public static final String COLUMN_VIOLATION_TIME = "time_label";
    public static final String COLUMN_VIOLATION_SORT_KEY = "sort_key";
    public static final String COLUMN_IS_DELETED = "is_deleted";
    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_ADMIN = "admin";
    public static final String DEFAULT_ADMIN_ID = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private static final String DATABASE_NAME = "students.db";
    private static final int DATABASE_VERSION = 8;

    private static final String DATABASE_CREATE =
            "create table " + TABLE_USERS + "("
                    + COLUMN_ID + " integer primary key autoincrement, "
                    + COLUMN_NAME + " text not null, "
                    + COLUMN_STUDENT_ID + " text not null, "
                    + COLUMN_PASSWORD + " text not null, "
                    + COLUMN_ROLE + " text not null default '" + ROLE_STUDENT + "');";

    private static final String VIOLATIONS_TABLE_CREATE =
            "create table " + TABLE_VIOLATIONS + "("
                    + COLUMN_ID + " integer primary key autoincrement, "
                    + COLUMN_STUDENT_ID + " text not null, "
                    + COLUMN_VIOLATION_TITLE + " text not null, "
                    + COLUMN_VIOLATION_STATUS + " text not null, "
                    + COLUMN_VIOLATION_MONTH + " text not null, "
                    + COLUMN_VIOLATION_DAY + " text not null, "
                    + COLUMN_VIOLATION_YEAR + " text not null, "
                    + COLUMN_VIOLATION_TIME + " text not null, "
                    + COLUMN_VIOLATION_SORT_KEY + " integer not null, "
                    + COLUMN_IS_DELETED + " integer not null default 0);";

    private static final String VIOLATION_TYPES_TABLE_CREATE =
            "create table " + TABLE_VIOLATION_TYPES + "("
                    + COLUMN_ID + " integer primary key autoincrement, "
                    + COLUMN_VIOLATION_TITLE + " text not null unique);";

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        database.execSQL(VIOLATIONS_TABLE_CREATE);
        database.execSQL(VIOLATION_TYPES_TABLE_CREATE);
        insertDefaultAdmin(database);
        seedDefaultViolationTypes(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN "
                    + COLUMN_ROLE + " text not null default '" + ROLE_STUDENT + "'");
        }
        if (oldVersion < 4) {
            db.execSQL(VIOLATIONS_TABLE_CREATE);
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_VIOLATIONS + " ADD COLUMN "
                    + COLUMN_VIOLATION_TIME + " text not null default '12:00 AM'");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE " + TABLE_VIOLATIONS + " ADD COLUMN "
                    + COLUMN_IS_DELETED + " integer not null default 0");
        }
        if (oldVersion < 7) {
            db.execSQL("DELETE FROM " + TABLE_VIOLATIONS);
        }
        if (oldVersion < 8) {
            db.execSQL(VIOLATION_TYPES_TABLE_CREATE);
        }
        insertDefaultAdmin(db);
        seedDefaultViolationTypes(db);
    }

    public boolean insertUser(String name, String studentId, String password) {
        String normalizedStudentId = normalizeStudentId(studentId);
        SQLiteDatabase db = this.getWritableDatabase();
        if (isStudentIdExists(db, normalizedStudentId)) {
            db.close();
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name.trim());
        values.put(COLUMN_STUDENT_ID, normalizedStudentId);
        values.put(COLUMN_PASSWORD, password.trim());
        values.put(COLUMN_ROLE, ROLE_STUDENT);

        long rowId = db.insert(TABLE_USERS, null, values);
        db.close();
        return rowId != -1;
    }

    public boolean validateUser(String studentId, String password) {
        String normalizedStudentId = normalizeStudentId(studentId);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID + " FROM " + TABLE_USERS + " WHERE "
                        + "REPLACE(" + COLUMN_STUDENT_ID + ", ' ', '')=? AND "
                        + COLUMN_PASSWORD + "=?",
                new String[]{normalizedStudentId, password.trim()});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    public boolean checkUser(String studentId, String password) {
        return validateUser(studentId, password);
    }

    public boolean isStudentIdExists(String studentId) {
        String normalizedStudentId = normalizeStudentId(studentId);
        SQLiteDatabase db = this.getReadableDatabase();
        boolean exists = isStudentIdExists(db, normalizedStudentId);
        db.close();
        return exists;
    }

    public String getUserRole(String studentId, String password) {
        String normalizedStudentId = normalizeStudentId(studentId);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COALESCE(" + COLUMN_ROLE + ", ?) FROM " + TABLE_USERS
                        + " WHERE REPLACE(" + COLUMN_STUDENT_ID + ", ' ', '')=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{ROLE_STUDENT, normalizedStudentId, password.trim()});

        String role = null;
        if (cursor.moveToFirst()) {
            role = cursor.getString(0);
        }

        if (role == null && validateUser(normalizedStudentId, password)) {
            role = ROLE_STUDENT;
        }

        cursor.close();
        db.close();
        return role;
    }

    public String getUserRoleByStudentId(String studentId) {
        String normalizedStudentId = normalizeStudentId(studentId);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COALESCE(" + COLUMN_ROLE + ", ?) FROM " + TABLE_USERS
                        + " WHERE REPLACE(" + COLUMN_STUDENT_ID + ", ' ', '')=?",
                new String[]{ROLE_STUDENT, normalizedStudentId});

        String role = null;
        if (cursor.moveToFirst()) {
            role = cursor.getString(0);
        }

        cursor.close();
        db.close();
        return role;
    }

    public String getUserFullName(String studentId) {
        String normalizedStudentId = normalizeStudentId(studentId);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_NAME + " FROM " + TABLE_USERS
                        + " WHERE REPLACE(" + COLUMN_STUDENT_ID + ", ' ', '')=?",
                new String[]{normalizedStudentId});

        String fullName = null;
        if (cursor.moveToFirst()) {
            fullName = cursor.getString(0);
        }

        cursor.close();
        db.close();
        return fullName;
    }

    public void updateUserFullName(String studentId, String fullName) {
        String normalizedStudentId = normalizeStudentId(studentId);
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_USERS + " SET " + COLUMN_NAME + "=? WHERE REPLACE("
                        + COLUMN_STUDENT_ID + ", ' ', '')=?",
                new Object[]{fullName.trim(), normalizedStudentId});
        db.close();
    }

    public boolean updateUserPassword(String studentId, String newPassword) {
        String normalizedStudentId = normalizeStudentId(studentId);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PASSWORD, newPassword.trim());
        int rows = db.update(TABLE_USERS, values, "REPLACE(" + COLUMN_STUDENT_ID + ", ' ', '')=?",
                new String[]{normalizedStudentId});
        db.close();
        return rows > 0;
    }

    public boolean deleteUser(String studentId) {
        String normalizedStudentId = normalizeStudentId(studentId);
        if (DEFAULT_ADMIN_ID.equals(normalizedStudentId)) {
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        int violationRows = db.delete(TABLE_VIOLATIONS, "REPLACE(" + COLUMN_STUDENT_ID + ", ' ', '')=?",
                new String[]{normalizedStudentId});
        int userRows = db.delete(TABLE_USERS, "REPLACE(" + COLUMN_STUDENT_ID + ", ' ', '')=?",
                new String[]{normalizedStudentId});
        db.close();
        return userRows > 0 || violationRows > 0;
    }

    public void ensureSampleViolationsForStudent(String studentId) {
        String normalizedStudentId = normalizeStudentId(studentId);
        if (normalizedStudentId.isEmpty() || ROLE_ADMIN.equals(normalizedStudentId)) {
            return;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID + " FROM " + TABLE_VIOLATIONS
                        + " WHERE REPLACE(" + COLUMN_STUDENT_ID + ", ' ', '')=?",
                new String[]{normalizedStudentId});

        boolean hasViolations = cursor.moveToFirst();
        cursor.close();

        if (!hasViolations) {
            insertViolation(db, normalizedStudentId, "Late Attendance", "IN PROGRESS", "APRIL", "20", "2026", "10:15 AM", 202604201015L);
            insertViolation(db, normalizedStudentId, "Uniform Policy", "PENDING", "APRIL", "18", "2026", "09:30 AM", 202604180930L);
            insertViolation(db, normalizedStudentId, "Haircut Policy", "RESOLVED", "APRIL", "10", "2026", "01:45 PM", 202604101345L);
        }

        db.close();
    }

    public Cursor getViolationsForStudent(String studentId) {
        String normalizedStudentId = normalizeStudentId(studentId);
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT "
                        + COLUMN_ID + ", "
                        + COLUMN_VIOLATION_MONTH + ", "
                        + COLUMN_VIOLATION_DAY + ", "
                        + COLUMN_VIOLATION_YEAR + ", "
                        + COLUMN_VIOLATION_TIME + ", "
                        + COLUMN_VIOLATION_TITLE + ", "
                        + COLUMN_VIOLATION_STATUS + ", "
                        + COLUMN_IS_DELETED
                        + " FROM " + TABLE_VIOLATIONS
                        + " WHERE REPLACE(" + COLUMN_STUDENT_ID + ", ' ', '')=?"
                        + " ORDER BY " + COLUMN_VIOLATION_SORT_KEY + " DESC",
                new String[]{normalizedStudentId});
    }

    public Cursor getAllViolationsForAdmin() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT "
                        + "v." + COLUMN_ID + ", "
                        + "u." + COLUMN_NAME + ", "
                        + "v." + COLUMN_STUDENT_ID + ", "
                        + "v." + COLUMN_VIOLATION_TITLE + ", "
                        + "v." + COLUMN_VIOLATION_STATUS + ", "
                        + "v." + COLUMN_VIOLATION_MONTH + ", "
                        + "v." + COLUMN_VIOLATION_DAY + ", "
                        + "v." + COLUMN_VIOLATION_YEAR + ", "
                        + "v." + COLUMN_VIOLATION_TIME + ", "
                        + "v." + COLUMN_VIOLATION_SORT_KEY + ", "
                        + "v." + COLUMN_IS_DELETED
                        + " FROM " + TABLE_VIOLATIONS + " v"
                        + " INNER JOIN " + TABLE_USERS + " u"
                        + " ON REPLACE(u." + COLUMN_STUDENT_ID + ", ' ', '') = REPLACE(v." + COLUMN_STUDENT_ID + ", ' ', '')"
                        + " ORDER BY v." + COLUMN_VIOLATION_SORT_KEY + " DESC",
                null);
    }

    public Cursor getViolationsForAdminByStudent(String studentId, boolean showDeletedOnly) {
        String normalizedStudentId = normalizeStudentId(studentId);
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT "
                        + COLUMN_VIOLATION_TITLE + ", "
                        + COLUMN_VIOLATION_STATUS + ", "
                        + COLUMN_VIOLATION_MONTH + ", "
                        + COLUMN_VIOLATION_DAY + ", "
                        + COLUMN_VIOLATION_YEAR + ", "
                        + COLUMN_VIOLATION_TIME + ", "
                        + COLUMN_IS_DELETED
                        + " FROM " + TABLE_VIOLATIONS
                        + " WHERE REPLACE(" + COLUMN_STUDENT_ID + ", ' ', '')=?"
                        + " AND " + COLUMN_IS_DELETED + "=?"
                        + " ORDER BY " + COLUMN_VIOLATION_SORT_KEY + " DESC",
                new String[]{normalizedStudentId, showDeletedOnly ? "1" : "0"});
    }

    public boolean updateViolationStatus(long violationId, String newStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_VIOLATION_STATUS + " FROM " + TABLE_VIOLATIONS
                        + " WHERE " + COLUMN_ID + "=? AND " + COLUMN_IS_DELETED + "=0",
                new String[]{String.valueOf(violationId)});

        String currentStatus = null;
        if (cursor.moveToFirst()) {
            currentStatus = cursor.getString(0);
        }
        cursor.close();

        // Once a violation is marked resolved or in progress, lock it permanently.
        if (("RESOLVED".equalsIgnoreCase(currentStatus) || "IN PROGRESS".equalsIgnoreCase(currentStatus))
                && !newStatus.equalsIgnoreCase(currentStatus)) {
            db.close();
            return false;
        }

        // Once a violation is marked resolved or in progress, do not allow reverting it back to pending.
        if ("PENDING".equalsIgnoreCase(newStatus)
                && ("RESOLVED".equalsIgnoreCase(currentStatus) || "IN PROGRESS".equalsIgnoreCase(currentStatus))) {
            db.close();
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_VIOLATION_STATUS, newStatus.trim());
        int rows = db.update(TABLE_VIOLATIONS, values, COLUMN_ID + "=? AND " + COLUMN_IS_DELETED + "=0",
                new String[]{String.valueOf(violationId)});
        db.close();
        return rows > 0;
    }

    public boolean softDeleteViolation(long violationId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_DELETED, 1);
        int rows = db.update(TABLE_VIOLATIONS, values, COLUMN_ID + "=? AND " + COLUMN_IS_DELETED + "=0",
                new String[]{String.valueOf(violationId)});
        db.close();
        return rows > 0;
    }

    public boolean restoreViolation(long violationId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_DELETED, 0);
        int rows = db.update(TABLE_VIOLATIONS, values, COLUMN_ID + "=? AND " + COLUMN_IS_DELETED + "=1",
                new String[]{String.valueOf(violationId)});
        db.close();
        return rows > 0;
    }

    public Cursor getAllViolationTypes() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT " + COLUMN_VIOLATION_TITLE
                        + " FROM " + TABLE_VIOLATION_TYPES
                        + " ORDER BY " + COLUMN_VIOLATION_TITLE + " COLLATE NOCASE ASC",
                null);
    }

    public boolean addViolationType(String violationTitle) {
        String normalizedTitle = normalizeViolationTypeLabel(violationTitle);
        if (normalizedTitle.isEmpty()) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        boolean inserted = insertViolationTypeIfMissing(db, normalizedTitle);
        db.close();
        return inserted;
    }

    public boolean addViolationForStudent(String studentId, String title, String status,
                                          String month, String day, String year, String time, long sortKey) {
        String normalizedStudentId = normalizeStudentId(studentId);
        SQLiteDatabase db = this.getWritableDatabase();
        insertViolationTypeIfMissing(db, title);
        if (hasViolationForStudent(db, normalizedStudentId, title)) {
            db.close();
            return false;
        }
        insertViolation(db, normalizedStudentId, title.trim(), status.trim(),
                month.trim(), day.trim(), year.trim(), time.trim(), sortKey);
        db.close();
        return true;
    }

    private void insertDefaultAdmin(SQLiteDatabase database) {
        Cursor cursor = database.rawQuery(
                "SELECT " + COLUMN_ID + " FROM " + TABLE_USERS + " WHERE " + COLUMN_STUDENT_ID + "=?",
                new String[]{DEFAULT_ADMIN_ID}
        );

        boolean adminExists = cursor.moveToFirst();
        cursor.close();

        if (!adminExists) {
            database.execSQL("INSERT INTO " + TABLE_USERS + " (" +
                            COLUMN_NAME + ", " +
                            COLUMN_STUDENT_ID + ", " +
                            COLUMN_PASSWORD + ", " +
                            COLUMN_ROLE + ") VALUES (?, ?, ?, ?)",
                    new Object[]{"System Admin", DEFAULT_ADMIN_ID, DEFAULT_ADMIN_PASSWORD, ROLE_ADMIN});
        }
    }

    private void seedDefaultViolationTypes(SQLiteDatabase database) {
        insertViolationTypeIfMissing(database, "HAIRCUT");
        insertViolationTypeIfMissing(database, "UNIFORM");
        insertViolationTypeIfMissing(database, "LATE ATTENDANCE");
    }

    private String normalizeStudentId(String studentId) {
        return studentId == null ? "" : studentId.trim().replaceAll("\\s+", "");
    }

    private boolean isStudentIdExists(SQLiteDatabase database, String normalizedStudentId) {
        Cursor cursor = database.rawQuery("SELECT " + COLUMN_ID + " FROM " + TABLE_USERS
                        + " WHERE REPLACE(" + COLUMN_STUDENT_ID + ", ' ', '')=?",
                new String[]{normalizedStudentId});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    private boolean hasViolationForStudent(SQLiteDatabase database, String normalizedStudentId, String violationTitle) {
        String requestedCategory = normalizeViolationCategory(violationTitle);
        Cursor cursor = database.rawQuery("SELECT " + COLUMN_VIOLATION_TITLE + " FROM " + TABLE_VIOLATIONS
                        + " WHERE REPLACE(" + COLUMN_STUDENT_ID + ", ' ', '')=?",
                new String[]{normalizedStudentId});

        try {
            while (cursor.moveToNext()) {
                String existingCategory = normalizeViolationCategory(cursor.getString(0));
                if (requestedCategory.equals(existingCategory)) {
                    return true;
                }
            }
            return false;
        } finally {
            cursor.close();
        }
    }

    private String normalizeViolationCategory(String violationTitle) {
        String normalized = violationTitle == null ? "" : violationTitle.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("HAIRCUT")) {
            return "HAIRCUT";
        }
        if (normalized.contains("UNIFORM")) {
            return "UNIFORM";
        }
        if (normalized.contains("LATE")) {
            return "LATE ATTENDANCE";
        }
        return normalized;
    }

    private String normalizeViolationTypeLabel(String violationTitle) {
        return violationTitle == null ? "" : violationTitle.trim().toUpperCase(Locale.ROOT);
    }

    private boolean insertViolationTypeIfMissing(SQLiteDatabase database, String violationTitle) {
        String normalizedTitle = normalizeViolationTypeLabel(violationTitle);
        if (normalizedTitle.isEmpty()) {
            return false;
        }

        Cursor cursor = database.rawQuery("SELECT " + COLUMN_ID + " FROM " + TABLE_VIOLATION_TYPES
                        + " WHERE UPPER(" + COLUMN_VIOLATION_TITLE + ")=?",
                new String[]{normalizedTitle});
        boolean exists = cursor.moveToFirst();
        cursor.close();

        if (exists) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_VIOLATION_TITLE, normalizedTitle);
        return database.insert(TABLE_VIOLATION_TYPES, null, values) != -1;
    }

    private void insertViolation(SQLiteDatabase database, String studentId, String title, String status,
                                 String month, String day, String year, String time, long sortKey) {
        database.execSQL("INSERT INTO " + TABLE_VIOLATIONS + " ("
                        + COLUMN_STUDENT_ID + ", "
                        + COLUMN_VIOLATION_TITLE + ", "
                        + COLUMN_VIOLATION_STATUS + ", "
                        + COLUMN_VIOLATION_MONTH + ", "
                        + COLUMN_VIOLATION_DAY + ", "
                        + COLUMN_VIOLATION_YEAR + ", "
                        + COLUMN_VIOLATION_TIME + ", "
                        + COLUMN_VIOLATION_SORT_KEY + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{studentId, title, status, month, day, year, time, sortKey});
    }

    public boolean checkUserExist(String student_id){

        String normalizedStudentId = normalizeStudentId(student_id);
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE REPLACE(student_id, ' ', '') = ?",
                new String[]{normalizedStudentId});

        if(cursor.getCount()> 0){
            cursor.close();
            return true;
        }else{
            cursor.close();
            return false;
        }
    }
}
    
