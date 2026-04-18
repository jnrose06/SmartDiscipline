package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
    private static final String STUDENT_ID_PATTERN = "\\d+";

    EditText fullName, studentId, password, confirmPassword;
    Button btnRegister;
    TextView loginLink;
    ImageView togglePasswordVisibility, toggleConfirmPasswordVisibility;
    MySQLiteHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fullName = findViewById(R.id.full_name);
        studentId = findViewById(R.id.student_id);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirm_password);
        togglePasswordVisibility = findViewById(R.id.toggle_password_visibility);
        toggleConfirmPasswordVisibility = findViewById(R.id.toggle_confirm_password_visibility);
        btnRegister = findViewById(R.id.btn_register);
        loginLink = findViewById(R.id.login_link);
        dbHelper = new MySQLiteHelper(MainActivity.this);
        setupPasswordToggle(password, togglePasswordVisibility);
        setupPasswordToggle(confirmPassword, toggleConfirmPasswordVisibility);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = fullName.getText().toString().trim();
                String id = normalizeStudentId(studentId.getText().toString());
                String pass = password.getText().toString().trim();
                String confirmPass = confirmPassword.getText().toString().trim();

                if(name.isEmpty() || id.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()){

                    Toast.makeText(MainActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                } else if (!isValidStudentId(id)) {
                    Toast.makeText(MainActivity.this, "Student ID must contain numbers only", Toast.LENGTH_SHORT).show();
                } else if (id.length() > 10) {
                    Toast.makeText(MainActivity.this, "Student ID must be 10 characters or less", Toast.LENGTH_SHORT).show();
                } else if (!pass.equals(confirmPass)) {
                    Toast.makeText(MainActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                } else {
                    boolean isRegistered = dbHelper.insertUser(name, id, pass);

                    if (isRegistered) {
                        Toast.makeText(MainActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Student ID already exists", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
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
