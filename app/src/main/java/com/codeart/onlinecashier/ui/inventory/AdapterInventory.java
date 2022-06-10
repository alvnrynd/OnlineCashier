package com.codeart.onlinecashier.ui.inventory;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.codeart.onlinecashier.R;
import com.codeart.onlinecashier.models.ProductModel;
import com.codeart.onlinecashier.models.UserModel;
import com.codeart.onlinecashier.ui.AddItemActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;



public class AdapterInventory extends RecyclerView.Adapter<AdapterInventory.InventoryHolder> {
    private final Activity mActivity;
    private final ArrayList<ProductModel> productModelArrayList;

    private String salesId, invoiceNumber, noHp, noRek, bankName, storeName;
    private String dateString, monthString, yearString;
    private FirebaseUser firebaseUser;
    private DatabaseReference refSales, refProducts, refUsers;

    private Calendar calendar;

    public AdapterInventory(Activity mActivity, ArrayList<ProductModel> productModelArrayList) {
        this.mActivity = mActivity;
        this.productModelArrayList = productModelArrayList;
    }

    public static class InventoryHolder extends RecyclerView.ViewHolder {
        private final ImageView imgItem;
        private final TextView txtItemName, txtStock, txtSell, txtBuy;
        private final ImageView btnEdit, btnDelete, btnAdd;

        public InventoryHolder(@NonNull View itemView) {
            super(itemView);
            imgItem = itemView.findViewById(R.id.image_item);
            txtItemName = itemView.findViewById(R.id.txt_name_item);
            txtStock = itemView.findViewById(R.id.txt_stock_item);
            txtSell = itemView.findViewById(R.id.txt_sell_price);
            txtBuy = itemView.findViewById(R.id.txt_buy_price);
            btnEdit = itemView.findViewById(R.id.btn_img_edit);
            btnDelete = itemView.findViewById(R.id.btn_img_delete);
            btnAdd = itemView.findViewById(R.id.btn_img_add);
        }
    }

