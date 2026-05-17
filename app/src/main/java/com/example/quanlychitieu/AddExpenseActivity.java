package com.example.quanlychitieu;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
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

public class AddExpenseActivity extends AppCompatActivity {

    public static final String EXTRA_TRANSACTION_TYPE = "TRANSACTION_TYPE";

    private EditText edtMoney, edtNote;
    private TextView tvSelectedCategory, btnBack, tvHeaderTitle;
    private GridLayout gridCategorySelect;
    private Button btnSave;

    private DatabaseReference mDatabase;
    private String userId;
    private String selectedCategoryName = "";
    private String transactionType = "chi";

    private double currentBalance = 0;

    private double monthlyLimit = 0;
    private double yearlyLimit = 0;
    private double currentMonthExpense = 0;
    private double currentYearExpense = 0;

    private int monthlyOverLimitCount = 0;
    private int yearlyOverLimitCount = 0;

    private String currentMonth;
    private String currentYear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        String typeFromIntent = getIntent().getStringExtra(EXTRA_TRANSACTION_TYPE);
        if ("thu".equals(typeFromIntent) || "chi".equals(typeFromIntent)) {
            transactionType = typeFromIntent;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = (user != null) ? user.getUid() : "test_user_123";

        String dbUrl = "https://dbqlchitieu-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(userId);

        currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        currentYear = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());

