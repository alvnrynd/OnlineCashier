package com.codeart.onlinecashier.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.codeart.onlinecashier.R;
import com.codeart.onlinecashier.databinding.ActivityEditStoreBinding;
import com.codeart.onlinecashier.models.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class EditStoreActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityEditStoreBinding binding;

    private DatabaseReference refUsers, refStores;
    private FirebaseUser firebaseUser;

    private String firstName, lastName, email, storeId, storeName, noHp, noRek, address, bankName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.bg_top_color);
        binding = ActivityEditStoreBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        refUsers = FirebaseDatabase.getInstance().getReference("Users");
        refStores = FirebaseDatabase.getInstance().getReference("Stores");
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        getDataUser();
        setEditTextField();

        binding.btnSave.setOnClickListener(this);
        binding.imgBack.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.img_back) {
            super.onBackPressed();
        } else if (view.getId() == R.id.btn_save) {
            String storeName = binding.etStoreName.getText().toString();
            String noHp = binding.etNumberOwner.getText().toString();
            String noRek = binding.etNumberRekOwner.getText().toString();
            String address = binding.etAddressOwner.getText().toString();
            String bankName = binding.etBankName.getText().toString();
            if (noHp.length() > 12){
                binding.etNumberOwner.setError("Nomor hp maksimal 12 huruf");
                binding.etNumberOwner.requestFocus();
                return;
            }

            if (!TextUtils.isEmpty(storeName) && !TextUtils.isEmpty(noHp) && !TextUtils.isEmpty(noRek) &&
                    !TextUtils.isEmpty(address) && !TextUtils.isEmpty(bankName)) {
                registryStore(storeName, noHp, noRek, address, bankName);
            } else {
                Toast.makeText(this, "Tolong lengkapi semua field!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getDataUser() {
        Intent data = getIntent();
        firstName = data.getStringExtra("firstName");
        lastName = data.getStringExtra("lastName");
        email = data.getStringExtra("email");
        storeId = data.getStringExtra("storeId");
        storeName = data.getStringExtra("storeName");
        noHp = data.getStringExtra("noHp");
        noRek = data.getStringExtra("noRek");
        address = data.getStringExtra("address");
        bankName = data.getStringExtra("bankName");
    }

    private void setEditTextField() {
        if (firstName != null && lastName != null && email != null) {
            binding.etNameOwner.setText(String.format("%s %s", firstName, lastName));
            binding.etEmailOwner.setText(email);
            binding.etStoreName.setText(storeName);
            binding.etNumberOwner.setText(noHp);
            binding.etNumberRekOwner.setText(noRek);
            binding.etAddressOwner.setText(address);
            binding.etBankName.setText(bankName);
        }
    }

    private void registryStore(String storeName, String noHp, String noRek, String address, String bankName) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Tunggu sebentar ya..");
        progressDialog.show();
        progressDialog.setCancelable(false);

        String userId = firebaseUser.getUid();
        UserModel userModel = new UserModel();

        // generate id owner store
        if (storeId.equals("null")) {
            storeId = refStores.child(userId).push().getKey();
            userModel.setStoreId(storeId);
        }

        HashMap<String, Object> hashUsers = new HashMap<>();
        hashUsers.put("storeId", storeId);
        hashUsers.put("storeName", storeName);
        hashUsers.put("noHpOwner", noHp);
        hashUsers.put("noRekOwner", noRek);
        hashUsers.put("address", address);
        hashUsers.put("bankName", bankName);

        refUsers.child(userId).updateChildren(hashUsers).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // create/update store
                HashMap<String, Object> hashStores = new HashMap<>();
                hashStores.put("nameOwner", firstName + " " + lastName);
                hashStores.put("storeId", storeId);
                hashStores.put("userId", userId);
                hashStores.put("storeName", storeName);

                refStores.child(storeId).setValue(hashStores).addOnCompleteListener(task1 -> {
                    Toast.makeText(this, "Toko berhasil diperbarui", Toast.LENGTH_SHORT).show();

                    Intent toHome = new Intent(EditStoreActivity.this, HomeActivity.class);
                    toHome.putExtra("storeName", storeName);
                    startActivity(toHome);
                    finish();
                }).addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());

            } else {
                Toast.makeText(this, "Maaf, Toko gagal dibuat", Toast.LENGTH_SHORT).show();
            }
            progressDialog.dismiss();
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}