package com.codeart.onlinecashier.ui.stock;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.ArrayList;
import java.util.Locale;



public class AdapterAddStock extends RecyclerView.Adapter<AdapterAddStock.AddStockHolder> {
    private final Activity mActivity;
    private final ArrayList<ProductModel> productModelArrayList;
    private String noHp, noRek, bankName, storeName;

    private FirebaseUser firebaseUser;
    private DatabaseReference refUsers;

    public AdapterAddStock(Activity mActivity, ArrayList<ProductModel> productModelArrayList) {
        this.mActivity = mActivity;
        this.productModelArrayList = productModelArrayList;
    }

    public static class AddStockHolder extends RecyclerView.ViewHolder {
        private final ImageView imgItem;
        private final TextView txtItemName, txtStock, txtSell, txtBuy;
        private final ImageView btnEdit;

        public AddStockHolder(@NonNull View itemView) {
            super(itemView);
            imgItem = itemView.findViewById(R.id.image_item_stock);
            txtItemName = itemView.findViewById(R.id.txt_name_item_stock);
            txtStock = itemView.findViewById(R.id.txt_stock_item_stock);
            txtSell = itemView.findViewById(R.id.txt_sell_price_stock);
            txtBuy = itemView.findViewById(R.id.txt_buy_price_stock);
            btnEdit = itemView.findViewById(R.id.btn_img_edit_stock);
        }
    }

    @NonNull
    @Override
    public AddStockHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.item_add_stock_list, parent, false);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        refUsers = FirebaseDatabase.getInstance().getReference("Users");

        getDataUser();

        return new AddStockHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddStockHolder holder, int position) {
        ProductModel productModel = productModelArrayList.get(position);

        Picasso.get().load(productModel.getImageItem()).into(holder.imgItem);

        holder.txtItemName.setText(productModel.getItemName());
        holder.txtStock.setText(productModel.getTotal());
        holder.txtBuy.setText(convertRp(Integer.parseInt(productModel.getBuyPrice())));
        holder.txtSell.setText(convertRp(Integer.parseInt(productModel.getSellPrice())));

        holder.btnEdit.setOnClickListener(view -> {
            Intent toAddItem = new Intent(mActivity, AddItemActivity.class);
            toAddItem.putExtra("type", "stock");
            toAddItem.putExtra("imageItem", productModel.getImageItem());
            toAddItem.putExtra("itemName", productModel.getItemName());
            toAddItem.putExtra("buyPrice", productModel.getBuyPrice());
            toAddItem.putExtra("total", productModel.getTotal());
            toAddItem.putExtra("itemId", productModel.getItemId());
            toAddItem.putExtra("productId", productModel.getProductId());
            toAddItem.putExtra("historyId", productModel.getHistoryId());
            toAddItem.putExtra("noHp", noHp);
            toAddItem.putExtra("noRek", noRek);
            toAddItem.putExtra("bankName", bankName);
            toAddItem.putExtra("storeName", storeName);
            mActivity.startActivity(toAddItem);
        });
    }

    @Override
    public int getItemCount() {
        return productModelArrayList.size();
    }

    private String convertRp(int rupiah) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRp = NumberFormat.getCurrencyInstance(localeID);

        return formatRp.format((double) rupiah);
    }

    private void getDataUser() {
        refUsers.child(firebaseUser.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserModel userModel = snapshot.getValue(UserModel.class);
                if (snapshot.exists()) {
                    assert userModel != null;
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
}
