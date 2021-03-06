package com.example.sf.TimeShaft.ComparedTimeShaft;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RecoverySystem;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.example.sf.CONSTANTS_SF;
import com.example.sf.DataBase.Poetry;
import com.example.sf.PoetryInfo.PoetryList;
import com.example.sf.Server.Server;
import com.example.sf.TimeShaft.ComparedTimeShaft.PoetChoiceListview.PoetMutipleAdapter;
import com.example.sf.TimeShaft.Poetries;
import com.example.sf.TimeShaft.PortTimerShaft;
import com.example.sf.TimeShaft.TS_Thread;
import com.example.sf.amap3d.R;

import java.util.ArrayList;
import java.util.List;

public class PortComapredTimerShaft extends Activity implements View.OnClickListener {

    private DisplayMetrics metrics=new DisplayMetrics();

    private MapView mapView;
    private MapView textureMapView;
    private List<TS_Thread> threads;
    private LinearLayout controller;
    private Button prevButton;
    private Button startButton;
    private Button nextButton;
    private int width;
    private int height;
    private int pnum=2;
    private int poetId1;
    private int poetId2;
//    判断当前状态是暂停还是播放的标志位
    private boolean isStart=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.poet_timer_sheft);//设置对应的XML布局文件
        threads=new ArrayList<>();
        Intent getPoetId=getIntent();
        poetId1= (int) getIntent().getExtras().get(PoetMutipleAdapter.POET_ID_KEY1);
        poetId2=(int) getIntent().getExtras().get(PoetMutipleAdapter.POET_ID_KEY2);
//        poetId1=getPoetId.getIntExtra(PoetMutipleAdapter.POET_ID_KEY1,-1);
//        poetId1=getPoetId.getIntExtra(PoetMutipleAdapter.POET_ID_KEY2,-1);

        Log.i("let_me_crazy_1", "——————————"+poetId1+"——————————");
        Log.i("let_me_crazy_2", "——————————"+poetId2+"——————————");

        mapView = (MapView) findViewById(R.id.map);
//        textureMapView = (MapView) findViewById(R.id.texturemapview);
        mapView.onCreate(savedInstanceState);
//        textureMapView.onCreate(savedInstanceState);


        prevButton =(Button) findViewById(R.id.prev);
        startButton=(Button)findViewById(R.id.start);
        nextButton =(Button)findViewById(R.id.next);

        init(mapView,savedInstanceState);
