package com.codeart.onlinecashier.ui.stock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.codeart.onlinecashier.R;
import com.codeart.onlinecashier.databinding.ActivityAddStockBinding;
import com.codeart.onlinecashier.models.ProductModel;
import com.codeart.onlinecashier.ui.HomeActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AddStockActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityAddStockBinding binding;

    private DatabaseReference refProducts;
    private String productId;

    private AdapterAddStock adapterAddStock;
    private ArrayList<ProductModel> productModelArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.bg_full_color);
        binding = ActivityAddStockBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        refProducts = FirebaseDatabase.getInstance().getReference("Products");

        binding.rvAddStock.setHasFixedSize(true);
        binding.rvAddStock.setLayoutManager(new LinearLayoutManager(this));

        Intent data = getIntent();
        if (data != null) {
            productId = data.getStringExtra("productId");
        }

        getAllProduct();

        binding.imgBackAddStock.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.img_back_add_stock) {
            super.onBackPressed();
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
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

                        adapterAddStock = new AdapterAddStock(AddStockActivity.this, productModelArrayList);
                        binding.rvAddStock.setAdapter(adapterAddStock);
                        adapterAddStock.notifyDataSetChanged();

                        binding.txtItemIsEmpty.setVisibility(View.GONE);
                    } else {
                        binding.txtItemIsEmpty.setVisibility(View.VISIBLE);
                    }
                    binding.progressBarAddStock.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(AddStockActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "productId NULL", Toast.LENGTH_SHORT).show();
        }
    }
}