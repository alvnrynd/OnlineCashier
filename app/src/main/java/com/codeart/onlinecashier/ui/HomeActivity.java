package com.codeart.onlinecashier.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.codeart.onlinecashier.R;
import com.codeart.onlinecashier.databinding.ActivityHomeBinding;
import com.codeart.onlinecashier.models.UserModel;
import com.codeart.onlinecashier.ui.bookkeeping.BookkeepingActivity;
import com.codeart.onlinecashier.ui.inventory.InventoryActivity;
import com.codeart.onlinecashier.ui.sales.SalesActivity;
import com.codeart.onlinecashier.ui.stock.AddStockActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityHomeBinding binding;

    private String firstName, lastName, email, storeId, storeName, noHp, noRek, address, myStore, bankName;
    private String historyId, productId, salesId, invoiceNumber;
    private DatabaseReference refUsers;
    private FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.bg_home);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        binding.cvDataItemHome.setBackgroundResource(R.drawable.bg_round20_gradient);
        binding.cvAddStockHome.setBackgroundResource(R.drawable.bg_round20_gradient);
        binding.cvBookkeepingHome.setBackgroundResource(R.drawable.bg_round20_gradient);
        binding.cvSalesHome.setBackgroundResource(R.drawable.bg_round20_gradient);
        binding.cvEditStoreHome.setBackgroundResource(R.drawable.bg_round20_gradient);

        refUsers = FirebaseDatabase.getInstance().getReference("Users");
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        getDataUser();

        Intent getStore = getIntent();
        myStore = getStore.getStringExtra("storeName");

        binding.imgLogout.setOnClickListener(this);
        binding.cvDataItemHome.setOnClickListener(this);
        binding.cvAddStockHome.setOnClickListener(this);
        binding.cvBookkeepingHome.setOnClickListener(this);
        binding.cvSalesHome.setOnClickListener(this);
        binding.cvEditStoreHome.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.img_logout) {
            showAlertDialogLogout();
        } else if (view.getId() == R.id.cv_data_item_home) {
            moveToActivity(InventoryActivity.class);
        } else if (view.getId() == R.id.cv_add_stock_home) {
            moveToActivity(AddStockActivity.class);
        } else if (view.getId() == R.id.cv_bookkeeping_home) {
            moveToActivity(BookkeepingActivity.class);
        } else if (view.getId() == R.id.cv_sales_home) {
            moveToActivity(SalesActivity.class);
        } else if (view.getId() == R.id.cv_edit_store_home) {
            moveToActivity(EditStoreActivity.class);
        }
    }

    @SuppressWarnings("rawtypes")
    private void moveToActivity(Class activity) {
        Intent moveToActivity = new Intent(HomeActivity.this, activity);
        moveToActivity.putExtra("firstName", firstName);
        moveToActivity.putExtra("lastName", lastName);
        moveToActivity.putExtra("email", email);
        moveToActivity.putExtra("storeId", storeId);
        moveToActivity.putExtra("storeName", storeName);
        moveToActivity.putExtra("noHp", noHp);
        moveToActivity.putExtra("noRek", noRek);
        moveToActivity.putExtra("bankName", bankName);
        moveToActivity.putExtra("address", address);
        moveToActivity.putExtra("productId", productId);
        moveToActivity.putExtra("historyId", historyId);
        moveToActivity.putExtra("salesId", salesId);
        moveToActivity.putExtra("invoiceNumber", invoiceNumber);
        startActivity(moveToActivity);
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
                    firstName = userModel.getFirstName();
                    lastName = userModel.getLastName();
                    storeName = userModel.getStoreName();
                    email = userModel.getEmail();
                    noHp = userModel.getNoHpOwner();
                    noRek = userModel.getNoRekOwner();
                    bankName = userModel.getBankName();
                    address = userModel.getAddress();

                    binding.txtStoreName.setText(storeName);

                    if (storeName != null || myStore != null) {
                        binding.progressBarHome.setVisibility(View.GONE);
                    } else {
                        binding.progressBarHome.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(HomeActivity.this, "Data user tidak ditemukan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showAlertDialogLogout() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Logout");
        alertDialogBuilder
                .setMessage("Apakah Anda ingin keluar dari aplikasi?")
                .setCancelable(false)
                .setPositiveButton("Ya", (dialog, id) -> {

                    FirebaseAuth.getInstance().signOut();

                    startActivity(new Intent(HomeActivity.this, LoginActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                })
                .setNegativeButton("Tidak", (dialog, id) -> dialog.cancel());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}