package com.example.quanlychitieu;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.text.Normalizer;

public class LichSuGDActivity extends AppCompatActivity {

    private TextView tvDate, btnBack;
    private EditText edtSearch;
    private Spinner spType;
    private ListView listHistory;
    private BottomNavigationView bottomNav;

    private DatabaseReference mDatabase;
    private List<Transaction> allTransactions = new ArrayList<>();
    private List<Transaction> filteredTransactions = new ArrayList<>();
    private ArrayAdapter<Transaction> adapter;

    private String selectedDate = "";
    private String selectedType = "Tất cả";
    private String searchText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lich_su_gdactivity);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = (user != null) ? user.getUid() : "test_user_123";
        
        String dbUrl = "https://dbqlchitieu-default-rtdb.asia-southeast1.firebasedatabase.app/";
        mDatabase = FirebaseDatabase.getInstance(dbUrl).getReference("users").child(userId).child("transactions");

        tvDate = findViewById(R.id.tvDate);
        btnBack = findViewById(R.id.btnBack);
        edtSearch = findViewById(R.id.edtSearch);
        spType = findViewById(R.id.spType);
        listHistory = findViewById(R.id.listHistory);
        bottomNav = findViewById(R.id.bottomNav);

        adapter = new ArrayAdapter<Transaction>(this, R.layout.item_history, filteredTransactions) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) convertView = getLayoutInflater().inflate(R.layout.item_history, parent, false);
                Transaction t = getItem(position);
                TextView tvTitle = convertView.findViewById(R.id.tvHistoryTitle);
                TextView tvAmount = convertView.findViewById(R.id.tvHistoryAmount);
                if (t != null) {
                    String title = t.getTitle() != null ? t.getTitle() : "(Không có tiêu đề)";

                    if (t.isOverLimit()) {
                        title = "🚩 " + title + " - Vượt hạn mức";
                        tvTitle.setTextColor(0xFFF44336);
                    } else {
                        tvTitle.setTextColor(0xFF333333);
                    }

                    tvTitle.setText(title);

                    tvAmount.setText((t.getType().equals("chi") ? "-" : "+")
                            + String.format(Locale.GERMANY, "%,.0f", t.getAmount())
                            + " VNĐ");

                    if (t.isOverLimit()) {
                        tvAmount.setTextColor(0xFFF44336);
                    } else {
                        tvAmount.setTextColor(t.getType().equals("chi") ? 0xFFFF0000 : 0xFF2E7D32);
                    }
                }
                return convertView;
            }
        };
        listHistory.setAdapter(adapter);

        setupEvents();
        loadData();
    }

    private void setupEvents() {
        btnBack.setOnClickListener(v -> finish());
        tvDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
                tvDate.setText("Ngày: " + selectedDate);
                filterData();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        String[] types = {"Tất cả", "Thu nhập", "Chi tiêu"};
        spType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types));
        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { selectedType = types[pos]; filterData(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchText = s.toString();
                filterData();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listHistory.setOnItemClickListener((parent, view, position, id) -> {
            Transaction selectedTransaction = filteredTransactions.get(position);

            Intent intent = new Intent(LichSuGDActivity.this, ChiTietActivity.class);
            intent.putExtra("TRANSACTION_DATA", selectedTransaction);
            startActivity(intent);
        });

        bottomNav.setSelectedItemId(R.id.nav_history);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_history) return true;
            
            Intent intent = null;
            if (id == R.id.nav_home) intent = new Intent(this, MainActivity.class);
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

    private void loadData() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allTransactions.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Transaction t = data.getValue(Transaction.class);
                    if (t != null) allTransactions.add(t);
                }
                filterData();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterData() {
        filteredTransactions.clear();

        String keyword = normalize(searchText);

        for (Transaction t : allTransactions) {
            if (t == null || t.getDate() == null || t.getType() == null) continue;

            boolean dateMatch = selectedDate.isEmpty() || t.getDate().startsWith(selectedDate);

            boolean typeMatch =
                    selectedType.equals("Tất cả")
                            || (selectedType.equals("Thu nhập") && "thu".equals(t.getType()))
                            || (selectedType.equals("Chi tiêu") && "chi".equals(t.getType()));

            String title = normalize(t.getTitle());
            String note = normalize(t.getNote());
            String category = normalize(t.getCategoryId());
            String relatedPerson = normalize(t.getRelatedPerson());
            boolean searchMatch =
                    keyword.isEmpty()
                            || title.contains(keyword)
                            || note.contains(keyword)
                            || category.contains(keyword)
                            || relatedPerson.contains(keyword);

            if (dateMatch && typeMatch && searchMatch) {
                filteredTransactions.add(t);
            }
        }

        adapter.notifyDataSetChanged();
    }
    private String normalize(String text) {
        if (text == null) return "";
        String result = text.trim().toLowerCase();
        result = Normalizer.normalize(result, Normalizer.Form.NFD);
        result = result.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return result;
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNav.setSelectedItemId(R.id.nav_history);
    }
}
