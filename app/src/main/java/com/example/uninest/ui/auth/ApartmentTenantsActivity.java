package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApartmentApi;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApartmentTenantsActivity extends AppCompatActivity {

    private LinearLayout tenantList;
    private ApartmentApi apartmentApi;
    private String apartmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apartment_tenants);

        tenantList = findViewById(R.id.layoutTenantList);
        TextView tvBuildingName = findViewById(R.id.tvBuildingName);
        TextView tvApartmentName = findViewById(R.id.tvApartmentName);
        TextView tvTenantCount = findViewById(R.id.tvTenantCount);

        apartmentApi = ApiClient.getApartmentApi();
        // Get extras from previous screen (later, when we wire navigation)
        String buildingName = getIntent().getStringExtra("EXTRA_BUILDING_NAME");
        String apartmentName = getIntent().getStringExtra("EXTRA_APARTMENT_NAME");
        apartmentId = getIntent().getStringExtra("EXTRA_APARTMENT_ID");

        if (buildingName != null && !buildingName.isEmpty()) {
            tvBuildingName.setText(buildingName);
        } else {
            tvBuildingName.setText("Apartments");
        }

        if (apartmentName != null && !apartmentName.isEmpty()) {
            tvApartmentName.setText(apartmentName);
        } else {
            tvApartmentName.setText("Apartment");
        }

        // For now: hard-coded dummy tenants
        //addDummyTenants();
        // Check if ID is available before fetching
        if (apartmentId != null && !apartmentId.isEmpty()) {
            fetchTenants(apartmentId);
        }
        else {
            // Handle error: ID is missing, cannot fetch tenants
            Log.e("TENANTS_ACTIVITY", "Apartment ID is missing.");
            tvTenantCount.setText("Error: ID Missing");
        }

        // Update tenant count text based on dummy list
        //int occupied = 3;  // number of dummy tenants
        //int capacity = 5;  // just an example
        //tvTenantCount.setText(occupied + "/" + capacity + " Tenants");

    }

    private void fetchTenants(String id) {


        // Call the endpoint: GET /api/v1/apartments/{apartmentId}/users
        Call<List<User>> call = apartmentApi.getUsersForApartment(id);

        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> tenants = response.body();
                    displayTenants(tenants);
                } else {
                    Log.e("API_CALL", "Failed to fetch tenants: " + response.code());
                    // Optionally show a failure message to the user
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Log.e("API_CALL", "Network error fetching tenants", t);
                // Optionally show a network error message
            }
        });
    }

    private void displayTenants(List<User> tenants) {
        // Clear the hard-coded views first
        tenantList.removeAllViews();

        // Update the tenant count
        int occupied = tenants.size();

        int capacity = 5; // Hard-coded for now
        TextView tvTenantCount = findViewById(R.id.tvTenantCount);
        tvTenantCount.setText(occupied + "/" + capacity + " Tenants");

        for (User tenant : tenants) {
            TenantCardView card = new TenantCardView(this);


            card.setTenantName(tenant.getEmail());

            card.setRoomLabel("Room");

            // Add the card to the container
            tenantList.addView(card);
        }
    }



        private void addDummyTenants() {
            TenantCardView t1 = new TenantCardView(this);
            t1.setTenantName("Maya Smith");
            t1.setRoomLabel("Room 1");
            tenantList.addView(t1);

            TenantCardView t2 = new TenantCardView(this);
            t2.setTenantName("Ruth Mainland");
            t2.setRoomLabel("Room 2");
            tenantList.addView(t2);

            TenantCardView t3 = new TenantCardView(this);
            t3.setTenantName("Sarah Williams");
            t3.setRoomLabel("Room 3");
            tenantList.addView(t3);
        }
    }