//        init(textureMapView,savedInstanceState);
        try {
            TimerSControl(mapView,mapView,poetId1,poetId2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    public void init(MapView mapView, Bundle savedInstanceState){

        mapView.onCreate(savedInstanceState);
        AMap aMap = mapView.getMap();

        controller=(LinearLayout)findViewById(R.id.controller);
            Button b1 = new Button(this);
            b1.setWidth(40);
            b1.setHeight(40);
            b1.setText("Poetry1");;
            b1.setId((int)1);
            b1.setOnClickListener(this);
        Button b2 = new Button(this);
        b2.setWidth(40);
        b2.setHeight(40);
        b2.setText("Poetry2");;
        b2.setId((int)2);
        b2.setOnClickListener(this);
        controller.addView(b1);
        controller.addView(b2);
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        /**
         * 需要修改offset
         */
        width=metrics.widthPixels/2;
        height=metrics.heightPixels/2;
    }


    /**
     * 时间轴的控制函数*/
    private void TimerSControl(MapView mapView1, MapView mapView2, int poetId1, int poetId2) throws InterruptedException {
        final AMap aMap1 = mapView1.getMap();
        final AMap aMap2=mapView2.getMap();
        final boolean[] isPoet1Ready = {false};
        final boolean[] isPoet2Ready = {false};
//        List<Poetry> poetries1= DataSupport.where("poet_id=?",String.valueOf(poetId1)).find(Poetry.class);
//        List<Poetry> poetries2= DataSupport.where("poet_id=?",String.valueOf(poetId2)).find(Poetry.class);
       final List<Poetry> poetries1=new ArrayList<>();
       final List<Poetry> poetries2=new ArrayList<>();
        final Handler hander = new Handler(){
            public void handleMessage(Message m){
                Toast.makeText(PortComapredTimerShaft.this,"HELLO",Toast.LENGTH_SHORT);
                Log.i("TAG", "handleMessage: **********__________________))))))))))))))))))))))))))");
//                ArrayList<Integer> poetryList= (ArrayList<Integer>) m.obj;
                Poetries poetries=(Poetries)m.obj;
//                Log.i(TAG, "handleMessage: "+poetryList.toString());
                Intent intent=new Intent(PortComapredTimerShaft.this,PoetryList.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra(TS_Thread.POETRY_ID_LIST,poetries.poetriesId);
                intent.putExtra(TS_Thread.POETRY_TITLE_LIST,poetries.poetriesName);
                startActivity(intent);
            }
        };
        final Handler couldStart=new Handler(){
            @Override
            public void handleMessage(Message msg){
                if((isPoet1Ready[0]&&isPoet2Ready[0])){
                    nextRun(aMap1,aMap2,poetries1,poetries2,hander);

                }
            }
        };

        Handler serMsgHander1=new Handler(){
            @Override
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                String result=msg.obj.toString();
                PortTimerShaft.extraJson(result,poetries1);
                isPoet1Ready[0] =true;
                couldStart.sendMessage(new Message());
            }
        };

        Handler serMsgHander2=new Handler(){
            @Override
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                String result=msg.obj.toString();
                PortTimerShaft.extraJson(result,poetries2);
                isPoet2Ready[0] =true;
                couldStart.sendMessage(new Message());
            }
        };



        String sql="select * from "+ CONSTANTS_SF.TABLE_POETRIES_L.TABLE_NAME +" where "+ CONSTANTS_SF.TABLE_POETRIES_L.POET_ID+"="+poetId1+ " order by age";
        Server server=new Server(serMsgHander1, CONSTANTS_SF.URL_ROOT+"poetriesList");
        server.post(sql);
        String sql1="select * from "+ CONSTANTS_SF.TABLE_POETRIES_L.TABLE_NAME +" where "+ CONSTANTS_SF.TABLE_POETRIES_L.POET_ID+"="+poetId2+ " order by age";
        Server server1=new Server(serMsgHander2, CONSTANTS_SF.URL_ROOT+"poetriesList");
        server1.post(sql1);
    }
    /*在所有数据执行完毕之后要执行的函数*/
    public void nextRun(AMap aMap1,AMap aMap2, List<Poetry> poetries1,List<Poetry> poetries2,Handler handler){
        recovercamera();
        final TS_Thread ts=new TS_Thread(aMap1, poetries1,width,height, PortComapredTimerShaft.this,handler,false);
        final TS_Thread tf=new TS_Thread(aMap2, poetries2,width,height, PortComapredTimerShaft.this,handler,false);
        threads.add(ts);
        threads.add(tf);
        Thread thread1=new Thread(ts);
        thread1.start();
        Thread thread2=new Thread(tf);
        thread2.start();
        aMap1.setOnMapTouchListener(new AMap.OnMapTouchListener() {
            @Override
            public void onTouch(MotionEvent motionEvent) {
                int i=0;
                for(;i<threads.size();i++){
                    threads.get(i).setIsmain(false);
                }
            }
        });
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ts.remove();
                tf.remove();
            }
        });
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//               自动播放按钮，设置线程的自动持续绘制及暂停
                if(isStart) {
                    isStart=false;
                    startButton.setBackgroundResource(R.drawable.stop);

                    ts.setWait(false);
                    tf.setWait(false);
                    ts.resumeThread();
                    tf.resumeThread();
                }
                else{
                    isStart=true;
                    startButton.setBackgroundResource(R.drawable.start);

                    ts.setWait(true);
                    tf.setWait(true);
                }
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                唤醒时间轴绘制线程，绘制下一步
                ts.resumeThread();
                tf.resumeThread();
                //设置中心点和缩放比例

            }
        });
    }

    public void recovercamera(){
       mapView.getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(new LatLngBounds(new LatLng(2.0, 72.0), new LatLng(54.0, 136.0)), 10), 1000, new AMap.CancelableCallback() {
                    @Override
                    public void onFinish() {

                    }

                    @Override
                    public void onCancel() {

                    }
                }
        );
    }



    @Override
    public void onClick(View v){
        int id=v.getId();
        switch(id){
            case 1:{
                if(threads.get(0).isIsmain()){
                    recovercamera();
                    threads.get(0).setIsmain(false);
                }else{
                    threads.get(1).setIsmain(false);
                    threads.get(0).setIsmain(true);
                }
                break;
            }
            case 2:{
                if(threads.get(1).isIsmain()){
                    recovercamera();
                    threads.get(1).setIsmain(false);
                }else{
                    threads.get(0).setIsmain(false);
                    threads.get(1).setIsmain(true);
                }
                break;
            }
        }
    }
    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
//        textureMapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
//        textureMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
//        textureMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
//        textureMapView.onDestroy();
    }

}
