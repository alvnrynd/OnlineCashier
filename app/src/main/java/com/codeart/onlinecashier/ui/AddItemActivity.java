package com.codeart.onlinecashier.ui;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.codeart.onlinecashier.R;
import com.codeart.onlinecashier.databinding.ActivityAddItemBinding;
import com.codeart.onlinecashier.models.UserModel;
import com.codeart.onlinecashier.ui.inventory.InventoryActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class AddItemActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityAddItemBinding binding;

    private Uri mImageUri;
    private StorageTask<UploadTask.TaskSnapshot> uploadTask;
    private StorageReference storageReference;
    private FirebaseUser firebaseUser;
    private DatabaseReference refUsers, refProducts, refHistories;

    private String storeId, productId, historyId, noHp, noRek, bankName, storeName;
    private String type, imageItem, itemId, tempTotal;
    private String dateString, monthString, yearString;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.bg_full_color);
        binding = ActivityAddItemBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        binding.cvFormAddItem.setBackgroundResource(R.drawable.bg_round20_white);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        refUsers = FirebaseDatabase.getInstance().getReference("Users");
        refProducts = FirebaseDatabase.getInstance().getReference("Products");
        refHistories = FirebaseDatabase.getInstance().getReference("Histories");
        storageReference = FirebaseStorage.getInstance().getReference("ImageItems");

        calendar = Calendar.getInstance();

        Date date = new Date();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            int year = localDate.getYear();
            int month = localDate.getMonthValue();
            int day = localDate.getDayOfMonth();

            // convert to string
            dateString = String.valueOf(day);
            monthString = String.valueOf(month);
            yearString = String.valueOf(year);
        }

        Intent data = getIntent();
        if (data != null) {
            storeId = data.getStringExtra("storeId");
            productId = data.getStringExtra("productId");
            historyId = data.getStringExtra("historyId");
            type = data.getStringExtra("type");
            noHp = data.getStringExtra("noHp");
            noRek = data.getStringExtra("noRek");
            bankName = data.getStringExtra("bankName");
            storeName = data.getStringExtra("storeName");
        }

        if (type != null) {
            if (type.equals("edit")) {
                binding.etTotalItems.setVisibility(View.GONE);
                binding.etBuyPrice.setVisibility(View.GONE);
                binding.txtTitle.setText(R.string.edit_barang);

                String sellPrice = data.getStringExtra("sellPrice");
                binding.etSellPrice.setText(sellPrice);
            } else if (type.equals("stock")) {
                binding.etSellPrice.setVisibility(View.GONE);
                binding.imgUploadItem.setEnabled(false);
                binding.etItemName.setEnabled(false);
                binding.txtTitle.setText(R.string.penambahan_stok);

                String buyPrice = data.getStringExtra("buyPrice");
                tempTotal = data.getStringExtra("total");
                binding.etBuyPrice.setText(buyPrice);
                binding.etTotalItems.setText(tempTotal);
            }

            imageItem = data.getStringExtra("imageItem");
            itemId = data.getStringExtra("itemId");
            productId = data.getStringExtra("productId");
            String itemName = data.getStringExtra("itemName");

            Picasso.get().load(imageItem).into(binding.imgUploadItem);
            binding.btnInputData.setText(R.string.update_data);
            binding.etItemName.setText(itemName);
        }

        binding.imgBack.setOnClickListener(this);
        binding.btnInputData.setOnClickListener(this);
        binding.imgUploadItem.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        String itemName = binding.etItemName.getText().toString();
        String sellPrice = binding.etSellPrice.getText().toString();
        String buyPrice = binding.etBuyPrice.getText().toString();
        String total = binding.etTotalItems.getText().toString();

        if (view.getId() == R.id.img_back) {
            super.onBackPressed();
            finish();
        } else if (view.getId() == R.id.img_upload_item) {
            CropImage.activity().setAspectRatio(1, 1).start(this);
        } else if (view.getId() == R.id.btn_input_data) {
            if (type != null) {
                if (type.equals("edit")) {
                    if (!TextUtils.isEmpty(itemName) && !TextUtils.isEmpty(sellPrice)) { // mImageUri = null = no update image
                        if (imageItem != null) {
                            editItem(productId, itemId, imageItem, itemName, sellPrice);
                        }
                    } else {
                        Toast.makeText(this, "Tolong lengkapi semua field", Toast.LENGTH_SHORT).show();
                    }
                } else if (type.equals("stock")) {
                    if (!TextUtils.isEmpty(buyPrice) && !TextUtils.isEmpty(total)) {
                        addStockItem(historyId, productId, itemId, buyPrice, total);
                    } else {
                        Toast.makeText(this, "Tolong lengkapi semua field", Toast.LENGTH_SHORT).show();
                    }
                }
            } else { // for add new item
                if (mImageUri != null && !TextUtils.isEmpty(itemName) && !TextUtils.isEmpty(sellPrice) &&
                        !TextUtils.isEmpty(buyPrice) && !TextUtils.isEmpty(total)) {

                    // todo cek before save
                    searchProductBeforeSave(itemName, sellPrice, buyPrice, total, storeId);
                } else {
                    Toast.makeText(this, "Tolong lengkapi semua field", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public String getCurrentLocalTimeStamp() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        return currentTime.format(calendar.getTime());
    }

    private void addStockItem(String historyId, String productId, String itemId, String buyPrice, String stock) {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Tunggu sebentar ya");
        pd.setCancelable(false);
        pd.show();

        int outcome = Integer.parseInt(buyPrice) * Integer.parseInt(stock);
        int newTotal = Integer.parseInt(tempTotal) + Integer.parseInt(stock);

        String dateCal = dateString + "-" + monthString + "-" + yearString;
        String monthCal = monthString + "-" + yearString;

        HashMap<String, Object> hashUpdateProducts = new HashMap<>();
        hashUpdateProducts.put("time", String.valueOf(getCurrentLocalTimeStamp()));
        hashUpdateProducts.put("buyPrice", buyPrice);
        hashUpdateProducts.put("total", String.valueOf(newTotal));

        refProducts.child(productId).child(itemId).updateChildren(hashUpdateProducts).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // UPDATE item - create newItemId in Histories (supaya dapat terlihat di riwayat pengeluaran)
                String newItemId = refProducts.child(productId).push().getKey();
                HashMap<String, Object> hashUpdateHistories = new HashMap<>();
                hashUpdateHistories.put("time", String.valueOf(getCurrentLocalTimeStamp()));
                hashUpdateHistories.put("date", dateCal);
                hashUpdateHistories.put("month", monthCal);
                hashUpdateHistories.put("year", yearString);
                hashUpdateHistories.put("historyId", historyId);
                hashUpdateHistories.put("buyTotal", stock);
                hashUpdateHistories.put("buyPrice", buyPrice);
                hashUpdateHistories.put("discountPercent", "0");
                hashUpdateHistories.put("discountPrice", "0");
                hashUpdateHistories.put("income", "0");
                hashUpdateHistories.put("outcome", String.valueOf(outcome));
                hashUpdateHistories.put("itemId", itemId);

                if (newItemId != null) {
                    refHistories.child(historyId).child(newItemId).setValue(hashUpdateHistories);
                }


                pd.dismiss();
                Intent toInventory = new Intent(this, InventoryActivity.class);
                toInventory.putExtra("productId", productId);
                toInventory.putExtra("storeId", storeId);
                toInventory.putExtra("noHp", noHp);
                toInventory.putExtra("noRek", noRek);
                toInventory.putExtra("bankName", bankName);
                toInventory.putExtra("storeName", storeName);
                startActivity(toInventory);
                finish();

                Toast.makeText(this, "Barang berhasil diperbarui", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Barang gagal diperbarui", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void editItem(String productId, String itemId, String imageItem, String itemName, String sellPrice) {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Tunggu sebentar ya");
        pd.setCancelable(false);
        pd.show();

        HashMap<String, Object> hashUpdateItem = new HashMap<>();

        if (mImageUri != null) { // update image item
            hashUpdateItem.put("time", String.valueOf(getCurrentLocalTimeStamp()));
            hashUpdateItem.put("imageItem", "null");
            hashUpdateItem.put("itemName", itemName);
            hashUpdateItem.put("searchItem", itemName.toLowerCase());
            hashUpdateItem.put("sellPrice", sellPrice);

            refProducts.child(productId).child(itemId).updateChildren(hashUpdateItem).addOnCompleteListener(task -> {
                // for save image items
                final StorageReference fileReference = storageReference.child("img-"
                        + itemName.toLowerCase().concat("-")
                        + System.currentTimeMillis() + ".jpg");

                uploadTask = fileReference.putFile(mImageUri);
                uploadTask.continueWithTask(task1 -> {
                    if (!task1.isSuccessful()) {
                        throw Objects.requireNonNull(task1.getException());
                    }
                    return fileReference.getDownloadUrl();
                }).addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful()) {
                        Uri downloadUri = task2.getResult();
                        assert downloadUri != null;
                        String mUri = downloadUri.toString();

                        // UPDATE imageItem in Products
                        HashMap<String, Object> hashUpdateProducts = new HashMap<>();
                        hashUpdateProducts.put("time", String.valueOf(getCurrentLocalTimeStamp()));
                        hashUpdateProducts.put("imageItem", mUri);
                        refProducts.child(productId).child(itemId).updateChildren(hashUpdateProducts);

                        Toast.makeText(this, "Barang berhasil diperbarui", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "upload image failed", Toast.LENGTH_SHORT).show();
                    }
                    pd.dismiss();

                    Intent toInventory = new Intent(this, InventoryActivity.class);
                    toInventory.putExtra("productId", productId);
                    toInventory.putExtra("storeId", storeId);
                    toInventory.putExtra("noHp", noHp);
                    toInventory.putExtra("noRek", noRek);
                    toInventory.putExtra("bankName", bankName);
                    toInventory.putExtra("storeName", storeName);
                    startActivity(toInventory);
                    finish();
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                });
            });
        } else { // just update name item or sell price
            hashUpdateItem.put("time", String.valueOf(getCurrentLocalTimeStamp()));
            hashUpdateItem.put("imageItem", imageItem);
            hashUpdateItem.put("itemName", itemName);
            hashUpdateItem.put("searchItem", itemName.toLowerCase());
            hashUpdateItem.put("sellPrice", sellPrice);

            refProducts.child(productId).child(itemId).updateChildren(hashUpdateItem).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    pd.dismiss();
                    Intent toInventory = new Intent(this, InventoryActivity.class);
                    toInventory.putExtra("productId", productId);
                    toInventory.putExtra("storeId", storeId);
                    toInventory.putExtra("noHp", noHp);
                    toInventory.putExtra("noRek", noRek);
                    toInventory.putExtra("bankName", bankName);
                    toInventory.putExtra("storeName", storeName);
                    startActivity(toInventory);
                    finish();

                    Toast.makeText(this, "Barang berhasil diperbarui", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Barang gagal diperbarui", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void addNewItem(String itemName, String sellPrice, String buyPrice, String total, String storeId) {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Tunggu sebentar ya");
        pd.setCancelable(false);
        pd.show();

        String userId = firebaseUser.getUid();
        UserModel userModel = new UserModel();

        // for save data items
        if (productId.equals("null") && historyId.equals("null")) {
            productId = refProducts.child(userId).push().getKey();
            historyId = refHistories.child(userId).push().getKey();
            userModel.setProductId(productId);
            userModel.setHistoryId(historyId);
        }

        String itemId = refProducts.child(productId).push().getKey();
        String dateCal = dateString + "-" + monthString + "-" + yearString;
        String monthCal = monthString + "-" + yearString;

        HashMap<String, Object> hashProducts = new HashMap<>();
        hashProducts.put("time", String.valueOf(getCurrentLocalTimeStamp()));
        hashProducts.put("date", dateCal);
        hashProducts.put("month", monthCal);
        hashProducts.put("year", yearString);
        hashProducts.put("itemName", itemName);
        hashProducts.put("searchItem", itemName.toLowerCase());
        hashProducts.put("imageItem", "null");
        hashProducts.put("sellPrice", sellPrice);
        hashProducts.put("buyPrice", buyPrice);
        hashProducts.put("total", total);
        hashProducts.put("itemId", itemId);
        hashProducts.put("productId", productId);
        hashProducts.put("historyId", historyId);

        assert itemId != null;
        refProducts.child(productId).child(itemId).setValue(hashProducts).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // CALCULATE OUTCOME
                int outcome = Integer.parseInt(buyPrice) * Integer.parseInt(total);

                // UPLOAD product in histories
                HashMap<String, Object> hashHistories = new HashMap<>();
                hashHistories.put("time", String.valueOf(getCurrentLocalTimeStamp()));
                hashHistories.put("date", dateCal);
                hashHistories.put("month", monthCal);
                hashHistories.put("year", yearString);
                hashHistories.put("historyId", historyId);
                hashHistories.put("itemId", itemId);
                hashHistories.put("buyTotal", total);
                hashHistories.put("buyPrice", buyPrice);
                hashHistories.put("discountPercent", "0");
                hashHistories.put("discountPrice", "0");
                hashHistories.put("income", "0");
                hashHistories.put("outcome", String.valueOf(outcome));

                refHistories.child(historyId).child(itemId).setValue(hashHistories).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        // UPDATE productId & historyId in Users
                        HashMap<String, Object> hashUpdateUsers = new HashMap<>();
                        hashUpdateUsers.put("productId", productId);
                        hashUpdateUsers.put("historyId", historyId);
                        refUsers.child(userId).updateChildren(hashUpdateUsers);
                    }
                });

                // for save image items
                final StorageReference fileReference = storageReference.child("img-"
                        + itemName.toLowerCase().concat("-")
                        + System.currentTimeMillis() + ".jpg");

                uploadTask = fileReference.putFile(mImageUri);
                uploadTask.continueWithTask(task1 -> {
                    if (!task1.isSuccessful()) {
                        throw Objects.requireNonNull(task1.getException());
                    }
                    return fileReference.getDownloadUrl();
                }).addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful()) {
                        Uri downloadUri = task2.getResult();
                        assert downloadUri != null;
                        String mUri = downloadUri.toString();

                        // UPDATE imageItem in Products
                        HashMap<String, Object> hashUpdateProducts = new HashMap<>();
                        hashUpdateProducts.put("time", String.valueOf(getCurrentLocalTimeStamp()));
                        hashUpdateProducts.put("imageItem", mUri);
                        refProducts.child(productId).child(itemId).updateChildren(hashUpdateProducts);

                        Toast.makeText(this, "Tambah barang sukses", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Tambah barang gagal", Toast.LENGTH_SHORT).show();
                    }
                    pd.dismiss();

                    Intent toInventory = new Intent(this, InventoryActivity.class);
                    toInventory.putExtra("productId", productId);
                    toInventory.putExtra("storeId", storeId);
                    toInventory.putExtra("noHp", noHp);
                    toInventory.putExtra("noRek", noRek);
                    toInventory.putExtra("bankName", bankName);
                    toInventory.putExtra("storeName", storeName);
                    startActivity(toInventory);
                    finish();
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            pd.dismiss();
        });
    }
    private void searchProductBeforeSave(String itemName, String sellPrice, String buyPrice, String total, String storeId) {
        String searchKey = itemName.toLowerCase();

        if (productId != null) {
            Query query = refProducts.child(productId).orderByChild("searchItem")
                    .startAt(searchKey).endAt(searchKey + "\uf8ff");
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!TextUtils.isEmpty(searchKey)) {
                        if (!snapshot.exists()) {
                            addNewItem(itemName, sellPrice, buyPrice, total, storeId);
                        } else {
                            Toast.makeText(AddItemActivity.this, "Produk dengan nama " + itemName + " sudah ada di inventory", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(AddItemActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            assert result != null;
            mImageUri = result.getUri();
            binding.imgUploadItem.setImageURI(mImageUri);
        } else {
            Toast.makeText(this, "Batal mengunggah foto", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }
    }
}