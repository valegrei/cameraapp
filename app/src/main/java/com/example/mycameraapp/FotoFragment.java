package com.example.mycameraapp;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.mycameraapp.databinding.FragmentFotoBinding;

import java.io.File;

public class FotoFragment extends Fragment {

    private static final String FILE_PATH = "FILE_PATH";
    private String filePath;
    private FragmentFotoBinding binding;
    private FotoFragmentListener mListener;

    public FotoFragment() {
        // Required empty public constructor
    }

    public static FotoFragment newInstance(String filePath) {
        FotoFragment fragment = new FotoFragment();
        Bundle args = new Bundle();
        args.putString(FILE_PATH,filePath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            filePath = getArguments().getString(FILE_PATH);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentFotoBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        File file = new File(filePath);
        Glide.with(context())
                .load(file)
                .into(binding.foto);
        binding.btnAceptar.setOnClickListener(v -> mListener.volver());
        binding.btnCancelar.setOnClickListener(v -> mListener.volver());
    }

    public Context context(){
        return getContext();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FotoFragmentListener) {
            mListener = (FotoFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement FotoFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface FotoFragmentListener {
        void volver();
    }
}