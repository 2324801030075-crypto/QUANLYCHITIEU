package com.example.quanlychitieu;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText edtUsername, edtEmail, edtPhone;
    private Button btnSave, btnChangePassword;
    private TextView btnBack;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        String dbUrl = "https://dbqlchitieu-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(currentUser.getUid()).child("profile");

        edtUsername = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        btnSave = findViewById(R.id.btnSave);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnBack = findViewById(R.id.btnBack);

        loadUserProfile();

        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveProfileChanges());

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void loadUserProfile() {
        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    edtUsername.setText(user.getName());
                    edtEmail.setText(user.getEmail());
                    edtPhone.setText(user.getPhone());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveProfileChanges() {
        String name = edtUsername.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ Tên và Số điện thoại!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);

        mDatabase.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        
        EditText etOldPass = view.findViewById(R.id.etOldPassword);
        EditText etNewPass = view.findViewById(R.id.etNewPassword);
        EditText etConfirmPass = view.findViewById(R.id.etConfirmNewPassword);
        Button btnSaveNew = view.findViewById(R.id.btnSaveNewPassword);

        AlertDialog dialog = builder.setView(view).create();

        btnSaveNew.setOnClickListener(v -> {
            String oldP = etOldPass.getText().toString();
            String newP = etNewPass.getText().toString();
            String confP = etConfirmPass.getText().toString();

            if (TextUtils.isEmpty(oldP) || TextUtils.isEmpty(newP) || TextUtils.isEmpty(confP)) {
                Toast.makeText(this, "Vui lòng điền đủ thông tin mật khẩu!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newP.length() < 6) {
                Toast.makeText(this, "Mật khẩu mới phải ít nhất 6 ký tự!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newP.equals(confP)) {
                Toast.makeText(this, "Xác nhận mật khẩu mới không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), oldP);
            currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    currentUser.updatePassword(newP).addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi đổi mật khẩu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Toast.makeText(this, "Mật khẩu hiện tại không đúng!", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }
}
