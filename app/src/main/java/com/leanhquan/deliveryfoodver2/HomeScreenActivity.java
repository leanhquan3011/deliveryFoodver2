package com.leanhquan.deliveryfoodver2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.andremion.counterfab.CounterFab;
import com.bumptech.glide.Glide;
import com.daimajia.slider.library.Animations.DescriptionAnimation;
import com.daimajia.slider.library.SliderLayout;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.daimajia.slider.library.SliderTypes.TextSliderView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.leanhquan.deliveryfoodver2.Common.Common;
import com.leanhquan.deliveryfoodver2.Database.Database;
import com.leanhquan.deliveryfoodver2.Inteface.ItemClickListener;
import com.leanhquan.deliveryfoodver2.Model.Banner;
import com.leanhquan.deliveryfoodver2.Model.Category;
import com.leanhquan.deliveryfoodver2.Service.ListenOrderService;
import com.leanhquan.deliveryfoodver2.ViewHolder.MenuViewHolder;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import io.paperdb.Paper;

public class HomeScreenActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private final static String       TAG = "TAG";
    private DrawerLayout              drawer;
    private NavigationView            navigationView;
    private Toolbar                   toolbar;
    private RecyclerView              recyclerMenu;
    private CounterFab                fapCart;
    private TextView                  txtFullname;

    //Slider
    private HashMap<String, String> image_list;
    private SliderLayout mSlider;


    private FirebaseRecyclerAdapter<Category, MenuViewHolder>   adapterCategorylist;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);
        init();

        Intent service = new Intent(this, ListenOrderService.class);
        startService(service);

        recyclerMenu = findViewById(R.id.recycler_menu);
        recyclerMenu.setLayoutManager(new GridLayoutManager(this, 2));

        fapCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomeScreenActivity.this, CartActivity.class);
                startActivity(i);
            }
        });

        fapCart.setCount(new Database(this).getCountCart());

        if (Common.isConnectedToInternet(this)){
            loadListCategory();
        }else {
            Toast.makeText(HomeScreenActivity.this, "Please check your internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        ActionBarDrawerToggle  actionBarDrawerToggle = new ActionBarDrawerToggle(
                this, drawer,toolbar,
                R.string.navigation_drawer_opne,
                R.string.navigation_drawer_close);
        drawer.setDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);


        View HeaderView = navigationView.getHeaderView(0);
        txtFullname = HeaderView.findViewById(R.id.txtFullName);
        txtFullname.setText(Common.currentUser.getName());

        //Setup Slider
        setupSlider();
    }

    private void setupSlider() { // TODO: 1/18/21 ********************* READ AGAIN
        mSlider = (SliderLayout) findViewById(R.id.slider);
        image_list = new HashMap<>();

        final DatabaseReference banners = FirebaseDatabase.getInstance().getReference("banners");
        banners.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                    Banner banner = postSnapShot.getValue(Banner.class);
                    // concat string name and id
                    // BanhMi_01 => BanhMi để show description, 01 cho food id để click
                    image_list.put(banner.getName() + "@@@" + banner.getId(), banner.getImage());
                }
                for (String key : image_list.keySet()) {
                    String[] keySplit = key.split("@@@");
                    String nameOfFood = keySplit[0];
                    String idOfFood = keySplit[1];

                    //tạo slider
                    final TextSliderView textSliderView = new TextSliderView(getBaseContext());
                    textSliderView.description(nameOfFood)
                            .image(image_list.get(key))
                            .setScaleType(BaseSliderView.ScaleType.Fit)
                            .setOnSliderClickListener(new BaseSliderView.OnSliderClickListener() {
                                @Override
                                public void onSliderClick(BaseSliderView slider) {
                                    Intent intent = new Intent(HomeScreenActivity.this, FoodDetailsActivity.class);
                                    //gửi food id cho foodDetail
                                    intent.putExtras(textSliderView.getBundle());
                                    startActivity(intent);
                                }
                            });
                    //add extra bundle
                    textSliderView.bundle(new Bundle());
                    textSliderView.getBundle().putString("FoodId", idOfFood);
                    textSliderView.setPicasso(Picasso.with(HomeScreenActivity.this));
                    mSlider.addSlider(textSliderView);

                    // remove event sau khi finish
                    banners.removeEventListener(this);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        mSlider.setPresetTransformer(SliderLayout.Transformer.Background2Foreground);
        mSlider.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom);
        mSlider.setCustomAnimation(new DescriptionAnimation());
        mSlider.setDuration(3500);
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (adapterCategorylist != null){adapterCategorylist.stopListening();}
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapterCategorylist != null){adapterCategorylist.startListening();}
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapterCategorylist != null){adapterCategorylist.startListening();}
        fapCart.setCount(new Database(this).getCountCart());
    }

    private void loadListCategory() {
        Query query = FirebaseDatabase.getInstance().getReference().child("categories");
        FirebaseRecyclerOptions<Category> options = new FirebaseRecyclerOptions.Builder<Category>()
                .setQuery(query, Category.class)
                .build();
         adapterCategorylist = new FirebaseRecyclerAdapter<Category, MenuViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MenuViewHolder holder, int position, @NonNull Category model) {
                Log.d(TAG, "onBindViewHolder: " +model.getName()+model.getImage());
                Glide
                        .with(HomeScreenActivity.this)
                        .load(model.getImage())
                        .centerCrop()
                        .into(holder.imgCate);
                holder.nameCate.setText(model.getName());
                final Category clickItem = model;
                holder.setItemClickListener(new ItemClickListener() {
                    @Override
                        public void onClick(View view, int position, boolean longClick) {
                        Toast.makeText(HomeScreenActivity.this, "Go to deltais of"+clickItem.getName(), Toast.LENGTH_SHORT).show();
                        String id = adapterCategorylist.getRef(position).getKey();
                        Intent idCategory = new Intent(HomeScreenActivity.this, FoodListActivity.class);
                        idCategory.putExtra("IdCategory", id);
                        startActivity(idCategory);
                    }
                });
            }

            @NonNull
            @Override
            public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(HomeScreenActivity.this).inflate(R.layout.layout_list_category, parent, false);
                return new MenuViewHolder(view);
            }
        };
        recyclerMenu.setAdapter(adapterCategorylist);
    }

    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {

        }
    }


    private void showSignOutDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(HomeScreenActivity.this);
        alertDialog.setTitle("Đăng xuất");
        alertDialog.setIcon(getDrawable(R.drawable.ic_log_out));
        alertDialog.setMessage("Bạn có chắc chắn muốn đăng xuất ?");
        LayoutInflater inflater = LayoutInflater.from(this);
        alertDialog.setNegativeButton("Hủy bỏ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        alertDialog.setPositiveButton("Đồng ý", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Paper.book().destroy();
                FirebaseAuth.getInstance().signOut();
                Intent signIn = new Intent(HomeScreenActivity.this, LoginActivity.class);
                signIn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(signIn);
            }
        });
        alertDialog.show();
    }


    private void init() {
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        fapCart = findViewById(R.id.fab);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.page_menu) {
            Toast.makeText(this, "Go to menu", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.page_historycart) {
            Toast.makeText(this, "Go to history", Toast.LENGTH_SHORT).show();
            Intent OrderIntent = new Intent(HomeScreenActivity.this, OrderListActivity.class);
            startActivity(OrderIntent);
        } else if (id == R.id.page_logout) {
            showSignOutDialog();
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
