package com.example.quanlychitieu;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private GridLayout gridCategories;
    private TextView tvBalance, tvIncome, tvExpense, tvManageCategory;
    private Button btnAddBalance, btnAddExpense, btnHomeIncomeCategories, btnHomeExpenseCategories;
    private BottomNavigationView bottomNav;

    private DatabaseReference mDatabase;
    private String userId;
    private double currentBalance = 0;
    private String currentCategoryType = "thu";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = (user != null) ? user.getUid() : "test_user_123";

        String dbUrl = "https://dbqlchitieu-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(userId);

        gridCategories = findViewById(R.id.gridCategories);
        tvBalance = findViewById(R.id.tvBalance);
        tvIncome = findViewById(R.id.tvIncome);
        tvExpense = findViewById(R.id.tvExpense);
        tvManageCategory = findViewById(R.id.tvManageCategory);
        btnAddBalance = findViewById(R.id.btnAddBalance);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnHomeIncomeCategories = findViewById(R.id.btnHomeIncomeCategories);
        btnHomeExpenseCategories = findViewById(R.id.btnHomeExpenseCategories);
        bottomNav = findViewById(R.id.bottomNav);

        setupEvents();
        setupBottomNav();
        loadHomeData();
    }

    private void loadHomeData() {
        mDatabase.child("profile").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object val = snapshot.child("totalBalance").getValue();

                if (val instanceof Long) currentBalance = ((Long) val).doubleValue();
                else if (val instanceof Double) currentBalance = (Double) val;
                else if (val instanceof Integer) currentBalance = ((Integer) val).doubleValue();

                tvBalance.setText(String.format(Locale.GERMANY, "%,.0f VNĐ", currentBalance));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        loadCategoriesByType();
        loadMonthlyStats();
    }

    private void loadCategoriesByType() {
        mDatabase.child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                gridCategories.removeAllViews();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Category cat = data.getValue(Category.class);

                    if (cat == null || cat.isDeleted()) continue;

                    String catType = cat.getType();
                    if (TextUtils.isEmpty(catType)) {
                        catType = "chi";
                    }

                    if (currentCategoryType.equals(catType)) {
                        addCategoryToGrid(cat);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addCategoryToGrid(Category cat) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_category_grid, gridCategories, false);
        ImageView imgIcon = v.findViewById(R.id.imgCategoryIcon);
        TextView tvName = v.findViewById(R.id.tvCategoryName);

        tvName.setText(cat.getName());

        String iconName = cat.getIconName();
        int resId = (iconName != null)
                ? getResources().getIdentifier(iconName, "drawable", getPackageName())
                : 0;

        imgIcon.setImageResource(resId != 0 ? resId : android.R.drawable.ic_menu_gallery);

        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0;
        p.height = GridLayout.LayoutParams.WRAP_CONTENT;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        v.setLayoutParams(p);

        gridCategories.addView(v);
    }

    private void loadMonthlyStats() {
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        mDatabase.child("transactions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double in = 0;
                double out = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Transaction t = data.getValue(Transaction.class);

                    if (t != null && t.getDate() != null && t.getDate().startsWith(currentMonth)) {
                        if ("thu".equals(t.getType())) {
                            in += t.getAmount();
                        } else if ("chi".equals(t.getType())) {
                            out += t.getAmount();
                        }
                    }
                }

                tvIncome.setText(String.format(Locale.GERMANY, "%,.0f VNĐ", in));
                tvExpense.setText(String.format(Locale.GERMANY, "%,.0f VNĐ", out));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupEvents() {
        btnAddBalance.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddExpenseActivity.class);
            intent.putExtra(AddExpenseActivity.EXTRA_TRANSACTION_TYPE, "thu");
            startActivity(intent);
        });

        btnAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddExpenseActivity.class);
            intent.putExtra(AddExpenseActivity.EXTRA_TRANSACTION_TYPE, "chi");
            startActivity(intent);
        });

        btnHomeIncomeCategories.setOnClickListener(v -> {
            currentCategoryType = "thu";
            updateCategoryTabColors();
            loadCategoriesByType();
        });

        btnHomeExpenseCategories.setOnClickListener(v -> {
            currentCategoryType = "chi";
            updateCategoryTabColors();
            loadCategoriesByType();
        });

        tvManageCategory.setOnClickListener(v ->
                startActivity(new Intent(this, QuanLyDanhMucActivity.class))
        );

        updateCategoryTabColors();
    }

    private void updateCategoryTabColors() {
        int green = Color.parseColor("#2E7D32");
        int gray = Color.parseColor("#757575");

        if ("thu".equals(currentCategoryType)) {
            btnHomeIncomeCategories.setTextColor(green);
            btnHomeExpenseCategories.setTextColor(gray);
        } else {
            btnHomeIncomeCategories.setTextColor(gray);
            btnHomeExpenseCategories.setTextColor(green);
        }
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) return true;

            Intent intent = null;

            if (id == R.id.nav_history) {
                intent = new Intent(this, LichSuGDActivity.class);
            } else if (id == R.id.nav_report) {
                intent = new Intent(this, BCThongKeActivity.class);
            } else if (id == R.id.nav_profile) {
                intent = new Intent(this, CaiDatActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}