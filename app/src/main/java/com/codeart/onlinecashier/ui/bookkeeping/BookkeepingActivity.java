package com.codeart.onlinecashier.ui.bookkeeping;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.codeart.onlinecashier.R;
import com.codeart.onlinecashier.databinding.ActivityBookkeepingBinding;
import com.codeart.onlinecashier.models.HistoryModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BookkeepingActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityBookkeepingBinding binding;

    private DatabaseReference refHistories;

    private AdapterBookkeeping adapterBookkeeping;
    private ArrayList<HistoryModel> historyModelArrayList;

    private String historyId, storeId, productId;
    private String dateString, monthString, yearString;

    private int incomeTotal, outcomeTotal;

    // SPINNER FILTER HISTORY
    private final String[] arrFilterHistory = {
            "HARI INI",
            "1 BULAN",
            "1 TAHUN"
    };
    private String filterHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.bg_top_color);
        binding = ActivityBookkeepingBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        binding.cvHistories.setBackgroundResource(R.color.primary_dark);

        refHistories = FirebaseDatabase.getInstance().getReference("Histories");

        binding.rvAllHistories.setHasFixedSize(true);
        binding.rvAllHistories.setLayoutManager(new LinearLayoutManager(this));

        Intent data = getIntent();
        if (data != null) {
            storeId = data.getStringExtra("storeId");
            productId = data.getStringExtra("productId");
            historyId = data.getStringExtra("historyId");
        }

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

        getHistoryToday();

        // SPINNER FILTER
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, arrFilterHistory
        );
        binding.spinnerFilterHistory.setAdapter(adapter);

        binding.spinnerFilterHistory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                filterHistory = adapter.getItem(i);
                if (filterHistory.equals("HARI INI")) {
                    getHistoryToday();
                } else if (filterHistory.equals("1 BULAN")) {
                    getHistoryMonth();
                } else {
                    getHistoryYear();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        binding.imgBackBookkeeping.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.img_back_bookkeeping) {
            super.onBackPressed();
        }
    }

    private void getHistoryToday() {
        historyModelArrayList = new ArrayList<>();
        String dateCal = dateString + "-" + monthString + "-" + yearString;

        if (historyId != null) {
            Query query = refHistories.child(historyId)
                    .orderByChild("date").startAt(dateCal).endAt(dateCal + "\uf8ff");

            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // RESET
                    historyModelArrayList.clear();
                    incomeTotal = 0;
                    outcomeTotal = 0;
                    if (snapshot.exists()) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            HistoryModel historyModel = dataSnapshot.getValue(HistoryModel.class);
                            if (refHistories != null) {
                                historyModelArrayList.add(historyModel);

                                assert historyModel != null;
                                setIncomeTotal(historyModel.getIncome());
                                setOutcomeTotal(historyModel.getOutcome());
                            }
                        }
                        // SET INCOME & OUTCOME TOTAL
                        binding.txtIncomeTotal.setText(convertRp(incomeTotal));
                        binding.txtOutcomeTotal.setText(convertRp(outcomeTotal));

                        adapterBookkeeping = new AdapterBookkeeping(BookkeepingActivity.this, historyModelArrayList);
                        binding.rvAllHistories.setAdapter(adapterBookkeeping);
                        adapterBookkeeping.notifyDataSetChanged();

                        binding.txtItemIsEmpty.setVisibility(View.GONE);
                    } else {
                        binding.txtItemIsEmpty.setVisibility(View.VISIBLE);
                    }
                    binding.progressBarBookkeeping.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(BookkeepingActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "historyId NULL", Toast.LENGTH_SHORT).show();
        }
    }

    private void getHistoryMonth() {
        historyModelArrayList = new ArrayList<>();
        String dateCal = monthString + "-" + yearString;

        if (historyId != null) {
            Query query = refHistories.child(historyId)
                    .orderByChild("month").startAt(dateCal).endAt(dateCal + "\uf8ff");

            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // RESET
                    historyModelArrayList.clear();
                    incomeTotal = 0;
                    outcomeTotal = 0;
                    if (snapshot.exists()) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            HistoryModel historyModel = dataSnapshot.getValue(HistoryModel.class);
                            if (refHistories != null) {
                                historyModelArrayList.add(historyModel);

                                assert historyModel != null;
                                setIncomeTotal(historyModel.getIncome());
                                setOutcomeTotal(historyModel.getOutcome());
                            }
                        }
                        // SET INCOME & OUTCOME TOTAL
                        binding.txtIncomeTotal.setText(convertRp(incomeTotal));
                        binding.txtOutcomeTotal.setText(convertRp(outcomeTotal));

                        adapterBookkeeping = new AdapterBookkeeping(BookkeepingActivity.this, historyModelArrayList);
                        binding.rvAllHistories.setAdapter(adapterBookkeeping);
                        adapterBookkeeping.notifyDataSetChanged();

                        binding.txtItemIsEmpty.setVisibility(View.GONE);
                    } else {
                        binding.txtItemIsEmpty.setVisibility(View.VISIBLE);
                    }
                    binding.progressBarBookkeeping.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(BookkeepingActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "historyId NULL", Toast.LENGTH_SHORT).show();
        }
    }

    private void getHistoryYear() {
        historyModelArrayList = new ArrayList<>();
        String dateCal = yearString;

        if (historyId != null) {
            Query query = refHistories.child(historyId)
                    .orderByChild("year").startAt(dateCal).endAt(dateCal + "\uf8ff");

            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // RESET
                    historyModelArrayList.clear();
                    incomeTotal = 0;
                    outcomeTotal = 0;
                    if (snapshot.exists()) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            HistoryModel historyModel = dataSnapshot.getValue(HistoryModel.class);
                            if (refHistories != null) {
                                historyModelArrayList.add(historyModel);

                                assert historyModel != null;
                                setIncomeTotal(historyModel.getIncome());
                                setOutcomeTotal(historyModel.getOutcome());
                            }
                        }
                        // SET INCOME & OUTCOME TOTAL
                        binding.txtIncomeTotal.setText(convertRp(incomeTotal));
                        binding.txtOutcomeTotal.setText(convertRp(outcomeTotal));

                        adapterBookkeeping = new AdapterBookkeeping(BookkeepingActivity.this, historyModelArrayList);
                        binding.rvAllHistories.setAdapter(adapterBookkeeping);
                        adapterBookkeeping.notifyDataSetChanged();

                        binding.txtItemIsEmpty.setVisibility(View.GONE);
                    } else {
                        binding.txtItemIsEmpty.setVisibility(View.VISIBLE);
                    }
                    binding.progressBarBookkeeping.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(BookkeepingActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "historyId NULL", Toast.LENGTH_SHORT).show();
        }
    }

    private void setIncomeTotal(String income) {
        int incomeInt = Integer.parseInt(income);
        incomeTotal += incomeInt;
    }

    private void setOutcomeTotal(String outcome) {
        int outcomeInt = Integer.parseInt(outcome);
        outcomeTotal += outcomeInt;
    }

    private String convertRp(int rupiah) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRp = NumberFormat.getCurrencyInstance(localeID);

        return formatRp.format((double) rupiah);
    }
}