    @NonNull
    @Override
    public InventoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.item_inventory_list, parent, false);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        refSales = FirebaseDatabase.getInstance().getReference("Sales");
        refProducts = FirebaseDatabase.getInstance().getReference("Products");
        refUsers = FirebaseDatabase.getInstance().getReference("Users");

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

        return new InventoryHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InventoryHolder holder, int position) {
        ProductModel productModel = productModelArrayList.get(position);

        Picasso.get().load(productModel.getImageItem()).into(holder.imgItem);

        holder.txtItemName.setText(productModel.getItemName());
        holder.txtStock.setText(productModel.getTotal());
        holder.txtBuy.setText(convertRp(Integer.parseInt(productModel.getBuyPrice())));
        holder.txtSell.setText(convertRp(Integer.parseInt(productModel.getSellPrice())));

        holder.btnEdit.setOnClickListener(view -> {
            Intent toAddItem = new Intent(mActivity, AddItemActivity.class);
            toAddItem.putExtra("type", "edit");
            toAddItem.putExtra("imageItem", productModel.getImageItem());
            toAddItem.putExtra("itemName", productModel.getItemName());
            toAddItem.putExtra("sellPrice", productModel.getSellPrice());
            toAddItem.putExtra("itemId", productModel.getItemId());
            toAddItem.putExtra("productId", productModel.getProductId());
            toAddItem.putExtra("noHp", noHp);
            toAddItem.putExtra("noRek", noRek);
            toAddItem.putExtra("bankName", bankName);
            toAddItem.putExtra("storeName", storeName);
            mActivity.startActivity(toAddItem);
        });

        // data yang di riwayat itemId masih belum kehapus semua
        holder.btnDelete.setOnClickListener(view ->
                showDialogAlertDelete(
                        productModel.getHistoryId(),
                        productModel.getProductId(),
                        productModel.getItemId()
                )
        );

        holder.btnAdd.setOnClickListener(view -> {
            Dialog dialog = new Dialog(mActivity);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setContentView(R.layout.dialog_total_buy);
            dialog.setCancelable(false);
            dialog.show();

            EditText etTotalBuy = dialog.findViewById(R.id.et_total_buy);
            TextView txtCancel = dialog.findViewById(R.id.txt_cancel);
            TextView txtSave = dialog.findViewById(R.id.txt_save);

            getDataUser();

            txtCancel.setOnClickListener(view1 -> dialog.dismiss());

            txtSave.setOnClickListener(view1 -> {
                String totalBuy = etTotalBuy.getText().toString();
                String tempTotal = productModel.getTotal();
                String productId = productModel.getProductId();
                String itemId = productModel.getItemId();
                String itemName = productModel.getItemName();
                String sellPrice = productModel.getSellPrice();


                    if (Integer.parseInt(tempTotal) < Integer.parseInt(totalBuy)) {
                        Toast.makeText(mActivity, "stok barang hanya ada " + tempTotal, Toast.LENGTH_SHORT).show();
                    } else {
                        sellItems(productId, itemId, itemName, tempTotal, totalBuy, sellPrice);
                    }
                    dialog.dismiss();

            });
        });
    }

    @Override
    public int getItemCount() {
        return productModelArrayList.size();
    }

    private void sellItems(String productId, String itemId, String itemName, String tempTotal, String total, String sellPrice) {
        final ProgressDialog pd = new ProgressDialog(mActivity);
        pd.setMessage("Tunggu sebentar ya");
        pd.setCancelable(false);
        pd.show();

        assert firebaseUser != null;
        String userId = firebaseUser.getUid();
        UserModel userModel = new UserModel();

        if (invoiceNumber.equals("null")) {
            if (salesId.equals("null")) {
                salesId = refSales.child(userId).push().getKey();
                userModel.setSalesId(salesId);
            }
            invoiceNumber = String.format("%s%s%s%s%s", dateString,monthString,yearString,
                    getCurrentLocalTimeStamp().replace(":", ""),
                    System.currentTimeMillis()).substring(0,12);
        }

        String itemSalesId = refProducts.child(productId).push().getKey();

        int totalPrice = Integer.parseInt(sellPrice) * Integer.parseInt(total);
        int totalStock = Integer.parseInt(tempTotal) - Integer.parseInt(total);
        String combineDate = dateString + "-" + monthString + "-" + yearString;

        HashMap<String, Object> hashSales = new HashMap<>();
        hashSales.put("time", String.valueOf(getCurrentLocalTimeStamp()));
        hashSales.put("salesId", salesId);
        hashSales.put("invoiceNumber", invoiceNumber);
        hashSales.put("productId", productId);
        hashSales.put("itemSalesId", itemSalesId);
        hashSales.put("itemProductId", itemId);
        hashSales.put("itemName", itemName);
        hashSales.put("total", total);
        hashSales.put("itemPrice", sellPrice);
        hashSales.put("totalPrice", String.valueOf(totalPrice));
        hashSales.put("date", combineDate);

        assert itemSalesId != null;
        refSales.child(salesId).child(invoiceNumber).child(itemSalesId).setValue(hashSales).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                HashMap<String, Object> hashUpdateProducts = new HashMap<>();
                hashUpdateProducts.put("time", String.valueOf(getCurrentLocalTimeStamp()));
                hashUpdateProducts.put("total", String.valueOf(totalStock));

                refProducts.child(productId).child(itemId).updateChildren(hashUpdateProducts);

                HashMap<String, Object> hashUpdateUsers = new HashMap<>();
                hashUpdateUsers.put("salesId", salesId);
                hashUpdateUsers.put("invoiceNumber", invoiceNumber);

                refUsers.child(userId).updateChildren(hashUpdateUsers);

                pd.dismiss();
                Toast.makeText(mActivity, "Berhasil tambah di keranjang", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public String getCurrentLocalTimeStamp() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        return currentTime.format(calendar.getTime());
    }

    private void getDataUser() {
        refUsers.child(firebaseUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserModel userModel = snapshot.getValue(UserModel.class);
                if (snapshot.exists()) {
                    assert userModel != null;
                    salesId = userModel.getSalesId();
                    invoiceNumber = userModel.getInvoiceNumber();
                    noHp = userModel.getNoHpOwner();
                    noRek = userModel.getNoRekOwner();
                    bankName = userModel.getBankName();
                    storeName = userModel.getStoreName();
                } else {
                    Toast.makeText(mActivity, "Data user tidak ditemukan", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String convertRp(int rupiah) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRp = NumberFormat.getCurrencyInstance(localeID);

        return formatRp.format((double) rupiah);
    }

    private void showDialogAlertDelete(String historyId, String productId, String itemId) {
        DatabaseReference refProducts = FirebaseDatabase.getInstance().getReference("Products");
        DatabaseReference refHistories = FirebaseDatabase.getInstance().getReference("Histories");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
        alertDialogBuilder.setTitle("HAPUS BARANG");
        alertDialogBuilder
                .setMessage("Apakah Anda yakin ingin menghapus barang ini?")
                .setCancelable(false)
                .setPositiveButton("IYA", (dialog, id) -> {
                    refProducts.child(productId).child(itemId).removeValue();
                    refHistories.child(historyId).child(itemId).removeValue();

                    mActivity.overridePendingTransition(0, 0);
                    mActivity.startActivity(mActivity.getIntent());
                    mActivity.finish();
                    mActivity.overridePendingTransition(0, 0);

                    Toast.makeText(mActivity, "Barang berhasil dihapus", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("TIDAK", (dialog, id) -> dialog.cancel());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
