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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
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
    private BarChart barChart;
    private TextView tvSelectTime, tvTotalIn, tvTotalOut, tvWarning, btnBack, btnSwitchChart, tvChartTitle;
    private Button btnTabExpense, btnTabIncome, btnTabCompare;
    private ListView lvReport;
    private BottomNavigationView bottomNav;

    private DatabaseReference mUserRef;
    private DatabaseReference mTransactionRef;
    private DatabaseReference mCategoryRef;

    private final Set<String> activeCategoryNames = new HashSet<>();
    private final Map<String, Category> activeCategoryById = new HashMap<>();
    private final Map<String, Category> activeCategoryByName = new HashMap<>();
    private final Map<String, Category> activeExpenseCategoryById = new HashMap<>();
    private final Map<String, Category> activeExpenseCategoryByName = new HashMap<>();
    private final Map<String, Category> activeIncomeCategoryById = new HashMap<>();
    private final Map<String, Category> activeIncomeCategoryByName = new HashMap<>();
    private final List<Transaction> allTransactions = new ArrayList<>();
    private final List<Transaction> currentDisplayList = new ArrayList<>();

    private String selectedYearMonth = "";
    private double monthlyExpenseLimit = 0;

    private static final int TAB_EXPENSE = 0;
    private static final int TAB_INCOME = 1;
    private static final int TAB_COMPARE = 2;

    private static final int CHART_PIE = 0;
    private static final int CHART_BAR = 1;

    private static final int COLOR_GREEN = Color.rgb(76, 175, 80);
    private static final int COLOR_RED = Color.rgb(244, 67, 54);
    private static final int COLOR_BLUE = Color.rgb(33, 150, 243);
    private static final int COLOR_GRAY = Color.rgb(117, 117, 117);

    private int currentTab = TAB_EXPENSE;
    private int expenseChartMode = CHART_PIE;
    private int incomeChartMode = CHART_BAR;
    private int compareChartMode = CHART_PIE;

    private boolean hasShownOverIncomeWarning = false;
    private boolean hasShownExpenseLimitWarning = false;
    private boolean hasShownCompareBarWarning = false;

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
        barChart = findViewById(R.id.barChart);
        tvSelectTime = findViewById(R.id.tvSelectTime);
        tvTotalIn = findViewById(R.id.tvTotalIn);
        tvTotalOut = findViewById(R.id.tvTotalOut);
        tvWarning = findViewById(R.id.tvWarning);
        tvChartTitle = findViewById(R.id.tvChartTitle);
        btnSwitchChart = findViewById(R.id.btnSwitchChart);
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
        setupBarChartStyle();
        loadData();
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());

        tvSelectTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();

            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedYearMonth = String.format(Locale.getDefault(), "%d-%02d", year, month + 1);
                tvSelectTime.setText("Thống kê: " + selectedYearMonth);

                resetToastWarnings();
                updateUI();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), 1).show();
        });

        btnTabExpense.setOnClickListener(v -> {
            currentTab = TAB_EXPENSE;
            resetToastWarnings();
            updateUI();
        });

        btnTabIncome.setOnClickListener(v -> {
            currentTab = TAB_INCOME;
            resetToastWarnings();
            updateUI();
        });

        btnTabCompare.setOnClickListener(v -> {
            currentTab = TAB_COMPARE;
            resetToastWarnings();
            updateUI();
        });

        btnSwitchChart.setOnClickListener(v -> {
            switchCurrentChartMode();
            resetToastWarnings();
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
        pieChart.setNoDataText("Không có dữ liệu");
    }

    private void setupBarChartStyle() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setScaleEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setNoDataText("Không có dữ liệu");

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.parseColor("#555555"));
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return formatMoneyCompact(value);
            }
        });

        barChart.getAxisRight().setEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.parseColor("#555555"));

        barChart.getLegend().setEnabled(true);
    }

    private void loadData() {
        mCategoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                activeCategoryNames.clear();
                activeCategoryById.clear();
                activeCategoryByName.clear();
                activeExpenseCategoryById.clear();
                activeExpenseCategoryByName.clear();
                activeIncomeCategoryById.clear();
                activeIncomeCategoryByName.clear();
                monthlyExpenseLimit = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Category cat = data.getValue(Category.class);

                    if (cat == null || cat.isDeleted()) {
                        continue;
                    }

                    String categoryName = cat.getName() != null ? cat.getName().trim() : "";
                    String categoryId = cat.getId() != null ? cat.getId().trim() : "";

                    if (!categoryName.isEmpty()) {
                        activeCategoryNames.add(categoryName);
                        activeCategoryByName.put(categoryName, cat);
                    }

                    if (!categoryId.isEmpty()) {
                        activeCategoryById.put(categoryId, cat);
                    }

                    if (isExpenseCategory(cat)) {
                        if (!categoryName.isEmpty()) {
                            activeExpenseCategoryByName.put(categoryName, cat);
                        }

                        if (!categoryId.isEmpty()) {
                            activeExpenseCategoryById.put(categoryId, cat);
                        }

                        // Hạn mức chi tiêu tháng được lấy từ amount của các danh mục chi chưa bị xóa.
                        monthlyExpenseLimit += parseMoneyToDouble(cat.getAmount());
                    } else if (isIncomeCategory(cat)) {
                        if (!categoryName.isEmpty()) {
                            activeIncomeCategoryByName.put(categoryName, cat);
                        }

                        if (!categoryId.isEmpty()) {
                            activeIncomeCategoryById.put(categoryId, cat);
                        }
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
            if (t == null || t.getDate() == null || t.getType() == null) {
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

                String incomeName = getChartCategoryName(t.getCategoryId(), "thu");
                incomeCategoryMap.put(
                        incomeName,
                        incomeCategoryMap.getOrDefault(incomeName, 0.0) + amount
                );

            } else if ("chi".equals(type)) {
                totalOut += amount;

                if (currentTab == TAB_EXPENSE || currentTab == TAB_COMPARE) {
                    currentDisplayList.add(t);
                }

                String expenseName = getChartCategoryName(t.getCategoryId(), "chi");
                expenseCategoryMap.put(
                        expenseName,
                        expenseCategoryMap.getOrDefault(expenseName, 0.0) + amount
                );
            }
        }

        tvTotalIn.setText("Tổng thu: " + formatMoney(totalIn));
        tvTotalOut.setText("Tổng chi: " + formatMoney(totalOut));

        updateTabColor();
        updateChartHeader();

        double[] monthlyIncomeTotals = getMonthlyTotals("thu");
        double[] monthlyExpenseTotals = getMonthlyTotals("chi");
        boolean[] monthlyExpenseOverLimit = getMonthlyExpenseOverLimitFlags(monthlyExpenseTotals);

        if (currentTab == TAB_EXPENSE) {
            if (expenseChartMode == CHART_PIE) {
                setupPieChartFromMap(expenseCategoryMap, "Chi tiêu");
                showGeneralWarningIfNeeded(totalIn, totalOut);
            } else {
                setupExpenseBarChart(monthlyExpenseTotals, monthlyExpenseOverLimit);
                showExpenseLimitWarningIfNeeded(monthlyExpenseTotals, monthlyExpenseOverLimit);
            }
        } else if (currentTab == TAB_INCOME) {
            hideWarning();

            if (incomeChartMode == CHART_PIE) {
                setupPieChartFromMap(incomeCategoryMap, "Thu nhập");
            } else {
                setupIncomeBarChart(totalIn);
            }
        } else {
            if (compareChartMode == CHART_PIE) {
                setupComparePieChart(totalIn, totalOut);
                showCompareWarningIfNeeded(totalIn, totalOut, true);
            } else {
                setupCompareBarChart(monthlyIncomeTotals, monthlyExpenseTotals);
                showCompareBarWarningIfNeeded(monthlyIncomeTotals, monthlyExpenseTotals);
            }
        }

        updateListView();
    }

    private void switchCurrentChartMode() {
        if (currentTab == TAB_EXPENSE) {
            expenseChartMode = expenseChartMode == CHART_PIE ? CHART_BAR : CHART_PIE;
        } else if (currentTab == TAB_INCOME) {
            incomeChartMode = incomeChartMode == CHART_PIE ? CHART_BAR : CHART_PIE;
        } else {
            compareChartMode = compareChartMode == CHART_PIE ? CHART_BAR : CHART_PIE;
        }
    }

    private int getCurrentChartMode() {
        if (currentTab == TAB_EXPENSE) {
            return expenseChartMode;
        }

        if (currentTab == TAB_INCOME) {
            return incomeChartMode;
        }

        return compareChartMode;
    }

    private void updateChartHeader() {
        String tabName;

        if (currentTab == TAB_EXPENSE) {
            tabName = "chi tiêu";
        } else if (currentTab == TAB_INCOME) {
            tabName = "thu nhập";
        } else {
            tabName = "so sánh";
        }

        String chartType = getCurrentChartMode() == CHART_PIE ? "tròn" : "cột";
        tvChartTitle.setText("Biểu đồ " + tabName + " (" + chartType + ")");
        btnSwitchChart.setText("Thay đổi");
    }

    private void showPieChart() {
        pieChart.setVisibility(View.VISIBLE);
        barChart.setVisibility(View.GONE);
        barChart.clear();
        barChart.getAxisLeft().removeAllLimitLines();
    }

    private void showBarChart() {
        pieChart.setVisibility(View.GONE);
        barChart.setVisibility(View.VISIBLE);
        pieChart.clear();
    }

    private void setupPieChartFromMap(Map<String, Double> map, String centerText) {
        showPieChart();

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
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatMoneyCompact(value);
            }
        });

        PieData data = new PieData(dataSet);

        pieChart.setData(data);
        pieChart.setCenterText(centerText);
        pieChart.invalidate();
    }

    private void setupComparePieChart(double totalIn, double totalOut) {
        showPieChart();

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
        dataSet.setColors(COLOR_GREEN, COLOR_RED);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatMoneyCompact(value);
            }
        });

        PieData data = new PieData(dataSet);

        pieChart.setData(data);
        pieChart.setCenterText("Thu / Chi");
        pieChart.invalidate();
    }

    private void setupIncomeBarChart(double totalIn) {
        if (totalIn <= 0) {
            setupEmptyBarChart("Không có dữ liệu thu nhập");
            return;
        }

        showBarChart();
        resetBarChartForNormalBars();

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, (float) totalIn));

        BarDataSet dataSet = new BarDataSet(entries, "Thu nhập " + selectedYearMonth);
        dataSet.setColor(COLOR_GREEN);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(11f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.45f);
        data.setValueFormatter(getMoneyValueFormatter());

        barChart.setData(data);
        configureSingleBarXAxis(selectedYearMonth);
        adjustLeftAxisMax((float) totalIn);
        barChart.invalidate();
        barChart.animateY(400);
    }

    private void setupExpenseBarChart(double[] monthlyExpenseTotals, boolean[] monthlyExpenseOverLimit) {
        if (!hasAnyAmount(monthlyExpenseTotals)) {
            setupEmptyBarChart("Không có dữ liệu chi tiêu");
            return;
        }

        showBarChart();
        resetBarChartForNormalBars();

        ArrayList<BarEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        float maxValue = 0f;

        for (int i = 0; i < 12; i++) {
            float value = (float) monthlyExpenseTotals[i];
            entries.add(new BarEntry(i, value));
            colors.add(monthlyExpenseOverLimit[i] ? COLOR_RED : COLOR_GREEN);
            maxValue = Math.max(maxValue, value);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Chi tiêu theo tháng");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(9f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.55f);
        data.setValueFormatter(getMoneyValueFormatter());

        barChart.setData(data);
        configureMonthXAxis(false);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.removeAllLimitLines();

        if (monthlyExpenseLimit > 0) {
            LimitLine limitLine = new LimitLine((float) monthlyExpenseLimit, "Hạn mức");
            limitLine.setLineColor(COLOR_RED);
            limitLine.setLineWidth(1.5f);
            limitLine.enableDashedLine(12f, 8f, 0f);
            limitLine.setTextColor(COLOR_RED);
            limitLine.setTextSize(10f);
            leftAxis.addLimitLine(limitLine);
            maxValue = Math.max(maxValue, (float) monthlyExpenseLimit);
        }

        adjustLeftAxisMax(maxValue);
        barChart.invalidate();
        barChart.animateY(400);
    }

    private void setupCompareBarChart(double[] monthlyIncomeTotals, double[] monthlyExpenseTotals) {
        if (!hasAnyAmount(monthlyIncomeTotals) && !hasAnyAmount(monthlyExpenseTotals)) {
            setupEmptyBarChart("Không có dữ liệu so sánh");
            return;
        }

        showBarChart();
        resetBarChartForNormalBars();

        ArrayList<BarEntry> incomeEntries = new ArrayList<>();
        ArrayList<BarEntry> expenseEntries = new ArrayList<>();
        List<Integer> expenseColors = new ArrayList<>();
        float maxValue = 0f;

        for (int i = 0; i < 12; i++) {
            float income = (float) monthlyIncomeTotals[i];
            float expense = (float) monthlyExpenseTotals[i];

            incomeEntries.add(new BarEntry(i, income));
            expenseEntries.add(new BarEntry(i, expense));

            boolean overIncome = expense > income && expense > 0;
            expenseColors.add(overIncome ? COLOR_RED : COLOR_BLUE);

            maxValue = Math.max(maxValue, Math.max(income, expense));
        }

        BarDataSet incomeDataSet = new BarDataSet(incomeEntries, "Thu nhập");
        incomeDataSet.setColor(COLOR_GREEN);
        incomeDataSet.setValueTextColor(Color.BLACK);
        incomeDataSet.setValueTextSize(8f);

        BarDataSet expenseDataSet = new BarDataSet(expenseEntries, "Chi tiêu");
        expenseDataSet.setColors(expenseColors);
        expenseDataSet.setValueTextColor(Color.BLACK);
        expenseDataSet.setValueTextSize(8f);

        BarData data = new BarData(incomeDataSet, expenseDataSet);
        data.setValueFormatter(getMoneyValueFormatter());

        float groupSpace = 0.22f;
        float barSpace = 0.02f;
        float barWidth = 0.37f;
        data.setBarWidth(barWidth);

        barChart.setData(data);
        configureMonthXAxis(true);
        barChart.groupBars(0f, groupSpace, barSpace);
        barChart.setFitBars(false);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.removeAllLimitLines();

        int selectedMonthIndex = getSelectedMonthIndex();
        if (isValidMonthIndex(selectedMonthIndex)) {
            double selectedIncome = monthlyIncomeTotals[selectedMonthIndex];
            double selectedExpense = monthlyExpenseTotals[selectedMonthIndex];

            if (selectedExpense > selectedIncome && selectedExpense > 0) {
                LimitLine incomeLine = new LimitLine(
                        (float) selectedIncome,
                        "Thu nhập " + getSelectedMonthLabel()
                );
                incomeLine.setLineColor(COLOR_RED);
                incomeLine.setLineWidth(1.5f);
                incomeLine.enableDashedLine(12f, 8f, 0f);
                incomeLine.setTextColor(COLOR_RED);
                incomeLine.setTextSize(10f);
                leftAxis.addLimitLine(incomeLine);
                maxValue = Math.max(maxValue, (float) selectedIncome);
            }
        }

        adjustLeftAxisMax(maxValue);
        barChart.invalidate();
        barChart.animateY(400);
    }

    private void setupEmptyBarChart(String message) {
        showBarChart();
        barChart.clear();
        barChart.getAxisLeft().removeAllLimitLines();
        barChart.setNoDataText(message);
        barChart.invalidate();
    }

    private void resetBarChartForNormalBars() {
        barChart.setNoDataText("Không có dữ liệu");
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().removeAllLimitLines();
        barChart.getXAxis().setLabelRotationAngle(0f);
        barChart.setFitBars(true);
    }

    private void configureSingleBarXAxis(String label) {
        XAxis xAxis = barChart.getXAxis();
        xAxis.setCenterAxisLabels(false);
        xAxis.setGranularity(1f);
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(0.5f);
        xAxis.setLabelCount(1, true);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return label;
            }
        });
    }

    private void configureMonthXAxis(boolean groupedBars) {
        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(12, false);
        xAxis.setCenterAxisLabels(groupedBars);

        if (groupedBars) {
            xAxis.setAxisMinimum(0f);
            xAxis.setAxisMaximum(12f);
        } else {
            xAxis.setAxisMinimum(-0.5f);
            xAxis.setAxisMaximum(11.5f);
        }

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = groupedBars ? (int) Math.floor(value) : Math.round(value);

                if (index < 0 || index > 11) {
                    return "";
                }

                return "T" + (index + 1);
            }
        });
    }

    private void adjustLeftAxisMax(float maxValue) {
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);

        if (maxValue <= 0) {
            maxValue = 1f;
        }

        leftAxis.setAxisMaximum(maxValue * 1.25f);
    }

    private ValueFormatter getMoneyValueFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value <= 0) {
                    return "";
                }

                return formatMoneyCompact(value);
            }
        };
    }

    private double[] getMonthlyTotals(String type) {
        double[] totals = new double[12];
        String selectedYear = String.valueOf(getSelectedYear());

        for (Transaction t : allTransactions) {
            if (t == null || t.getDate() == null || t.getType() == null) {
                continue;
            }

            if (!type.equals(t.getType())) {
                continue;
            }

            if (!t.getDate().startsWith(selectedYear + "-")) {
                continue;
            }

            int monthIndex = getMonthIndexFromDate(t.getDate());

            if (isValidMonthIndex(monthIndex)) {
                totals[monthIndex] += t.getAmount();
            }
        }

        return totals;
    }

    private boolean[] getMonthlyExpenseOverLimitFlags(double[] monthlyExpenseTotals) {
        boolean[] result = new boolean[12];

        for (int i = 0; i < 12; i++) {
            result[i] = monthlyExpenseLimit > 0 && monthlyExpenseTotals[i] > monthlyExpenseLimit;
        }

        String selectedYear = String.valueOf(getSelectedYear());

        for (Transaction t : allTransactions) {
            if (t == null || t.getDate() == null || t.getType() == null) {
                continue;
            }

            if (!"chi".equals(t.getType())) {
                continue;
            }

            if (!t.getDate().startsWith(selectedYear + "-")) {
                continue;
            }

            if (t.isOverLimit()) {
                int monthIndex = getMonthIndexFromDate(t.getDate());

                if (isValidMonthIndex(monthIndex)) {
                    result[monthIndex] = true;
                }
            }
        }

        return result;
    }

    private boolean hasAnyAmount(double[] totals) {
        for (double total : totals) {
            if (total > 0) {
                return true;
            }
        }

        return false;
    }

    private String getChartCategoryName(String categoryValue, String transactionType) {
        if (categoryValue == null || categoryValue.trim().isEmpty()) {
            return "Khác";
        }

        String key = categoryValue.trim();
        Map<String, Category> categoryByIdMap;
        Map<String, Category> categoryByNameMap;

        if ("thu".equals(transactionType)) {
            categoryByIdMap = activeIncomeCategoryById;
            categoryByNameMap = activeIncomeCategoryByName;
        } else {
            categoryByIdMap = activeExpenseCategoryById;
            categoryByNameMap = activeExpenseCategoryByName;
        }

        Category categoryById = categoryByIdMap.get(key);
        if (categoryById != null && categoryById.getName() != null && !categoryById.getName().trim().isEmpty()) {
            return categoryById.getName().trim();
        }

        Category categoryByName = categoryByNameMap.get(key);
        if (categoryByName != null && categoryByName.getName() != null && !categoryByName.getName().trim().isEmpty()) {
            return categoryByName.getName().trim();
        }

        return "Khác";
    }

    private boolean isExpenseCategory(Category category) {
        if (category == null) {
            return false;
        }

        String type = category.getType();
        return type == null || type.trim().isEmpty() || "chi".equalsIgnoreCase(type.trim());
    }

    private boolean isIncomeCategory(Category category) {
        if (category == null || category.getType() == null) {
            return false;
        }

        return "thu".equalsIgnoreCase(category.getType().trim());
    }

    private double parseMoneyToDouble(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        boolean negative = text.contains("-");
        String digitsOnly = text.replaceAll("[^0-9]", "");

        if (digitsOnly.isEmpty()) {
            return 0;
        }

        try {
            double value = Double.parseDouble(digitsOnly);
            return negative ? -value : value;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int getSelectedYear() {
        try {
            return Integer.parseInt(selectedYearMonth.substring(0, 4));
        } catch (Exception e) {
            return Calendar.getInstance().get(Calendar.YEAR);
        }
    }

    private int getSelectedMonthIndex() {
        try {
            return Integer.parseInt(selectedYearMonth.substring(5, 7)) - 1;
        } catch (Exception e) {
            return Calendar.getInstance().get(Calendar.MONTH);
        }
    }

    private String getSelectedMonthLabel() {
        int monthIndex = getSelectedMonthIndex();

        if (isValidMonthIndex(monthIndex)) {
            return "T" + (monthIndex + 1);
        }

        return selectedYearMonth;
    }

    private int getMonthIndexFromDate(String date) {
        if (date == null || date.length() < 7) {
            return -1;
        }

        try {
            return Integer.parseInt(date.substring(5, 7)) - 1;
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean isValidMonthIndex(int monthIndex) {
        return monthIndex >= 0 && monthIndex < 12;
    }

    private void updateTabColor() {
        btnTabExpense.setTextColor(currentTab == TAB_EXPENSE ? COLOR_GREEN : COLOR_GRAY);
        btnTabIncome.setTextColor(currentTab == TAB_INCOME ? COLOR_GREEN : COLOR_GRAY);
        btnTabCompare.setTextColor(currentTab == TAB_COMPARE ? COLOR_GREEN : COLOR_GRAY);
    }

    private void showGeneralWarningIfNeeded(double totalIn, double totalOut) {
        if (totalOut > totalIn && totalOut > 0) {
            double overAmount = totalOut - totalIn;
            showWarning("⚠ Chi tiêu đã vượt quá thu nhập " + formatMoney(overAmount));
        } else {
            hideWarning();
        }
    }

    private void showExpenseLimitWarningIfNeeded(double[] monthlyExpenseTotals, boolean[] monthlyExpenseOverLimit) {
        int selectedMonthIndex = getSelectedMonthIndex();

        if (!isValidMonthIndex(selectedMonthIndex)) {
            hideWarning();
            return;
        }

        double selectedExpense = monthlyExpenseTotals[selectedMonthIndex];
        boolean isOverLimit = monthlyExpenseOverLimit[selectedMonthIndex];

        if (!isOverLimit || selectedExpense <= 0) {
            hideWarning();
            return;
        }

        String message;

        if (monthlyExpenseLimit > 0 && selectedExpense > monthlyExpenseLimit) {
            double overAmount = selectedExpense - monthlyExpenseLimit;
            message = "⚠ Chi tiêu tháng " + selectedYearMonth + " đã vượt hạn mức " + formatMoney(overAmount);
        } else {
            message = "⚠ Chi tiêu tháng " + selectedYearMonth + " có giao dịch vượt hạn mức";
        }

        showWarning(message);

        if (!hasShownExpenseLimitWarning) {
            Toast.makeText(this, message.replace("⚠ ", ""), Toast.LENGTH_LONG).show();
            hasShownExpenseLimitWarning = true;
        }
    }

    private void showCompareWarningIfNeeded(double totalIn, double totalOut, boolean allowToast) {
        if (totalOut > totalIn && totalOut > 0) {
            double overAmount = totalOut - totalIn;
            String message = "⚠ Chi tiêu tháng " + selectedYearMonth + " đã vượt thu nhập " + formatMoney(overAmount);
            showWarning(message);

            if (allowToast && !hasShownOverIncomeWarning) {
                Toast.makeText(this, message.replace("⚠ ", ""), Toast.LENGTH_LONG).show();
                hasShownOverIncomeWarning = true;
            }
        } else {
            hideWarning();
        }
    }

    private void showCompareBarWarningIfNeeded(double[] monthlyIncomeTotals, double[] monthlyExpenseTotals) {
        int selectedMonthIndex = getSelectedMonthIndex();

        if (!isValidMonthIndex(selectedMonthIndex)) {
            hideWarning();
            return;
        }

        double selectedIncome = monthlyIncomeTotals[selectedMonthIndex];
        double selectedExpense = monthlyExpenseTotals[selectedMonthIndex];

        if (selectedExpense > selectedIncome && selectedExpense > 0) {
            double overAmount = selectedExpense - selectedIncome;
            String message = "⚠ Chi tiêu tháng " + selectedYearMonth + " đã vượt thu nhập " + formatMoney(overAmount);
            showWarning(message);

            if (!hasShownCompareBarWarning) {
                Toast.makeText(this, message.replace("⚠ ", ""), Toast.LENGTH_LONG).show();
                hasShownCompareBarWarning = true;
            }
        } else {
            hideWarning();
        }
    }

    private void showWarning(String message) {
        tvWarning.setVisibility(View.VISIBLE);
        tvWarning.setText(message);
    }

    private void hideWarning() {
        tvWarning.setVisibility(View.GONE);
    }

    private void resetToastWarnings() {
        hasShownOverIncomeWarning = false;
        hasShownExpenseLimitWarning = false;
        hasShownCompareBarWarning = false;
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
                            tvTitle.setTextColor(COLOR_RED);
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
                        tvAmount.setTextColor(COLOR_RED);
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

    private String formatMoneyCompact(float amount) {
        if (amount >= 1_000_000_000f) {
            return String.format(Locale.GERMANY, "%.1ftỷ", amount / 1_000_000_000f);
        }

        if (amount >= 1_000_000f) {
            return String.format(Locale.GERMANY, "%.1ftr", amount / 1_000_000f);
        }

        if (amount >= 1_000f) {
            return String.format(Locale.GERMANY, "%.0fk", amount / 1_000f);
        }

        return String.format(Locale.GERMANY, "%.0f", amount);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_report);
        }
    }
}
