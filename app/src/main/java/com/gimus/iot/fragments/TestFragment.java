package com.gimus.iot.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gimus.iot.R;

public class TestFragment extends Fragment {
    private static final String ARG_FRASE_TEST = "frase_test";

    public TestFragment() {
    }

    public static TestFragment newInstance(String fraseTest) {
        TestFragment fragment = new TestFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FRASE_TEST, fraseTest);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_test , container, false);
        TextView textView = (TextView) rootView.findViewById(R.id.frase_test);
        textView.setText(getArguments().getString(ARG_FRASE_TEST));
        return rootView;
    }
}

