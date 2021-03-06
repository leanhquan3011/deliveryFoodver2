package com.leanhquan.deliveryfoodver2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.leanhquan.deliveryfoodver2.Common.Common;
import com.leanhquan.deliveryfoodver2.Database.Database;
import com.leanhquan.deliveryfoodver2.Model.Order;
import com.leanhquan.deliveryfoodver2.Model.Request;
import com.leanhquan.deliveryfoodver2.ViewHolder.CartAdapter;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity {

    private RecyclerView                   recyclerViewCart;
    private RecyclerView.LayoutManager     layoutManagerCart;
    private FirebaseDatabase               database;
    private DatabaseReference              requests;
    private TextView                       txtTotal;
    private Button                         btnOder;
    private CartAdapter                    adapter;

    private List<Order> cart = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        init();


        database =  FirebaseDatabase.getInstance();
        requests = database.getReference("requests");

        recyclerViewCart = findViewById(R.id.recycler_cart);
        layoutManagerCart = new LinearLayoutManager(this);
        recyclerViewCart.setLayoutManager(layoutManagerCart);

        if (cart != null){loadListFoodinCart();}

        btnOder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cart.size() > 0)
                    showAlertDialog();
                else
                    Toast.makeText(CartActivity.this, "your cart is emty", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void loadListFoodinCart() {
        cart = new Database(this).getCarts();
        adapter = new CartAdapter(cart, this);
        adapter.notifyDataSetChanged();
        recyclerViewCart.setAdapter(adapter);

        int total = 0;

        for (Order order:cart) {
            total += (Integer.parseInt(order.getProductPrice())) * (Integer.parseInt(order.getProductCount()));
        }
        Locale locale = new Locale("en", "US");
        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

        txtTotal.setText(fmt.format(total));
    }

    @Override
    protected void onStart() {
        super.onStart();
        new Database(getBaseContext()).clearCart();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent i = new Intent(this, HomeScreenActivity.class);
        startActivity(i);
    }

    private void showAlertDialog() {
        AlertDialog.Builder alertdialog = new AlertDialog.Builder(this);
        alertdialog.setTitle("One more step");
        alertdialog.setMessage("Enter your address: ");
        final EditText edtAddress = new EditText(this);
        LinearLayout.LayoutParams lp =  new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        edtAddress.setLayoutParams(lp);
        alertdialog.setView(edtAddress);
        alertdialog.setIcon(R.drawable.ic_cart_black);
        alertdialog.setPositiveButton("yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Request request = new Request(
                        Common.currentUser.getPhone(),
                        Common.currentUser.getName(),
                        edtAddress.getText().toString(),
                        txtTotal.getText().toString(),
                        "0",
                        cart
                );
                requests.child(String.valueOf(System.currentTimeMillis()))
                        .setValue(request);

                Log.d("TAG", "onClick: "+ Common.currentUser.getPhone());

                new Database(getBaseContext()).clearCart();

                Toast.makeText(CartActivity.this, "Thank you!!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(CartActivity.this, HomeScreenActivity.class);
                startActivity(intent);
            }
        });

        alertdialog.setNegativeButton("no", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertdialog.show();
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getTitle().equals(Common.DELETE)){
            deleteCart(item.getOrder());
        }
        return true;
    }

    private void deleteCart(int position) {
        cart.remove(position);
        new Database(this).clearCart();
        for (Order item:cart){
            new Database(this).addTocart(item);
        }

        loadListFoodinCart();
    }

    private void init() {
        txtTotal = findViewById(R.id.total);
        btnOder = findViewById(R.id.btnPlaceOrder);
    }
}
