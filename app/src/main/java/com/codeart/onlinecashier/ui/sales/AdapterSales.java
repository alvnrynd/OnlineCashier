package com.codeart.onlinecashier.ui.sales;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.codeart.onlinecashier.R;
import com.codeart.onlinecashier.models.ProductModel;
import com.codeart.onlinecashier.models.SalesModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;



public class AdapterSales extends RecyclerView.Adapter<AdapterSales.SalesHolder> {
    private final Activity mActivity;
    private final ArrayList<SalesModel> salesModelArrayList;
    private DatabaseReference refProducts, refSales;
    private String tempTotal;

    public AdapterSales(Activity mActivity, ArrayList<SalesModel> salesModelArrayList) {
        this.mActivity = mActivity;
        this.salesModelArrayList = salesModelArrayList;
    }

    public static class SalesHolder extends RecyclerView.ViewHolder {
        private final TextView noIndex, itemName, total, totalPrice;

        public SalesHolder(@NonNull View itemView) {
            super(itemView);
            noIndex = itemView.findViewById(R.id.txt_index_sales);
            itemName = itemView.findViewById(R.id.txt_item_name_sales);
            total = itemView.findViewById(R.id.txt_total_sales);
            totalPrice = itemView.findViewById(R.id.txt_price_sales);
        }
    }

    @NonNull
    @Override
    public SalesHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.item_sales_list, parent, false);
        refProducts = FirebaseDatabase.getInstance().getReference("Products");
        refSales = FirebaseDatabase.getInstance().getReference("Sales");

        return new SalesHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SalesHolder holder, int position) {
        SalesModel salesModel = salesModelArrayList.get(position);

        int index = position + 1;
        holder.noIndex.setText(String.format("%s.", index));
        holder.itemName.setText(salesModel.getItemName());
        holder.total.setText(salesModel.getTotal());
        holder.totalPrice.setText(convertRp(Integer.parseInt(salesModel.getTotalPrice())));

        holder.itemView.setOnLongClickListener(view -> {
            String salesId = salesModel.getSalesId();
            String itemSalesId = salesModel.getItemSalesId();
            String invoiceNumber = salesModel.getInvoiceNumber();
            String productId = salesModel.getProductId();
            String itemProductId = salesModel.getItemProductId();
            String totalBuy = salesModel.getTotal();

            // set tempTotal
            getTotalItemProducts(productId, itemProductId);

            showDialogAlertDelete(salesId, itemSalesId, invoiceNumber, productId, itemProductId, totalBuy);

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return salesModelArrayList.size();
    }

    private void showDialogAlertDelete(String salesId, String itemSalesId, String invoiceNumber,
                                       String productId, String itemProductId, String totalBuy) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
        alertDialogBuilder.setTitle("HAPUS BARANG");
        alertDialogBuilder
                .setMessage("Apakah Anda yakin ingin menghapus barang ini?")
                .setCancelable(false)
                .setPositiveButton("IYA", (dialog, id) -> {

                    // hitung total produk lama + total produk yang dibeli
                    int totalBack = Integer.parseInt(tempTotal) + Integer.parseInt(totalBuy);

                    // Kembalikan stok yang ada di products
                    HashMap<String, Object> hashUpdateProducts = new HashMap<>();
                    hashUpdateProducts.put("total", String.valueOf(totalBack));

                    refProducts.child(productId).child(itemProductId)
                            .updateChildren(hashUpdateProducts).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // hapus sales item
                            refSales.child(salesId).child(invoiceNumber).child(itemSalesId).removeValue();

                            mActivity.overridePendingTransition(0, 0);
                            mActivity.startActivity(mActivity.getIntent());
                            mActivity.finish();
                            mActivity.overridePendingTransition(0, 0);
                            Toast.makeText(mActivity, "Barang berhasil dihapus", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("TIDAK", (dialog, id) -> dialog.cancel());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void getTotalItemProducts(String productId, String itemProductId) {
        refProducts.child(productId).child(itemProductId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ProductModel productModel = snapshot.getValue(ProductModel.class);
                if (snapshot.exists()) {
                    assert productModel != null;
                    tempTotal = productModel.getTotal();
                } else {
                    Toast.makeText(mActivity, "total produk tidak ditemukan", Toast.LENGTH_SHORT).show();
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
}
