package com.example.stayride;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.stayride.data.DataStore;
import com.example.stayride.databinding.ActivityMainBinding;
import com.example.stayride.fragment.CabsFragment;
import com.example.stayride.fragment.GuestsFragment;
import com.example.stayride.fragment.HomeFragment;
import com.example.stayride.fragment.MoreFragment;
import com.example.stayride.fragment.RoomsFragment;

public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNav.setOnItemSelectedListener(this::onTabSelected);
        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private boolean onTabSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        Fragment fragment;
        String title;
        if (id == R.id.nav_home) {
            fragment = new HomeFragment();
            title = DataStore.get().homestay.name;
        } else if (id == R.id.nav_rooms) {
            fragment = new RoomsFragment();
            title = "Rooms & Bookings";
        } else if (id == R.id.nav_guests) {
            fragment = new GuestsFragment();
            title = "Guest Database";
        } else if (id == R.id.nav_cabs) {
            fragment = new CabsFragment();
            title = "Cabs & Drivers";
        } else {
            fragment = new MoreFragment();
            title = "More";
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, fragment)
                .commit();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
        return true;
    }

    public void selectTab(int menuItemId) {
        binding.bottomNav.setSelectedItemId(menuItemId);
    }
}
