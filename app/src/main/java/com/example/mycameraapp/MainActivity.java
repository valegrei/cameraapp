package com.example.mycameraapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity implements
        CamaraFragment.CamaraFragmentListener, FotoFragment.FotoFragmentListener {

    private static final String C_FRAGMENT = "C_FRAGMENT";
    private static final String FILE_PATH = "FILE_PATH";
    private int c_fragment;
    private String filePath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        c_fragment = 0;
        filePath = null;

        if(savedInstanceState !=null){
            c_fragment = savedInstanceState.getInt(C_FRAGMENT);
            filePath = savedInstanceState.getString(FILE_PATH);
        }

        setFragment(c_fragment, filePath);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(C_FRAGMENT,c_fragment);
        outState.putString(FILE_PATH, filePath);
        super.onSaveInstanceState(outState);
    }

    private void setFragment(int c_fragment, String filePath) {
        this.c_fragment = c_fragment;
        this.filePath = filePath;
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        Fragment fragment = null;
        switch (c_fragment) {
            case 0:
                fragment = CamaraFragment.newInstance();
                break;
            case 1:
                fragment = FotoFragment.newInstance(filePath);
                break;
            default:
                break;
        }
        if (fragment != null) {
            transaction.replace(R.id.container, fragment); //container
            transaction.commit();
        }
    }

    @Override
    public void cerrar() {
        finish();
    }

    @Override
    public void mostrarFoto(String filePath) {
        setFragment(1, filePath);
    }

    @Override
    public void volver() {
        setFragment(0,null);
    }
}