package com.example.quanlychitieu;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        String dbUrl = "https://dbqlchitieu-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference();

        if (mAuth.getCurrentUser() != null) {
            goToMain();
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> handleLogin());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập Email và Mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        goToMain();
                    } else {

                        if (email.equals("test@gmail.com")) {
                            autoRegisterTestUser(email, password);
                        } else {
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Sai email hoặc mật khẩu";
                            Toast.makeText(LoginActivity.this, "Lỗi: " + errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void autoRegisterTestUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        User newUser = new User(user.getUid(), "Người dùng Test", email);
                        newUser.setPhone("0123456789");
                        mDatabase.child("users").child(user.getUid()).child("profile").setValue(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Đã khởi tạo tài khoản Test!", Toast.LENGTH_SHORT).show();
                                    goToMain();
                                });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Không thể tạo tài khoản test: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
