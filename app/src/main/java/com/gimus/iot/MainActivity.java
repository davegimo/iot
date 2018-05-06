package com.gimus.iot;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gimus.iot.fragments.AccFragment;
import com.gimus.iot.fragments.ConnectFragment;
import com.gimus.iot.utils.Ticker;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, Ticker.OnTickReceiver {

    public static final int FRG_ACC=1;
    public static final int FRG_LOGIN=2;

    protected Fragment currentFragment;
    protected Fragment testFragment;
    protected Fragment accFragment;
    protected Fragment connectFragment;
    protected Ticker t;

    protected static int TKR_MAIN=1;
    IotApplication app;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        app= (IotApplication) Global.G.context;
        Global.G.mainActivity=this;
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        t = new Ticker(this,TKR_MAIN);
        t.start( 1000);


        app.Connect();
    }

    public void UpdateConnectionStatus(boolean connected ) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView tvConnectionStatus = (TextView) toolbar.findViewById(R.id.connection_status);
        if (connected)
            tvConnectionStatus.setText("Connesso");
        else
            tvConnectionStatus.setText("Disconnesso");
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case R.id.nav_connect:
                selectFragment(MainActivity.FRG_LOGIN);
                break;
            case R.id.nav_acc:
                selectFragment(MainActivity.FRG_ACC);
                break;


        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void selectFragment( int frag_id) {
        switch ( frag_id){
            case MainActivity.FRG_ACC:
                accFragment= AccFragment.newInstance();
                selectFragment(accFragment);
                break;

            case MainActivity.FRG_LOGIN:
                if (connectFragment == null)
                    connectFragment = ConnectFragment.newInstance();
                    selectFragment(connectFragment);
                    break;
        }
    }


    protected void selectFragment( Fragment frag) {
        int containerId=R.id.content_fragment;

        LinearLayout fl = (LinearLayout) findViewById(containerId);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (frag != currentFragment) {
            if (currentFragment != null)
                ft.remove(currentFragment);

            ft.add(containerId, frag);
            currentFragment = frag;
            ft.commit();
        }
    }


    @Override
    public void onTick(int Id) {
        if (accFragment != null) {
            if (currentFragment ==accFragment)
                ((AccFragment) accFragment).onTick();

        }
    }


}
