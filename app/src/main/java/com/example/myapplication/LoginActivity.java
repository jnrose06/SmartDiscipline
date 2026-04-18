package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {
    private static final String STUDENT_ID_PATTERN = "\\d+";

    EditText student_id, password;
    Button btn_login;
    TextView btn_go_register;
    ImageView togglePasswordVisibility;

    MySQLiteHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        student_id = findViewById(R.id.student_id);
        password = findViewById(R.id.password);
        togglePasswordVisibility = findViewById(R.id.toggle_password_visibility);
        btn_login = findViewById(R.id.btn_login);
        btn_go_register = findViewById(R.id.btn_go_register);

        dbHelper = new MySQLiteHelper(this);
        setupPasswordToggle(password, togglePasswordVisibility);

        // LOGIN BUTTON
        btn_login.setOnClickListener(v -> {

            String id = normalizeStudentId(student_id.getText().toString());
            String pass = password.getText().toString().trim();

            if(id.isEmpty() || pass.isEmpty()){
                Toast.makeText(LoginActivity.this,"Please enter ID and Password",Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidStudentId(id)) {
                Toast.makeText(LoginActivity.this, "Student ID must contain numbers only", Toast.LENGTH_SHORT).show();
                return;
            }

            String role = dbHelper.getUserRole(id, pass);

            if(role != null){
                Toast.makeText(LoginActivity.this,"Login Successful",Toast.LENGTH_SHORT).show();

                Intent intent;
                if (MySQLiteHelper.ROLE_ADMIN.equals(role)) {
                    intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                } else {
                    intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    intent.putExtra("full_name", dbHelper.getUserFullName(id));
                }
                intent.putExtra("student_id", id);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this,"Invalid Student ID or Password",Toast.LENGTH_SHORT).show();
            }

        });

        // GO TO REGISTER
        btn_go_register.setOnClickListener(v -> {

            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);

        });
    }

    private String normalizeStudentId(String studentIdValue) {
        return studentIdValue == null ? "" : studentIdValue.trim().replaceAll("\\s+", "");
    }

    private boolean isValidStudentId(String studentIdValue) {
        return studentIdValue.matches(STUDENT_ID_PATTERN);
    }

    private void setupPasswordToggle(EditText passwordField, ImageView toggleView) {
        toggleView.setOnClickListener(v -> {
            boolean isPasswordHidden = passwordField.getTransformationMethod() instanceof PasswordTransformationMethod;
            if (isPasswordHidden) {
                passwordField.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                passwordField.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            passwordField.setSelection(passwordField.getText().length());
        });
    }
}