        edtMoney = findViewById(R.id.edtMoney);
        edtNote = findViewById(R.id.edtNote);
        tvSelectedCategory = findViewById(R.id.tvSelectedCategory);
        gridCategorySelect = findViewById(R.id.gridCategorySelect);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);

        setupScreenByType();
        loadBalance();
        loadLimitAndStats();
        loadCategories();

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> processTransaction());
    }

    private void setupScreenByType() {
        if ("thu".equals(transactionType)) {
            tvHeaderTitle.setText("Thêm thu nhập");
            edtMoney.setTextColor(Color.parseColor("#2E7D32"));
            edtNote.setHint("Ví dụ: Lương tháng");
            tvSelectedCategory.setText("DANH MỤC THU: Chưa chọn");
            btnSave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            tvHeaderTitle.setText("Thêm khoản chi");
            edtMoney.setTextColor(Color.parseColor("#F44336"));
            edtNote.setHint("Ví dụ: Ăn trưa");
            tvSelectedCategory.setText("DANH MỤC CHI: Chưa chọn");
            btnSave.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }
    }

    private void loadBalance() {
        mDatabase.child("profile").child("totalBalance").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentBalance = getDoubleValue(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadLimitAndStats() {
        mDatabase.child("limits").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                monthlyLimit = getDoubleValue(snapshot.child("monthlyLimit"));
                yearlyLimit = getDoubleValue(snapshot.child("yearlyLimit"));
                loadExpenseStatsOnce();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        mDatabase.child("transactions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                calculateStats(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadExpenseStatsOnce() {
        mDatabase.child("transactions").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                calculateStats(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void calculateStats(DataSnapshot snapshot) {
        currentMonthExpense = 0;
        currentYearExpense = 0;
        monthlyOverLimitCount = 0;
        yearlyOverLimitCount = 0;

        for (DataSnapshot data : snapshot.getChildren()) {
            Transaction t = data.getValue(Transaction.class);
            if (t == null || t.getDate() == null || t.getType() == null) continue;
            if (!"chi".equals(t.getType())) continue;

            if (t.getDate().startsWith(currentMonth)) {
                currentMonthExpense += t.getAmount();

                if (t.isOverLimit()
                        && t.getOverLimitType() != null
                        && t.getOverLimitType().contains("month")) {
                    monthlyOverLimitCount++;
                }
            }

            if (t.getDate().startsWith(currentYear)) {
                currentYearExpense += t.getAmount();

                if (t.isOverLimit()
                        && t.getOverLimitType() != null
                        && t.getOverLimitType().contains("year")) {
                    yearlyOverLimitCount++;
                }
            }
        }
    }

    private void loadCategories() {
        mDatabase.child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                gridCategorySelect.removeAllViews();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Category cat = data.getValue(Category.class);

                    if (cat == null || cat.isDeleted()) continue;

                    String catType = cat.getType();
                    if (TextUtils.isEmpty(catType)) {
                        catType = "chi";
                    }

                    if (transactionType.equals(catType)) {
                        addCategoryToGrid(cat);
                    }
                }

                if (gridCategorySelect.getChildCount() == 0) {
                    Toast.makeText(
                            AddExpenseActivity.this,
                            "Chưa có danh mục " + ("thu".equals(transactionType) ? "thu nhập" : "chi tiêu") + ". Hãy thêm trong Quản lý danh mục.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addCategoryToGrid(Category cat) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_category_grid, gridCategorySelect, false);
        ImageView imgIcon = v.findViewById(R.id.imgCategoryIcon);
        TextView tvName = v.findViewById(R.id.tvCategoryName);

        tvName.setText(cat.getName());

        int resId = (cat.getIconName() != null)
                ? getResources().getIdentifier(cat.getIconName(), "drawable", getPackageName())
                : 0;

        imgIcon.setImageResource(resId != 0 ? resId : android.R.drawable.ic_menu_gallery);

        v.setOnClickListener(view -> {
            selectedCategoryName = cat.getName();

            if ("thu".equals(transactionType)) {
                tvSelectedCategory.setText("DANH MỤC THU: " + selectedCategoryName);
            } else {
                tvSelectedCategory.setText("DANH MỤC CHI: " + selectedCategoryName);
            }

            for (int i = 0; i < gridCategorySelect.getChildCount(); i++) {
                gridCategorySelect.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
            }

            v.setBackgroundColor(Color.parseColor("#E8F5E9"));
        });

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        v.setLayoutParams(params);

        gridCategorySelect.addView(v);
    }

    private void processTransaction() {
        String moneyStr = edtMoney.getText().toString().trim();

        if (TextUtils.isEmpty(moneyStr) || TextUtils.isEmpty(selectedCategoryName)) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;

        try {
            amount = Double.parseDouble(moneyStr);
        } catch (Exception e) {
            Toast.makeText(this, "Số tiền không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(this, "Số tiền phải lớn hơn 0!", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("thu".equals(transactionType)) {
            saveTransaction(amount, false, "");
        } else {
            if (amount > currentBalance) {
                Toast.makeText(this, "Số dư không đủ!", Toast.LENGTH_SHORT).show();
                return;
            }

            checkLimitBeforeSave(amount);
        }
    }

    private void checkLimitBeforeSave(double amount) {
        boolean exceedMonth = monthlyLimit > 0 && currentMonthExpense + amount > monthlyLimit;
        boolean exceedYear = yearlyLimit > 0 && currentYearExpense + amount > yearlyLimit;

        if (!exceedMonth && !exceedYear) {
            saveTransaction(amount, false, "");
            return;
        }

        if (exceedMonth && monthlyOverLimitCount >= 3) {
            Toast.makeText(this, "Bạn đã vượt hạn mức tháng quá 3 lần. Không thể thêm chi tiêu nữa!", Toast.LENGTH_LONG).show();
            return;
        }

        if (exceedYear && yearlyOverLimitCount >= 3) {
            Toast.makeText(this, "Bạn đã vượt hạn mức năm quá 3 lần. Không thể thêm chi tiêu nữa!", Toast.LENGTH_LONG).show();
            return;
        }

        String overLimitType;
        String message = "Khoản chi này sẽ vượt hạn mức.\n\n";

        if (exceedMonth && exceedYear) {
            overLimitType = "month_year";
            message += "Vượt hạn mức tháng: " + (monthlyOverLimitCount + 1) + "/3\n";
            message += "Vượt hạn mức năm: " + (yearlyOverLimitCount + 1) + "/3";
        } else if (exceedMonth) {
            overLimitType = "month";
            message += "Vượt hạn mức tháng: " + (monthlyOverLimitCount + 1) + "/3";
        } else {
            overLimitType = "year";
            message += "Vượt hạn mức năm: " + (yearlyOverLimitCount + 1) + "/3";
        }

        new AlertDialog.Builder(this)
                .setTitle("⚠️ Cảnh báo hạn mức")
                .setMessage(message)
                .setPositiveButton("Tiếp tục", (dialog, which) -> saveTransaction(amount, true, overLimitType))
                .setNegativeButton("Thoát", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void saveTransaction(double amount, boolean isOverLimit, String overLimitType) {
        String id = mDatabase.child("transactions").push().getKey();
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());

        Transaction t = new Transaction(
                id,
                selectedCategoryName,
                amount,
                date,
                selectedCategoryName,
                transactionType,
                edtNote.getText().toString()
        );

        t.setOverLimit(isOverLimit);
        t.setOverLimitType(overLimitType);

        if (id == null) {
            Toast.makeText(this, "Không tạo được giao dịch!", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("transactions").child(id).setValue(t)
                .addOnSuccessListener(aVoid -> {
                    double newBalance;

                    if ("thu".equals(transactionType)) {
                        newBalance = currentBalance + amount;
                    } else {
                        newBalance = currentBalance - amount;
                    }

                    mDatabase.child("profile").child("totalBalance").setValue(newBalance);

                    if ("thu".equals(transactionType)) {
                        Toast.makeText(this, "Đã lưu thu nhập!", Toast.LENGTH_SHORT).show();
                    } else if (isOverLimit) {
                        Toast.makeText(this, "Đã lưu khoản chi vượt hạn mức!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Đã lưu khoản chi!", Toast.LENGTH_SHORT).show();
                    }

                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private double getDoubleValue(DataSnapshot snapshot) {
        Object val = snapshot.getValue();

        if (val instanceof Long) return ((Long) val).doubleValue();
        if (val instanceof Double) return (Double) val;
        if (val instanceof Integer) return ((Integer) val).doubleValue();

        return 0;
    }
}