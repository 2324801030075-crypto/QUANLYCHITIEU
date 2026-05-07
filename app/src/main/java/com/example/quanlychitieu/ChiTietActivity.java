package com.example.quanlychitieu;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ChiTietActivity extends AppCompatActivity {

    private TextView tvMoney, tvDateTime, tvCode, tvBalanceAfter, tvCategory, tvNote;
    private Button btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chi_tiet);

        tvMoney = findViewById(R.id.tvMoney);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvCode = findViewById(R.id.tvCode);
        tvBalanceAfter = findViewById(R.id.tvBalanceAfter);
        tvCategory = findViewById(R.id.tvCategory);
        tvNote = findViewById(R.id.tvNote);
        btnClose = findViewById(R.id.btnClose);

        Transaction transaction;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            transaction = getIntent().getSerializableExtra("TRANSACTION_DATA", Transaction.class);
        } else {
            transaction = (Transaction) getIntent().getSerializableExtra("TRANSACTION_DATA");
        }

        if (transaction != null) {
            String amountStr = (transaction.getType().equals("chi") ? "-" : "+") + String.format("%,.0f", transaction.getAmount()) + " VNĐ";
            tvMoney.setText(amountStr);
            tvMoney.setTextColor(transaction.getType().equals("chi") ? Color.RED : Color.parseColor("#2E7D32"));
            tvCode.setText(transaction.getId());
            tvDateTime.setText(transaction.getDate());
            tvNote.setText(transaction.getNote());
            tvCategory.setText(transaction.getCategoryId()); 
            tvBalanceAfter.setText("Đã cập nhật");
        }

        btnClose.setOnClickListener(v -> finish());
    }
}
