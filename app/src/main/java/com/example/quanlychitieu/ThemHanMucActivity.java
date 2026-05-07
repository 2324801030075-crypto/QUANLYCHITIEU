package com.example.quanlychitieu;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.*;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ThemHanMucActivity extends AppCompatActivity {

    private TextView btnBack;
    private EditText edtLimit;
    private RadioGroup rgLimitType;
    private RadioButton rbMonth, rbYear;
    private Button btnSaveLimit, btnExitLimit;

    private DatabaseReference limitRef;
    private boolean isFormatting = false;

    private double monthlyLimit = 0;
    private double yearlyLimit = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_them_han_muc);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user != null ? user.getUid() : "test_user_123";

        String dbUrl = "https://dbqlchitieu-default-rtdb.asia-southeast1.firebasedatabase.app/";
        limitRef = FirebaseDatabase.getInstance(dbUrl)
                .getReference("users")
                .child(userId)
                .child("limits");

        btnBack = findViewById(R.id.btnBack);
        edtLimit = findViewById(R.id.edtLimit);
        rgLimitType = findViewById(R.id.rgLimitType);
        rbMonth = findViewById(R.id.rbMonth);
        rbYear = findViewById(R.id.rbYear);
        btnSaveLimit = findViewById(R.id.btnSaveLimit);
        btnExitLimit = findViewById(R.id.btnExitLimit);

        btnBack.setOnClickListener(v -> finish());
        btnExitLimit.setOnClickListener(v -> finish());

        setupMoneyFormatter();
        setupRadioEvents();
        loadCurrentLimits();

        btnSaveLimit.setOnClickListener(v -> saveLimit());
    }

    private void setupRadioEvents() {
        rgLimitType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbMonth) {
                if (monthlyLimit > 0) {
                    edtLimit.setText(formatMoneyWithoutCurrency(monthlyLimit));
                    edtLimit.setSelection(edtLimit.getText().length());
                } else {
                    edtLimit.setText("");
                }
            } else if (checkedId == R.id.rbYear) {
                if (yearlyLimit > 0) {
                    edtLimit.setText(formatMoneyWithoutCurrency(yearlyLimit));
                    edtLimit.setSelection(edtLimit.getText().length());
                } else {
                    edtLimit.setText("");
                }
            }
        });
    }

    private void loadCurrentLimits() {
        limitRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                monthlyLimit = getDoubleValue(snapshot.child("monthlyLimit"));
                yearlyLimit = getDoubleValue(snapshot.child("yearlyLimit"));

                if (monthlyLimit > 0) {
                    rbMonth.setChecked(true);
                    edtLimit.setText(formatMoneyWithoutCurrency(monthlyLimit));
                    edtLimit.setSelection(edtLimit.getText().length());
                } else if (yearlyLimit > 0) {
                    rbYear.setChecked(true);
                    edtLimit.setText(formatMoneyWithoutCurrency(yearlyLimit));
                    edtLimit.setSelection(edtLimit.getText().length());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ThemHanMucActivity.this, "Không tải được hạn mức", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveLimit() {
        String rawMoney = edtLimit.getText().toString()
                .replace(".", "")
                .replace(",", "")
                .replace("VNĐ", "")
                .trim();

        if (TextUtils.isEmpty(rawMoney)) {
            Toast.makeText(this, "hãy nhập hạn mức", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rgLimitType.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Hãy chọn hạn mức tháng hoặc năm", Toast.LENGTH_SHORT).show();
            return;
        }

        double limitAmount;

        try {
            limitAmount = Double.parseDouble(rawMoney);
        } catch (Exception e) {
            Toast.makeText(this, "Hạn mức không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (limitAmount <= 0) {
            Toast.makeText(this, "Hạn mức phải lớn hơn 0", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();

        if (rbMonth.isChecked()) {
            updates.put("monthlyLimit", limitAmount);
        } else if (rbYear.isChecked()) {
            updates.put("yearlyLimit", limitAmount);
        }

        limitRef.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã lưu hạn mức", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void setupMoneyFormatter() {
        edtLimit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isFormatting) return;

                String raw = s.toString()
                        .replace(".", "")
                        .replace(",", "")
                        .replace("VNĐ", "")
                        .trim();

                if (raw.isEmpty()) return;

                try {
                    isFormatting = true;
                    double value = Double.parseDouble(raw);
                    String formatted = formatMoneyWithoutCurrency(value);
                    edtLimit.setText(formatted);
                    edtLimit.setSelection(edtLimit.getText().length());
                } catch (Exception ignored) {
                } finally {
                    isFormatting = false;
                }
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private String formatMoneyWithoutCurrency(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMANY);
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(value);
    }

    private double getDoubleValue(DataSnapshot snapshot) {
        Object value = snapshot.getValue();
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        return 0;
    }
}