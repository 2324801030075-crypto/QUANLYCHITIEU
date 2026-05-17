package com.example.quanlychitieu;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuanLyDanhMucActivity extends AppCompatActivity {

    private TextView btnBack;
    private Button btnAddCategory, btnTabIncome, btnTabExpense;
    private RecyclerView rvCategoryList;
    private BottomNavigationView bottomNav;

    private DatabaseReference mCategoryRef;
    private DatabaseReference mTransactionRef;

    private final List<Category> categoryList = new ArrayList<>();
    private final List<Category> rawCategoryList = new ArrayList<>();
    private final List<Transaction> transactionList = new ArrayList<>();
    private final List<String> iconList = new ArrayList<>();

    private CategoryAdapter adapter;
    private String currentCategoryType = "thu";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_danh_muc);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = (user != null) ? user.getUid() : "test_user_123";

        String dbUrl = "https://dbqlchitieu-default-rtdb.asia-southeast1.firebasedatabase.app/";
        DatabaseReference userRef = FirebaseDatabase.getInstance(dbUrl)
                .getReference("users")
                .child(userId);

        mCategoryRef = userRef.child("categories");
        mTransactionRef = userRef.child("transactions");

        btnBack = findViewById(R.id.btnBack);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        btnTabIncome = findViewById(R.id.btnTabIncome);
        btnTabExpense = findViewById(R.id.btnTabExpense);
        rvCategoryList = findViewById(R.id.rvCategoryList);
        bottomNav = findViewById(R.id.bottomNav);

        iconList.addAll(getIcons8DrawableNames());

        rvCategoryList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter(categoryList, new CategoryAdapter.OnCategoryActionListener() {
            @Override
            public void onItemClick(Category category) {
                showCategoryOptionsDialog(category);
            }

            @Override
            public void onItemLongClick(Category category) {
                showDeleteCategoryDialog(category);
            }
        });
        rvCategoryList.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnAddCategory.setOnClickListener(v -> showCategoryDialog(null));

        btnTabIncome.setOnClickListener(v -> {
            currentCategoryType = "thu";
            updateTabUI();
            rebuildCategoryDisplay();
        });

        btnTabExpense.setOnClickListener(v -> {
            currentCategoryType = "chi";
            updateTabUI();
            rebuildCategoryDisplay();
        });

        setupBottomNav();
        updateTabUI();
        listenCategories();
        listenTransactions();
    }

    private void updateTabUI() {
        int green = Color.parseColor("#2E7D32");
        int gray = Color.parseColor("#757575");

        if ("thu".equals(currentCategoryType)) {
            btnTabIncome.setTextColor(green);
            btnTabExpense.setTextColor(gray);

            btnAddCategory.setText("+ Thêm danh mục thu nhập");
            btnAddCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            btnTabIncome.setTextColor(gray);
            btnTabExpense.setTextColor(green);

            btnAddCategory.setText("+ Thêm danh mục chi tiêu");
            btnAddCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
        }
    }

    private void showDeleteCategoryDialog(Category category) {
        String[] options = {"Xóa"};

        new AlertDialog.Builder(this)
                .setTitle(category.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        confirmDeleteCategory(category);
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void confirmDeleteCategory(Category category) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa danh mục")
                .setMessage("Bạn có chắc muốn xóa danh mục \"" + category.getName() + "\" không?\n\n"
                        + "Danh mục này sẽ không còn hiển thị khi thêm giao dịch mới.\n"
                        + "Các giao dịch cũ vẫn được giữ nguyên.")
                .setPositiveButton("Xóa", (dialog, which) -> softDeleteCategory(category))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void softDeleteCategory(Category category) {
        if (category == null || TextUtils.isEmpty(category.getId())) {
            Toast.makeText(this, "Danh mục không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        mCategoryRef.child(category.getId()).child("deleted").setValue(true)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Đã xóa danh mục!", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Xóa danh mục thất bại!", Toast.LENGTH_SHORT).show()
                );
    }

    private void setupBottomNav() {
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, LichSuGDActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_report) {
                startActivity(new Intent(this, BCThongKeActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, CaiDatActivity.class));
                finish();
                return true;
            }

            return false;
        });
    }

    private void listenCategories() {
        mCategoryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rawCategoryList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Category cat = data.getValue(Category.class);

                    if (cat != null && !cat.isDeleted()) {
                        if (TextUtils.isEmpty(cat.getType())) {
                            cat.setType("chi");
                        }

                        rawCategoryList.add(cat);
                    }
                }

                rebuildCategoryDisplay();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QuanLyDanhMucActivity.this, "Không tải được danh mục!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenTransactions() {
        mTransactionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                transactionList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Transaction t = data.getValue(Transaction.class);

                    if (t != null) {
                        transactionList.add(t);
                    }
                }

                rebuildCategoryDisplay();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QuanLyDanhMucActivity.this, "Không tải được giao dịch!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rebuildCategoryDisplay() {
        categoryList.clear();

        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        Map<String, Double> monthlyMap = new LinkedHashMap<>();

        for (Transaction t : transactionList) {
            if (t.getDate() == null || t.getType() == null) continue;
            if (!currentCategoryType.equals(t.getType())) continue;
            if (!t.getDate().startsWith(currentMonth)) continue;

            String categoryName = t.getCategoryId();

            if (TextUtils.isEmpty(categoryName)) {
                categoryName = t.getTitle();
            }

            if (TextUtils.isEmpty(categoryName)) {
                categoryName = "Khác";
            }

            double oldValue = monthlyMap.containsKey(categoryName) ? monthlyMap.get(categoryName) : 0.0;
            monthlyMap.put(categoryName, oldValue + t.getAmount());
        }

        for (Category raw : rawCategoryList) {
            String rawType = raw.getType();

            if (TextUtils.isEmpty(rawType)) {
                rawType = "chi";
            }

            if (!currentCategoryType.equals(rawType)) {
                continue;
            }

            double total = monthlyMap.containsKey(raw.getName()) ? monthlyMap.get(raw.getName()) : 0.0;

            Category displayCategory = new Category(
                    raw.getId(),
                    raw.getName(),
                    raw.getIconName(),
                    String.format(Locale.GERMANY, "%,.0f VNĐ", total),
                    false,
                    currentCategoryType
            );

            categoryList.add(displayCategory);
        }

        adapter.notifyDataSetChanged();
    }

    private void showCategoryOptionsDialog(Category category) {
        String[] options = {"Sửa"};

        new AlertDialog.Builder(this)
                .setTitle(category.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showCategoryDialog(category);
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showCategoryDialog(Category categoryToEdit) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category, null);

        ImageView imgSelectedIcon = dialogView.findViewById(R.id.imgSelectedIcon);
        EditText etCatName = dialogView.findViewById(R.id.etCatName);
        RecyclerView rvIconPicker = dialogView.findViewById(R.id.rvIconPicker);
        Button btnSaveCat = dialogView.findViewById(R.id.btnSaveCat);

        rvIconPicker.setLayoutManager(new GridLayoutManager(this, 4));

        if (iconList.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy icon có tiền tố icons8_", Toast.LENGTH_SHORT).show();
        }

        final boolean isEdit = categoryToEdit != null;
        final String[] selectedIconName = {""};

        if (isEdit) {
            etCatName.setText(categoryToEdit.getName());
            selectedIconName[0] = categoryToEdit.getIconName() != null ? categoryToEdit.getIconName() : "";
            btnSaveCat.setText("LƯU THAY ĐỔI");
        } else {
            selectedIconName[0] = iconList.isEmpty() ? "" : iconList.get(0);
            btnSaveCat.setText("THÊM DANH MỤC");
        }

        updatePreviewIcon(imgSelectedIcon, selectedIconName[0]);

        IconAdapter iconAdapter = new IconAdapter(iconList, iconName -> {
            selectedIconName[0] = iconName;
            updatePreviewIcon(imgSelectedIcon, iconName);
        });
        rvIconPicker.setAdapter(iconAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnSaveCat.setOnClickListener(v -> {
            String categoryName = etCatName.getText().toString().trim();

            if (TextUtils.isEmpty(categoryName)) {
                Toast.makeText(this, "Vui lòng nhập tên danh mục!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(selectedIconName[0])) {
                Toast.makeText(this, "Vui lòng chọn icon!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEdit) {
                saveEditedCategory(categoryToEdit, categoryName, selectedIconName[0], dialog);
            } else {
                saveNewCategory(categoryName, selectedIconName[0], dialog);
            }
        });

        dialog.show();
    }

    private void saveNewCategory(String categoryName, String iconName, AlertDialog dialog) {
        String id = mCategoryRef.push().getKey();

        if (id == null) {
            Toast.makeText(this, "Không tạo được danh mục!", Toast.LENGTH_SHORT).show();
            return;
        }

        Category category = new Category(id, categoryName, iconName, "0 VNĐ", false, currentCategoryType);

        mCategoryRef.child(id).setValue(category)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Đã thêm danh mục!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lưu danh mục thất bại!", Toast.LENGTH_SHORT).show()
                );
    }

    private void saveEditedCategory(Category oldCategory, String newName, String newIconName, AlertDialog dialog) {
        String id = oldCategory.getId();

        if (TextUtils.isEmpty(id)) {
            Toast.makeText(this, "Danh mục không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        Category updatedCategory = new Category(
                id,
                newName,
                newIconName,
                oldCategory.getAmount(),
                false,
                currentCategoryType
        );

        mCategoryRef.child(id).setValue(updatedCategory)
                .addOnSuccessListener(unused -> {
                    if (!TextUtils.equals(oldCategory.getName(), newName)) {
                        updateTransactionsCategoryName(oldCategory.getName(), newName, dialog);
                    } else {
                        Toast.makeText(this, "Đã cập nhật danh mục!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Cập nhật danh mục thất bại!", Toast.LENGTH_SHORT).show()
                );
    }

    private void updateTransactionsCategoryName(String oldName, String newName, AlertDialog dialog) {
        mTransactionRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Transaction t = data.getValue(Transaction.class);
                    if (t == null) continue;

                    if (!currentCategoryType.equals(t.getType())) {
                        continue;
                    }

                    boolean needUpdateCategory = oldName.equals(t.getCategoryId());
                    boolean needUpdateTitle = oldName.equals(t.getTitle());

                    if (needUpdateCategory) {
                        data.getRef().child("categoryId").setValue(newName);
                    }

                    if (needUpdateTitle) {
                        data.getRef().child("title").setValue(newName);
                    }
                }

                Toast.makeText(QuanLyDanhMucActivity.this, "Đã cập nhật danh mục!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(QuanLyDanhMucActivity.this, "Không cập nhật được giao dịch liên quan!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }

    private void updatePreviewIcon(ImageView imageView, String iconName) {
        int resId = getResources().getIdentifier(iconName, "drawable", getPackageName());

        if (resId != 0) {
            imageView.setImageResource(resId);
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private List<String> getIcons8DrawableNames() {
        List<String> result = new ArrayList<>();

        Field[] fields = R.drawable.class.getDeclaredFields();

        for (Field field : fields) {
            String name = field.getName();

            if (name.startsWith("icons8_")) {
                result.add(name);
            }
        }

        Collections.sort(result);
        return result;
    }

    private static class IconAdapter extends RecyclerView.Adapter<IconAdapter.IconViewHolder> {

        interface OnIconClickListener {
            void onIconClick(String iconName);
        }

        private final List<String> iconList;
        private final OnIconClickListener listener;

        public IconAdapter(List<String> iconList, OnIconClickListener listener) {
            this.iconList = iconList;
            this.listener = listener;
        }

        @NonNull
        @Override
        public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon, parent, false);
            return new IconViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
            String iconName = iconList.get(position);

            int resId = holder.itemView.getContext()
                    .getResources()
                    .getIdentifier(iconName, "drawable", holder.itemView.getContext().getPackageName());

            if (resId != 0) {
                holder.imgIconItem.setImageResource(resId);
            } else {
                holder.imgIconItem.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            holder.imgIconItem.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onIconClick(iconName);
                }
            });
        }

        @Override
        public int getItemCount() {
            return iconList != null ? iconList.size() : 0;
        }

        static class IconViewHolder extends RecyclerView.ViewHolder {
            ImageView imgIconItem;

            public IconViewHolder(@NonNull View itemView) {
                super(itemView);
                imgIconItem = itemView.findViewById(R.id.imgIconItem);
            }
        }
    }
}