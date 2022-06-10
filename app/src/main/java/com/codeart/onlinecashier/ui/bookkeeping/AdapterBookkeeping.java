package com.codeart.onlinecashier.ui.bookkeeping;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.codeart.onlinecashier.R;
import com.codeart.onlinecashier.models.HistoryModel;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;



public class AdapterBookkeeping extends RecyclerView.Adapter<AdapterBookkeeping.BookHolder> {
    private final Activity mActivity;
    private final ArrayList<HistoryModel> historyModelArrayList;

    public AdapterBookkeeping(Activity mActivity, ArrayList<HistoryModel> historyModelArrayList) {
        this.mActivity = mActivity;
        this.historyModelArrayList = historyModelArrayList;
    }

    public static class BookHolder extends RecyclerView.ViewHolder {
        private final TextView txtIncome, txtOutcome;

        public BookHolder(@NonNull View itemView) {
            super(itemView);
            txtIncome = itemView.findViewById(R.id.txt_income);
            txtOutcome = itemView.findViewById(R.id.txt_outcome);
        }
    }

    @NonNull
    @Override
    public BookHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.item_book_list, parent, false);
        return new BookHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookHolder holder, int position) {
        HistoryModel historyModel = historyModelArrayList.get(position);

        holder.txtIncome.setText(convertRp(Integer.parseInt(historyModel.getIncome())));
        holder.txtOutcome.setText(convertRp(Integer.parseInt(historyModel.getOutcome())));
    }

    @Override
    public int getItemCount() {
        return historyModelArrayList.size();
    }

    private String convertRp(int rupiah) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRp = NumberFormat.getCurrencyInstance(localeID);

        return formatRp.format((double) rupiah);
    }
}
