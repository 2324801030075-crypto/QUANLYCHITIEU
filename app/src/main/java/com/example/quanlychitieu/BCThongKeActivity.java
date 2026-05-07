package com.example.quanlychitieu;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BCThongKeActivity extends AppCompatActivity {

    private PieChart pieChart;
    private TextView tvSelectTime, tvTotalIn, tvTotalOut, tvWarning, btnBack;
    private Button btnTabExpense, btnTabIncome, btnTabCompare;
    private ListView lvReport;
    private BottomNavigationView bottomNav;

    private DatabaseReference mUserRef;
    private DatabaseReference mTransactionRef;
    private DatabaseReference mCategoryRef;

    private final Set<String> activeCategoryNames = new HashSet<>();
    private final List<Transaction> allTransactions = new ArrayList<>();
    private final List<Transaction> currentDisplayList = new ArrayList<>();

    private String selectedYearMonth = "";

    private static final int TAB_EXPENSE = 0;
    private static final int TAB_INCOME = 1;
    private static final int TAB_COMPARE = 2;

    private int currentTab = TAB_EXPENSE;
    private boolean hasShownOverIncomeWarning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bcthong_ke);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = (user != null) ? user.getUid() : "test_user_123";

        String dbUrl = "https://dbqlchitieu-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mUserRef = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(userId);
        mTransactionRef = mUserRef.child("transactions");
        mCategoryRef = mUserRef.child("categories");

        pieChart = findViewById(R.id.pieChart);
        tvSelectTime = findViewById(R.id.tvSelectTime);
        tvTotalIn = findViewById(R.id.tvTotalIn);
        tvTotalOut = findViewById(R.id.tvTotalOut);
        tvWarning = findViewById(R.id.tvWarning);
        btnTabExpense = findViewById(R.id.btnTabExpense);
        btnTabIncome = findViewById(R.id.btnTabIncome);
        btnTabCompare = findViewById(R.id.btnTabCompare);
        lvReport = findViewById(R.id.lvReport);
        btnBack = findViewById(R.id.btnBack);
        bottomNav = findViewById(R.id.bottomNav);

        Calendar c = Calendar.getInstance();
        selectedYearMonth = String.format(
                Locale.getDefault(),
                "%d-%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1
        );

        tvSelectTime.setText("Thống kê: " + selectedYearMonth);

        setupEvents();
        setupPieChartStyle();
        loadData();
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());

        tvSelectTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();

            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedYearMonth = String.format(Locale.getDefault(), "%d-%02d", year, month + 1);
                tvSelectTime.setText("Thống kê: " + selectedYearMonth);

                hasShownOverIncomeWarning = false;
                updateUI();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), 1).show();
        });

        btnTabExpense.setOnClickListener(v -> {
            currentTab = TAB_EXPENSE;
            hasShownOverIncomeWarning = false;
            updateUI();
        });

        btnTabIncome.setOnClickListener(v -> {
            currentTab = TAB_INCOME;
            hasShownOverIncomeWarning = false;
            updateUI();
        });

        btnTabCompare.setOnClickListener(v -> {
            currentTab = TAB_COMPARE;
            updateUI();
        });

        bottomNav.setSelectedItemId(R.id.nav_report);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_report) {
                return true;
            }

            Intent intent = null;

            if (id == R.id.nav_home) {
                intent = new Intent(this, MainActivity.class);
            } else if (id == R.id.nav_history) {
                intent = new Intent(this, LichSuGDActivity.class);
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

    private void setupPieChartStyle() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(11f);
        pieChart.getLegend().setEnabled(true);
    }

    private void loadData() {
        mCategoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                activeCategoryNames.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Category cat = data.getValue(Category.class);

                    if (cat != null && !cat.isDeleted() && cat.getName() != null) {
                        activeCategoryNames.add(cat.getName());
                    }
                }

                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BCThongKeActivity.this, "Lỗi tải danh mục", Toast.LENGTH_SHORT).show();
            }
        });

        mTransactionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTransactions.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Transaction t = data.getValue(Transaction.class);

                    if (t != null) {
                        allTransactions.add(t);
                    }
                }

                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BCThongKeActivity.this, "Lỗi tải giao dịch", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        double totalIn = 0;
        double totalOut = 0;

        currentDisplayList.clear();

        Map<String, Double> expenseCategoryMap = new HashMap<>();
        Map<String, Double> incomeCategoryMap = new HashMap<>();

        for (Transaction t : allTransactions) {
            if (t.getDate() == null || t.getType() == null) {
                continue;
            }

            if (!t.getDate().startsWith(selectedYearMonth)) {
                continue;
            }

            String type = t.getType();
            double amount = t.getAmount();

            if ("thu".equals(type)) {
                totalIn += amount;

                if (currentTab == TAB_INCOME || currentTab == TAB_COMPARE) {
                    currentDisplayList.add(t);
                }

                String incomeName = getSafeCategoryName(t.getCategoryId());
                incomeCategoryMap.put(
                        incomeName,
                        incomeCategoryMap.getOrDefault(incomeName, 0.0) + amount
                );

            } else if ("chi".equals(type)) {
                totalOut += amount;

                if (currentTab == TAB_EXPENSE || currentTab == TAB_COMPARE) {
                    currentDisplayList.add(t);
                }

                String expenseName = getExpenseChartCategoryName(t.getCategoryId());
                expenseCategoryMap.put(
                        expenseName,
                        expenseCategoryMap.getOrDefault(expenseName, 0.0) + amount
                );
            }
        }

        tvTotalIn.setText("Tổng thu: " + formatMoney(totalIn));
        tvTotalOut.setText("Tổng chi: " + formatMoney(totalOut));

        updateTabColor();
        updateWarning(totalIn, totalOut);

        if (currentTab == TAB_EXPENSE) {
            setupPieChartFromMap(expenseCategoryMap, "Chi tiêu");
        } else if (currentTab == TAB_INCOME) {
            setupPieChartFromMap(incomeCategoryMap, "Thu nhập");
        } else {
            setupComparePieChart(totalIn, totalOut);
            showOverIncomeToastIfNeeded(totalIn, totalOut);
        }

        updateListView();
    }

    private String getExpenseChartCategoryName(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return "Khác";
        }

        if (activeCategoryNames.contains(categoryName)) {
            return categoryName;
        }

        return "Khác";
    }

    private String getSafeCategoryName(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return "Khác";
        }

        return categoryName;
    }

    private void updateTabColor() {
        int green = Color.parseColor("#4CAF50");
        int gray = Color.parseColor("#757575");

        btnTabExpense.setTextColor(currentTab == TAB_EXPENSE ? green : gray);
        btnTabIncome.setTextColor(currentTab == TAB_INCOME ? green : gray);
        btnTabCompare.setTextColor(currentTab == TAB_COMPARE ? green : gray);
    }

    private void updateWarning(double totalIn, double totalOut) {
        if (totalOut > totalIn && totalOut > 0) {
            double overAmount = totalOut - totalIn;

            tvWarning.setVisibility(View.VISIBLE);
            tvWarning.setText("⚠ Chi tiêu đã vượt quá thu nhập " + formatMoney(overAmount));
        } else {
            tvWarning.setVisibility(View.GONE);
        }
    }

    private void showOverIncomeToastIfNeeded(double totalIn, double totalOut) {
        if (totalOut > totalIn && totalOut > 0 && !hasShownOverIncomeWarning) {
            Toast.makeText(
                    this,
                    "Cảnh báo: Chi tiêu đã vượt quá thu nhập!",
                    Toast.LENGTH_LONG
            ).show();

            hasShownOverIncomeWarning = true;
        }
    }

    private void setupPieChartFromMap(Map<String, Double> map, String centerText) {
        if (map.isEmpty()) {
            pieChart.clear();
            pieChart.setCenterText("Không có dữ liệu");
            pieChart.invalidate();
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();

        for (String name : map.keySet()) {
            double value = map.get(name) != null ? map.get(name) : 0;

            if (value > 0) {
                entries.add(new PieEntry((float) value, name));
            }
        }

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setCenterText("Không có dữ liệu");
            pieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        PieData data = new PieData(dataSet);

        pieChart.setData(data);
        pieChart.setCenterText(centerText);
        pieChart.invalidate();
    }

    private void setupComparePieChart(double totalIn, double totalOut) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        if (totalIn > 0) {
            entries.add(new PieEntry((float) totalIn, "Thu nhập"));
        }

        if (totalOut > 0) {
            entries.add(new PieEntry((float) totalOut, "Chi tiêu"));
        }

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setCenterText("Không có dữ liệu");
            pieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "So sánh");
        dataSet.setColors(
                Color.parseColor("#4CAF50"),
                Color.parseColor("#F44336")
        );
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);

        PieData data = new PieData(dataSet);

        pieChart.setData(data);
        pieChart.setCenterText("Thu / Chi");
        pieChart.invalidate();
    }

    private void updateListView() {
        ArrayAdapter<Transaction> adapter = new ArrayAdapter<Transaction>(
                this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                currentDisplayList
        ) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_history, parent, false);
                }

                TextView tvTitle = convertView.findViewById(R.id.tvHistoryTitle);
                TextView tvAmount = convertView.findViewById(R.id.tvHistoryAmount);

                Transaction t = getItem(position);

                if (t != null) {
                    String title = t.getTitle() != null ? t.getTitle() : "(Không có tiêu đề)";
                    String type = t.getType();

                    if ("chi".equals(type)) {
                        if (t.isOverLimit()) {
                            tvTitle.setText("🚩 " + title + " - Vượt hạn mức");
                            tvTitle.setTextColor(Color.parseColor("#F44336"));
                        } else {
                            tvTitle.setText("Chi tiêu: " + title);
                            tvTitle.setTextColor(Color.parseColor("#333333"));
                        }

                        tvAmount.setText("-" + formatMoneyWithoutSymbol(t.getAmount()) + " VNĐ");
                        tvAmount.setTextColor(Color.RED);

                    } else if ("thu".equals(type)) {
                        tvTitle.setText("Thu nhập: " + title);
                        tvTitle.setTextColor(Color.parseColor("#333333"));

                        tvAmount.setText("+" + formatMoneyWithoutSymbol(t.getAmount()) + " VNĐ");
                        tvAmount.setTextColor(Color.parseColor("#2E7D32"));

                    } else {
                        tvTitle.setText(title);
                        tvTitle.setTextColor(Color.parseColor("#333333"));

                        tvAmount.setText(formatMoneyWithoutSymbol(t.getAmount()) + " VNĐ");
                        tvAmount.setTextColor(Color.parseColor("#333333"));
                    }

                    if (t.isOverLimit()) {
                        tvAmount.setTextColor(Color.parseColor("#F44336"));
                    }
                }

                return convertView;
            }
        };

        lvReport.setAdapter(adapter);
    }

    private String formatMoney(double amount) {
        return String.format(Locale.GERMANY, "%,.0fđ", amount);
    }

    private String formatMoneyWithoutSymbol(double amount) {
        return String.format(Locale.GERMANY, "%,.0f", amount);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_report);
        }
    }
}