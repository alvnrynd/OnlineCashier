package com.codeart.onlinecashier.ui.inventory;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.codeart.onlinecashier.R;
import com.codeart.onlinecashier.databinding.ActivityInventoryBinding;
import com.codeart.onlinecashier.models.ProductModel;
import com.codeart.onlinecashier.models.UserModel;
import com.codeart.onlinecashier.ui.AddItemActivity;
import com.codeart.onlinecashier.ui.HomeActivity;
import com.codeart.onlinecashier.ui.sales.SalesActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

public class InventoryActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityInventoryBinding binding;

    private DatabaseReference refUsers, refProducts;
    private FirebaseUser firebaseUser;

    private String historyId, storeId, productId, salesId, invoiceNumber, noHp, noRek, bankName, storeName;

    private AdapterInventory adapterInventory;
    private ArrayList<ProductModel> productModelArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.bg_login);
        binding = ActivityInventoryBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        refUsers = FirebaseDatabase.getInstance().getReference("Users");
        refProducts = FirebaseDatabase.getInstance().getReference("Products");

        binding.rvAllProduct.setHasFixedSize(true);
        binding.rvAllProduct.setLayoutManager(new LinearLayoutManager(this));

        Intent data = getIntent();
        if (data != null) {
            storeId = data.getStringExtra("storeId");
            productId = data.getStringExtra("productId");
            historyId = data.getStringExtra("historyId");
            salesId = data.getStringExtra("salesId");
            invoiceNumber = data.getStringExtra("invoiceNumber");
            noHp = data.getStringExtra("noHp");
            noRek = data.getStringExtra("noRek");
            bankName = data.getStringExtra("bankName");
            storeName = data.getStringExtra("storeName");
        }

        getDataUser();
        //getDataStore();
        getAllProduct();

        binding.fabAddItem.setOnClickListener(this);
        binding.imgBackInventory.setOnClickListener(this);
        binding.btnLlCartSales.setOnClickListener(this);
        binding.cvSearchProduct.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fab_add_item) {
            if (!storeId.equals("null")) {
                moveToActivity(AddItemActivity.class);
            } else {
                Toast.makeText(this, "Silahkan membuat toko dahulu!", Toast.LENGTH_SHORT).show();
            }
        } else if (view.getId() == R.id.img_back_inventory) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        } else if (view.getId() == R.id.btn_ll_cart_sales) {
            moveToActivity(SalesActivity.class);
        } else if (view.getId() == R.id.cv_search_product) {
            Dialog dialog = new Dialog(this);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setContentView(R.layout.dialog_search_product);
            dialog.setCancelable(false);
            dialog.show();

            EditText etSearchProduct = dialog.findViewById(R.id.et_search_product);
            RecyclerView rvSearchProduct = dialog.findViewById(R.id.rv_search_product);
            TextView txtClose = dialog.findViewById(R.id.txt_close_dialog);

            rvSearchProduct.setHasFixedSize(true);
            rvSearchProduct.setLayoutManager(new LinearLayoutManager(this));

            etSearchProduct.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String itemName = etSearchProduct.getText().toString();

                    searchProduct(itemName, rvSearchProduct);
                    etSearchProduct.setText("");
                    hideSoftKeyboard();
                    return true;
                }
                return false;
            });

            txtClose.setOnClickListener(view1 -> dialog.dismiss());
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    @SuppressWarnings("rawtypes")
    private void moveToActivity(Class activity) {
        Intent moveToActivity = new Intent(InventoryActivity.this, activity);
        moveToActivity.putExtra("storeId", storeId);
        moveToActivity.putExtra("productId", productId);
        moveToActivity.putExtra("historyId", historyId);
        moveToActivity.putExtra("salesId", salesId);
        moveToActivity.putExtra("invoiceNumber", invoiceNumber);
        moveToActivity.putExtra("noHp", noHp);
        moveToActivity.putExtra("noRek", noRek);
        moveToActivity.putExtra("bankName", bankName);
        moveToActivity.putExtra("storeName", storeName);
        startActivity(moveToActivity);
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = this.getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void searchProduct(String itemName, RecyclerView rvSearchProduct) {
        productModelArrayList = new ArrayList<>();
        String searchKey = itemName.toLowerCase();

        if (productId != null) {
            Query query = refProducts.child(productId).orderByChild("searchItem")
                    .startAt(searchKey).endAt(searchKey + "\uf8ff");
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    productModelArrayList.clear();
                    if (!TextUtils.isEmpty(searchKey)) {
                        if (!snapshot.exists()) {
                            rvSearchProduct.setVisibility(View.GONE);
                            Toast.makeText(InventoryActivity.this, "Produk tidak ditemukan", Toast.LENGTH_SHORT).show();
                        } else {
                            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                ProductModel productModel = dataSnapshot.getValue(ProductModel.class);

                                productModelArrayList.add(productModel);
                            }

                            rvSearchProduct.setVisibility(View.VISIBLE);
                            adapterInventory = new AdapterInventory(InventoryActivity.this, productModelArrayList);
                            rvSearchProduct.setAdapter(adapterInventory);
                            adapterInventory.notifyDataSetChanged();
                        }
                    } else {
                        rvSearchProduct.setVisibility(View.GONE);
                        Toast.makeText(InventoryActivity.this, "Tolong, masukan nama barang\nyang ingin dicari", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(InventoryActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void getAllProduct() {
        productModelArrayList = new ArrayList<>();
        if (productId != null) {
            refProducts.child(productId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    productModelArrayList.clear();
                    if (snapshot.exists()) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            ProductModel productModel = dataSnapshot.getValue(ProductModel.class);
                            if (refProducts != null) {
                                productModelArrayList.add(productModel);
                            }
                        }

                        adapterInventory = new AdapterInventory(InventoryActivity.this, productModelArrayList);
                        binding.rvAllProduct.setAdapter(adapterInventory);
                        adapterInventory.notifyDataSetChanged();

                        binding.txtItemIsEmpty.setVisibility(View.GONE);
                    } else {
                        binding.txtItemIsEmpty.setVisibility(View.VISIBLE);
                    }
                    binding.progressBarInventory.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(InventoryActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "productId NULL", Toast.LENGTH_SHORT).show();
        }
    }

    private void getDataUser() {
        refUsers.child(firebaseUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserModel userModel = snapshot.getValue(UserModel.class);
                if (snapshot.exists()) {
                    assert userModel != null;
                    storeId = userModel.getStoreId();
                    productId = userModel.getProductId();
                    historyId = userModel.getHistoryId();
                    salesId = userModel.getSalesId();
                    invoiceNumber = userModel.getInvoiceNumber();
                } else {
                    Toast.makeText(InventoryActivity.this, "Data user tidak ditemukan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}