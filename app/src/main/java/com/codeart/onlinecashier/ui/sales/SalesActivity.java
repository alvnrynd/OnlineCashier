package com.codeart.onlinecashier.ui.sales;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.codeart.onlinecashier.R;
import com.codeart.onlinecashier.databinding.ActivitySalesBinding;
import com.codeart.onlinecashier.models.SalesModel;
import com.codeart.onlinecashier.ui.HomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class SalesActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivitySalesBinding binding;

    private Calendar calendar;
    private String dateString, monthString, yearString;
    private DatabaseReference refSales, refUsers, refHistories;
    private FirebaseUser firebaseUser;
    private String salesId, invoiceNumber;
    private String storeName, noHp, noRek, bankName, historyId;

    private AdapterSales adapterSales;
    private ArrayList<SalesModel> salesModelArrayList;

    private int totalPriceAll;
    int totalPriceAfterDiscount = 0;
    int pageHeight = 930;

    // for Invoice
    Bitmap bitmap, scaleBitmap;
    private ArrayList<String> arrItemName, arrItemPrice, arrTotalBuy, arrTotalPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.bg_top_bottom_color);
        binding = ActivitySalesBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        refSales = FirebaseDatabase.getInstance().getReference("Sales");
        refUsers = FirebaseDatabase.getInstance().getReference("Users");
        refHistories = FirebaseDatabase.getInstance().getReference("Histories");
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        binding.rvAllSales.setHasFixedSize(true);
        binding.rvAllSales.setLayoutManager(new LinearLayoutManager(this));

        calendar = Calendar.getInstance();

        Date date = new Date();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            int year = localDate.getYear();
            int month = localDate.getMonthValue();
            int day = localDate.getDayOfMonth();


            dateString = String.valueOf(day);
            monthString = String.valueOf(month);
            yearString = String.valueOf(year);
        }

        String currentDate = dateString + "-" + monthString + "-" + yearString;

        Intent data = getIntent();
        if (data != null) {
            invoiceNumber = data.getStringExtra("invoiceNumber");
            salesId = data.getStringExtra("salesId");
            historyId = data.getStringExtra("historyId");
            storeName = data.getStringExtra("storeName");
            noHp = data.getStringExtra("noHp");
            noRek = data.getStringExtra("noRek");
            bankName = data.getStringExtra("bankName");

            if (!invoiceNumber.equals("null")) {
                binding.numberSales.setText(invoiceNumber);
                binding.dateSales.setText(currentDate);
            }
        }

        getAllSales();

        binding.cvTitleTable.setBackgroundResource(R.color.primary_dark);


        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.invoice_header);
        scaleBitmap = Bitmap.createScaledBitmap(bitmap, 1200, 518, false);


        arrItemName = new ArrayList<>();
        arrItemPrice = new ArrayList<>();
        arrTotalBuy = new ArrayList<>();
        arrTotalPrice = new ArrayList<>();

        binding.imgBackSales.setOnClickListener(this);
        binding.imgCartDiscount.setOnClickListener(this);
        binding.btnBayar.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.img_back_sales) {
            super.onBackPressed();
        } else if (view.getId() == R.id.img_cart_discount) {
            Dialog dialog = new Dialog(this);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setContentView(R.layout.dialog_add_discount);
            dialog.setCancelable(false);
            dialog.show();

            EditText etAddDiscount = dialog.findViewById(R.id.et_add_discount);
            TextView txtCancel = dialog.findViewById(R.id.txt_cancel_discount);
            TextView txtSave = dialog.findViewById(R.id.txt_save_discount);

            txtCancel.setOnClickListener(view1 -> dialog.dismiss());

            txtSave.setOnClickListener(view1 -> {
                String discount = etAddDiscount.getText().toString();
                int discountInt = Integer.parseInt(discount);
                if (discountInt > 100){
                    Toast.makeText(this, "diskon tidak boleh lebih dari 100%", Toast.LENGTH_SHORT).show();}
                else {
                    binding.txtDiscountPercent.setText(String.format("%s%%", discount));
                    binding.txtDiscountPriceSales.setText(String.format("- %s", convertRp(calculateDiscountPrice(discountInt))));
                    binding.txtTotalPriceSales.setText(convertRp(setTotalPriceAfterDiscount(discountInt)));
                    dialog.dismiss();
                }
            });
        } else if (view.getId() == R.id.btn_bayar) {
            String discountPercent = binding.txtDiscountPercent.getText().toString();
            String discountPrice = binding.txtDiscountPriceSales.getText().toString();
            String income = binding.txtTotalPriceSales.getText().toString();


            if (!discountPercent.equals(". . .")) {
                Dialog dialog = new Dialog(this);
                Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.setContentView(R.layout.dialog_print_pdf);
                dialog.setCancelable(true);
                dialog.show();

                TextView txtExportPDF = dialog.findViewById(R.id.txt_export_pdf);


                int subTotalPrice = Integer.parseInt(replaceRp(discountPrice)) + Integer.parseInt(replaceRp(income));
                String subTotal = convertRp(subTotalPrice);

                txtExportPDF.setOnClickListener(view1 -> {
                    createPdf(discountPercent, discountPrice, subTotal, income);
                    dialog.dismiss();
                    payInvoice(historyId, invoiceNumber, discountPercent, replaceRp(discountPrice), replaceRp(income));
                });

            } else {
                Toast.makeText(this, "Tolong isi diskon dahulu", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int calculateDiscountPrice(int discount) {
        return (discount * totalPriceAll) / 100;
    }

    private int setTotalPriceAfterDiscount(int discountPercent) {
        return totalPriceAfterDiscount = totalPriceAll - calculateDiscountPrice(discountPercent);
    }

    private String replaceRp(String rupiah) {
        return rupiah.replace("- ", "")
                .replace("Rp", "")
                .replace(".", "")
                .replace(",00", "");
    }

    private void createPdf(String dcPercent, String dcPrice, String subTotal, String totalPayInvoice) {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        int pageWidth = 1200, centerWidth = 600;

        PdfDocument.PageInfo pageInfo
                = new PdfDocument.PageInfo.Builder(1200, 2010, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        canvas.drawBitmap(scaleBitmap, 0, 0, paint);


        paint.setColor(Color.BLACK);
        paint.setTextSize(30f);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Berbagai macam barang ada semua", 10, 40, paint);
        canvas.drawText("Pesan di no. " + noHp, 10, 80, paint);


        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(80);
        canvas.drawText("NOTA", 900, 330, titlePaint);
        titlePaint.setTextSize(50);
        canvas.drawText("PEMBELIAN", 900, 375, titlePaint);

        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("No. Pesanan", 20, 590, paint);
        canvas.drawText("Tanggal", 20, 640, paint);
        canvas.drawText("Pukul", 20, 690, paint);
        canvas.drawText(": " + invoiceNumber, 200, 590, paint);
        canvas.drawText(": " + dateString + "/" + monthString + "/" + yearString, 200, 640, paint);
        canvas.drawText(": " + getCurrentLocalTimeStamp(), 200, 690, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawRect(20, 780, pageWidth - 20, 860, paint);


        paint.setTextAlign(Paint.Align.LEFT);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText("No.", 40, 830, paint);
        canvas.drawText("Menu Pesanan", 130, 830, paint);
        canvas.drawText("Harga", 560, 830, paint);
        canvas.drawText("Jumlah", 800, 830, paint);
        canvas.drawText("Total", 950, 830, paint);
        canvas.drawLine(110, 790, 110, 840, paint);
        canvas.drawLine(540, 790, 540, 840, paint);
        canvas.drawLine(780, 790, 780, 840, paint);
        canvas.drawLine(930, 790, 930, 840, paint);


        for (int i = 0; i < arrItemName.size(); i++) {
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(i + 1 + ".", 40, pageHeight, paint);
            canvas.drawText(arrItemName.get(i), 130, pageHeight, paint);
            canvas.drawText(convertRp(Integer.parseInt(arrItemPrice.get(i))), 560, pageHeight, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(arrTotalBuy.get(i), 845, pageHeight, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(convertRp(Integer.parseInt(arrTotalPrice.get(i))), pageWidth - 40, pageHeight, paint);
            pageHeight += 80;
        }

        Log.d("height", String.valueOf(pageHeight));
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawLine(520, pageHeight, pageWidth - 20, pageHeight, paint);
        canvas.drawText("Sub Total", 700, pageHeight + 50, paint);
        canvas.drawText(":", 900, pageHeight + 50, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(subTotal, pageWidth - 40, pageHeight + 50, paint);


        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Diskon " + dcPercent, 700, pageHeight + 100, paint);
        canvas.drawText(":", 900, pageHeight + 100, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(dcPrice, pageWidth - 40, pageHeight + 100, paint);
        paint.setTextAlign(Paint.Align.LEFT);


        paint.setColor(Color.rgb(10, 188, 255));
        canvas.drawRect(520, pageHeight + 165, pageWidth - 20, pageHeight + 280, paint);


        paint.setTextSize(50f);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.BLACK);
        canvas.drawText("Total", 540, pageHeight + 238, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(totalPayInvoice, pageWidth - 40, pageHeight + 238, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(30);
        canvas.drawText("(*) Anda bisa Transfer di rek. " + noRek + " (" + bankName + ")", 10, pageHeight + 340, paint);

        paint.setTextSize(40);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Terimakasih Telah Belanja", centerWidth, 1940, paint);
        canvas.drawText("di Toko " + storeName, centerWidth, 1990, paint);

        pdfDocument.finishPage(page);

        File file = new File(Environment.getExternalStorageDirectory() + "/documents", "/pesanan-" + invoiceNumber + ".pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
        } catch (IOException e) {
            Log.d("pdf", e.getMessage());
            e.printStackTrace();
        }

        pdfDocument.close();
        Toast.makeText(SalesActivity.this, "PDF berhasil dibuat", Toast.LENGTH_LONG).show();
    }

    private void payInvoice(String historyId, String invoiceNumber, String dcPercent, String dcPrice, String income) {
        String dateCal = dateString + "-" + monthString + "-" + yearString;
        String monthCal = monthString + "-" + yearString;

        HashMap<String, Object> hashHistories = new HashMap<>();
        hashHistories.put("time", String.valueOf(getCurrentLocalTimeStamp()));
        hashHistories.put("date", dateCal);
        hashHistories.put("month", monthCal);
        hashHistories.put("year", yearString);
        hashHistories.put("historyId", historyId);
        hashHistories.put("itemId", invoiceNumber);
        hashHistories.put("buyTotal", "0");
        hashHistories.put("buyPrice", "0");
        hashHistories.put("discountPercent", dcPercent);
        hashHistories.put("discountPrice", dcPrice);
        hashHistories.put("income", income);
        hashHistories.put("outcome", "0");

        refHistories.child(historyId).child(invoiceNumber).setValue(hashHistories).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                HashMap<String, Object> hashUpdateUsers = new HashMap<>();
                hashUpdateUsers.put("invoiceNumber", "null");
                refUsers.child(firebaseUser.getUid()).updateChildren(hashUpdateUsers);

                startActivity(new Intent(SalesActivity.this, HomeActivity.class));
                finish();
                Toast.makeText(this, "Pembayaran berhasil", Toast.LENGTH_SHORT).show();
                pageHeight = 930;
            }
        });
    }

    private void getAllSales() {
        salesModelArrayList = new ArrayList<>();
        if (salesId != null) {
            refSales.child(salesId).child(invoiceNumber).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    salesModelArrayList.clear();
                    totalPriceAll = 0;
                    if (snapshot.exists()) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            SalesModel salesModel = dataSnapshot.getValue(SalesModel.class);
                            if (refSales != null) {
                                salesModelArrayList.add(salesModel);

                                assert salesModel != null;
                                setTotalPriceAllItem(salesModel.getTotalPrice());


                                arrItemName.add(salesModel.getItemName());
                                arrItemPrice.add(salesModel.getItemPrice());
                                arrTotalBuy.add(salesModel.getTotal());
                                arrTotalPrice.add(salesModel.getTotalPrice());
                            }
                        }

                        binding.txtTotalPriceSales.setText(convertRp(totalPriceAll));

                        adapterSales = new AdapterSales(SalesActivity.this, salesModelArrayList);
                        binding.rvAllSales.setAdapter(adapterSales);
                        adapterSales.notifyDataSetChanged();

                        binding.txtItemIsEmpty.setVisibility(View.GONE);
                    } else {
                        binding.txtItemIsEmpty.setVisibility(View.VISIBLE);
                    }
                    binding.progressBarAddStock.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(SalesActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "salesId NULL", Toast.LENGTH_SHORT).show();
        }
    }

    public String getCurrentLocalTimeStamp() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        return currentTime.format(calendar.getTime());
    }

    private void setTotalPriceAllItem(String totalPriceItem) {
        int totalPrice = Integer.parseInt(totalPriceItem);
        totalPriceAll += totalPrice;
    }

    private String convertRp(int rupiah) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRp = NumberFormat.getCurrencyInstance(localeID);

        return formatRp.format((double) rupiah);
    }
}