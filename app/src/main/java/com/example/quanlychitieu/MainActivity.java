package com.example.quanlychitieu;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
    private Button btnAddBalance, btnAddExpense;
    private BottomNavigationView bottomNav;
    
    private DatabaseReference mDatabase;
    private String userId;
    private double currentBalance = 0;

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
        bottomNav = findViewById(R.id.bottomNav);

        loadHomeData();
        setupEvents();
        setupBottomNav();
    }

    private void loadHomeData() {
        mDatabase.child("profile").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Object val = snapshot.child("totalBalance").getValue();
                    if (val instanceof Long) currentBalance = ((Long) val).doubleValue();
                    else if (val instanceof Double) currentBalance = (Double) val;
                    
                    tvBalance.setText(String.format(Locale.GERMANY, "%,.0f VNĐ", currentBalance));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        mDatabase.child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                gridCategories.removeAllViews();
                if (!snapshot.exists()) {
                    tvManageCategory.performClick();
                }
                for (DataSnapshot data : snapshot.getChildren()) {
                    Category cat = data.getValue(Category.class);

                    if (cat != null && !cat.isDeleted()) {
                        addCategoryToGrid(cat);
                    }
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        loadMonthlyStats();
    }

    private void addCategoryToGrid(Category cat) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_category_grid, gridCategories, false);
        ImageView imgIcon = v.findViewById(R.id.imgCategoryIcon);
        TextView tvName = v.findViewById(R.id.tvCategoryName);
        
        tvName.setText(cat.getName());
        String iconName = cat.getIconName();
        int resId = (iconName != null) ? getResources().getIdentifier(iconName, "drawable", getPackageName()) : 0;
        imgIcon.setImageResource(resId != 0 ? resId : android.R.drawable.ic_menu_gallery);

        GridLayout.LayoutParams p = new GridLayout.LayoutParams();
        p.width = 0; p.height = GridLayout.LayoutParams.WRAP_CONTENT;
        p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        v.setLayoutParams(p);
        gridCategories.addView(v);
    }

    private void loadMonthlyStats() {
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        mDatabase.child("transactions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double in = 0, out = 0;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Transaction t = data.getValue(Transaction.class);

                    if (t != null && t.getDate() != null && t.getDate().startsWith(currentMonth)) {
                        if ("thu".equals(t.getType())) in += t.getAmount();
                        else if ("chi".equals(t.getType())) out += t.getAmount();
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
        btnAddBalance.setOnClickListener(v -> showAddIncomeDialog());
        btnAddExpense.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddExpenseActivity.class);
            startActivity(intent);
        });
        tvManageCategory.setOnClickListener(v -> startActivity(new Intent(this, QuanLyDanhMucActivity.class)));
    }

    private void showAddIncomeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nhập thu nhập");
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);
        builder.setPositiveButton("Lưu", (d, w) -> {
            String s = input.getText().toString();
            if (!s.isEmpty()) {
                double amount = Double.parseDouble(s);
                mDatabase.child("profile").child("totalBalance").setValue(currentBalance + amount);
                String id = mDatabase.child("transactions").push().getKey();

                String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
                Transaction t = new Transaction(id, "Nạp tiền", amount, date, "Nạp tiền", "thu", "Nhập từ màn hình chính");
                if (id != null) mDatabase.child("transactions").child(id).setValue(t);
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            
            Intent intent = null;
            if (id == R.id.nav_history) intent = new Intent(this, LichSuGDActivity.class);
            else if (id == R.id.nav_report) intent = new Intent(this, BCThongKeActivity.class);
            else if (id == R.id.nav_profile) intent = new Intent(this, CaiDatActivity.class);

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
        bottomNav.setSelectedItemId(R.id.nav_home);
    }
}
