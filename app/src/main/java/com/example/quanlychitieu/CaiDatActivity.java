package com.example.quanlychitieu;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CaiDatActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserEmail, tvUserPhone, btnBack;
    private Button btnEditProfile, btnLogout;
    private LinearLayout itemAddress, itemSetting, itemHelp, itemContact;
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cai_dat);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        String userId = (user != null) ? user.getUid() : "test_user_123";

        String dbUrl = "https://dbqlchitieu-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(userId).child("profile");

        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserPhone = findViewById(R.id.tvUserPhone);
        btnBack = findViewById(R.id.btnBack);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNav = findViewById(R.id.bottomNav);

        itemAddress = findViewById(R.id.itemAddress);
        itemSetting = findViewById(R.id.itemSetting);
        itemHelp = findViewById(R.id.itemHelp);
        itemContact = findViewById(R.id.itemContact);

        loadUserProfile();

        btnBack.setOnClickListener(v -> finish());
        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
        
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        View.OnClickListener listener = v -> Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();

        itemAddress.setOnClickListener(listener);
        itemHelp.setOnClickListener(listener);
        itemContact.setOnClickListener(listener);

        itemSetting.setOnClickListener(v -> {
            Intent intent = new Intent(this, ThemHanMucActivity.class);
            startActivity(intent);
        });
        setupBottomNav();
    }

    private void loadUserProfile() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    tvUserName.setText("👤 " + (user.getName() != null ? user.getName() : "Chưa đặt tên"));
                    tvUserEmail.setText("📧 " + (user.getEmail() != null ? user.getEmail() : "Chưa có email"));
                    tvUserPhone.setText("📱 " + (user.getPhone() != null ? user.getPhone() : "Chưa có SĐT"));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) return true;
            
            Intent intent = null;
            if (id == R.id.nav_home) intent = new Intent(this, MainActivity.class);
            else if (id == R.id.nav_history) intent = new Intent(this, LichSuGDActivity.class);
            else if (id == R.id.nav_report) intent = new Intent(this, BCThongKeActivity.class);

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }
}
