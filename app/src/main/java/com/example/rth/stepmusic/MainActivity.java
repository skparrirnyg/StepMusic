package com.example.rth.stepmusic;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rth.serviecs.MusicServices;
import com.example.rth.util.MyWifiManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity implements ServiceConnection,MusicServices.CallbackInMusicService{

    private MusicServices musicServices;    //播放音乐的服务
    private MyWifiManager myWifiManager;  //管理wifi
    private ProgressDialog pd;
    private TextView tvWifiName;    //显示连接的wifi的名称
    private TextView tvWifiSsid;    //显示wifi的ssid值
    private Button btnSearchWifi;   //搜索wifi的按钮
    private ListView listView;  //显示搜索到的wifi
    private ProgressBar progressBar;    //显示正在搜索
    private SimpleAdapter adapter;  //listView适配器
    private boolean wifiOpened = false; //标识wifi是否已经打开

    private static final Handler mainHan = new Handler(Looper.myLooper());  //接收异步消息
    private List<Map<String,String>> listData = new ArrayList<>();  //listView的数据源

    //MywifiManager的回调接口
    private final MyWifiManager.CallBack wifiCallback = new MyWifiManager.CallBack() {
        @Override
        public void wifiEnabled() {
            wifiOpened = true;
        }

        @Override
        public void rssiChanged(int rssi) {
            //rssi值发生变化
            tvWifiSsid.setText(getString(R.string.current_strength,rssi));
        }
    };

    private List<ScanResult> results;   //搜索到的wifi结果

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        myWifiManager = new MyWifiManager(this);
        myWifiManager.setCallBack(wifiCallback);
        wifiOpened = myWifiManager.isWifiEnable();
        //绑定服务
        bindService(new Intent(this, MusicServices.class), this, BIND_AUTO_CREATE);
    }

    private void initView() {
        pd = new ProgressDialog(this);
        pd.setMessage("正在加载网络音乐...");
        tvWifiName = (TextView) findViewById(R.id.tv_current_wifi);
        tvWifiSsid = (TextView) findViewById(R.id.tv_current_strength);
        listView = (ListView) findViewById(R.id.listview);
        btnSearchWifi = (Button) findViewById(R.id.btn_search_wifi);
        progressBar = (ProgressBar) findViewById(R.id.pb_loading);
        tvWifiName.setText(getString(R.string.current_wifi, "NULL"));
        tvWifiSsid.setText(getString(R.string.current_strength,0));
        initEvent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!wifiOpened) {
            //开启wifi
            myWifiManager.startWifi();
        }else {
            String apName = myWifiManager.getConnectWifiName();
            tvWifiName.setText(getString(R.string.current_wifi,TextUtils.isEmpty(apName) ? "未知热点" : apName));
        }
    }

    /**
     * 设置事件监听
     */
    private void initEvent() {
        btnSearchWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchWifi();
            }
        });
    }

    /**
     * 搜索周围热点
     */
    private void searchWifi() {
        if (wifiOpened) {
            progressBar.setVisibility(View.VISIBLE);
            myWifiManager.startScan();
            mainHan.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(progressBar.getVisibility() == View.VISIBLE) {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                    //获取搜索结果
                    results = myWifiManager.getScanResults();
                    //更新listView的数据
                    if(results.size() > 1) {
                        listData.clear();
                        for (int i = 0;i < results.size();i++) {
                            Map<String,String> data = new HashMap<String, String>();
                            String ssid = results.get(i).SSID;
                            data.put("name",TextUtils.isEmpty(ssid) ? "unkown name" : ssid);
                            listData.add(data);
                        }
                        if(adapter == null) {
                            adapter = new SimpleAdapter(MainActivity.this,listData,android.R.layout.simple_list_item_1,
                                    new String[]{"name"},new int[]{android.R.id.text1});
                            listView.setAdapter(adapter);
                        }else {
                            adapter.notifyDataSetChanged();
                        }
                    }else {
                        Toast.makeText(MainActivity.this,"未搜索到任何wifi", Toast.LENGTH_SHORT).show();
                    }
                }
            },500);
        } else {
            Toast.makeText(MainActivity.this, "wifi正在开启,请稍后", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 播放网络音乐
     * @param view
     */
    public void playUrlMusic(View view) {
        if(musicServices == null) {
            Toast.makeText(this,"服务未绑定，请稍候！",Toast.LENGTH_SHORT).show();{
        myWifiManager.unBindBroadcast();
        super.onDestroy();
        unbindService(this);
    }
        }else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    musicServices.playUrl("http://stepmusics-library.stor.sinaapp.com/%E6%AD%BB%E4%BA%86%E9%83%BD%E8%A6%81%E7%88%B1%20-%20%E4%BF%A1%E4%B9%90%E5%9B%A2.mp3");
                }
            }).start();
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        musicServices = ((MusicServices.LocalMusicBinder)iBinder).getService();
        musicServices.setCallbackInMusicService(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        musicServices.setCallbackInMusicService(null);
        musicServices = null;
    }

    @Override
    protected void onDestroy() {
        //取消广播
        myWifiManager.unBindBroadcast();
        super.onDestroy();
        unbindService(this);
    }

    /**
     * 在MusicService中回调
     * 正在加载网络音乐
     */
    @Override
    public void preparingMusic() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pd.show();
            }
        });
    }

    /**
     * 在MusicService中回调
     * 开始播放网络音乐
     */
    @Override
    public void startPlay() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(pd.isShowing()) {
                    pd.dismiss();
                }
            }
        });
    }
}
