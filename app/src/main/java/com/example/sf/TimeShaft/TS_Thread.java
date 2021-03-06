package com.example.sf.TimeShaft;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.NavigateArrow;
import com.amap.api.maps.model.animation.TranslateAnimation;
import com.example.leesanghyuk.LayoutDemo.PushStoryDemo;
import com.example.sf.DataBase.Poetry;
import com.example.sf.DrawMap.DrawLine.DrawLine;
import com.example.sf.DrawMap.DrawMark.DrawMark;
import com.example.sf.PoetryInfo.PoetryList;
import com.example.sf.amap3d.MainActivity;
import com.example.sf.amap3d.R;
import com.example.LJJ.Global.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Created by sf on 17-12-11.
 */
public class TS_Thread extends Thread {
    private int ANIMATION_PERIOD=5;
    private final Object lock = new Object();
    //屏幕中心像素
    private int width;
    private AMap.CancelableCallback cb;

    private int height;
    //    需要顺序显示的点集
    private List<Poetry> poetryList;
    //    当前显示到了第几个
    private int num;
    //    自动播放时单步延迟
    private int delay = 100;
    //    已经显示在地图上的点将入栈
    private Stack<Marker> markerStack;
    //    画点的对象
    private DrawMark drawMark;
    //    当前处理的地图对象
    private AMap aMap;
    //  要描绘的点的数目
    private int windows=3;
    private int sumNum;
    private double[] borderUP;
    private double[] borderLeft;
    private double[] borderRight;
    private double[] borderDown;
    private Stack<NavigateArrow> arrowLineStack;
    private DrawLine drawLine;
    //    线程是否自动绘制线程的标志，若为false则绘制完一步之后继续绘制，不用等待调用方指示，为true则绘制完一步之后进入等待状态
    private boolean isWait;
    //     事件描述
    private String event = "";
    //     诗人移动覆盖物
    private MarkerOptions poetry = new MarkerOptions();
    //左移gif
    private BitmapDescriptor leftmove;
    //右移gif
    private BitmapDescriptor rightmove;
    //静止gif
    private ArrayList<BitmapDescriptor> icons;
    private View dialogue;
    private TextView text;
    private View charac;
    private ImageView animationIV;
    private AnimationDrawable ad;
    private BitmapDescriptor still;
    private Marker mainc;
    private  Activity activity;
    private MyApp app;
    private Handler handler;
    private boolean ismain;
    //    用于存放每一个marker及其对应的诗词信息
    private Map<Marker,MarkerPoetryInfo> markerMarkerPoetryInfoMap;
    public void setWait(boolean wait) {
        isWait = wait;
    }
    //向下一个activity传递参数，诗的ID
    public static final String POETRY_ID_LIST="POETRY_ID_LIST";
    public static final String POETRY_TITLE_LIST="POETRY_TITLE_LIST";
    public TS_Thread(AMap aMap, List<Poetry> poetryList, int width, int height, final Activity activity, final Handler handler,boolean ismain) {
        this.activity=activity;
        this.handler=handler;
        this.width = 540;
        this.height = 910;
        this.sumNum = poetryList.size();
        this.ismain=ismain;
        this.aMap = aMap;
        this.poetryList=poetryList;
        this.markerStack = new Stack<>();
        this.drawMark = new DrawMark(aMap);
        this.arrowLineStack = new Stack<>();
        this.drawLine = new DrawLine(aMap);
        this.isWait = true;
        app=(MyApp)activity.getApplication();
        //initborder();
        cb=new AMap.CancelableCallback() {
            @Override
            public void onFinish() {

            }

            @Override
            public void onCancel() {

            }
        };
        markerMarkerPoetryInfoMap=new HashMap<>();
        charac = LayoutInflater.from(activity).inflate(R.layout.gifs,null);
        animationIV=(ImageView)charac.findViewById(R.id.animate);
        animationIV.setBackgroundResource(R.drawable.gif_pic);
        ad=(AnimationDrawable)animationIV.getBackground();
        //设置主人物覆盖物View
        still= BitmapDescriptorFactory.fromView(charac);
        icons=new ArrayList<>();
        this.poetry.period(5);
        mainc=aMap.addMarker(this.poetry);
        recover();
        if(poetryList.size()!=0) {
            mainc.setPosition(Poetry.getLatlng(poetryList.get(0)));
            mainc.showInfoWindow();
        }

    }

    public boolean isIsmain() {
        return ismain;
    }

    public void setIsmain(boolean ismain) {
        this.ismain = ismain;
    }

    /**
     * 绘制时间轴的下一段
     */
    public void addNext() {
//        记录当前点的诗词信息
        MarkerPoetryInfo markerPoetryInfo = new MarkerPoetryInfo();
        Poetry poetry = poetryList.get(num);
        LatLng latLng = Poetry.getLatlng(poetry);
        if(num+1==sumNum){
            markerPoetryInfo.addPoetryNum(1);
            markerPoetryInfo.getPoetriesId().add(poetry.getId());
            markerPoetryInfo.getPoetriesName().add(poetry.getTitle());
        }else
        {
        if (poetry.getTitle() != null && !poetryList.get(num + 1).getTitle().equals("")) {
            markerPoetryInfo.addPoetryNum(1);
            markerPoetryInfo.getPoetriesId().add(poetry.getId());
            markerPoetryInfo.getPoetriesName().add(poetry.getTitle());
        }
//        数据库中有很多条目是诗人连续在一个地方，略过这些条目
//        有些条目可能没有录入位置信息，略过这些条目

        if (num + 1 == 1 && sumNum == 1) {
            System.out.println();
        }
//            当前的num值在循环中的处理过程中是否会造成越界（false）
        boolean b1 = (num >= 0) && (num < (sumNum - 1));

//            当前地点与下一地点是否为同一个地方
        boolean b2 = ((int) latLng.latitude * 100 == (int) Poetry.getLatlng(poetryList.get(num + 1)).latitude * 100) && ((int) latLng.longitude * 100 == (int) Poetry.getLatlng(poetryList.get(num + 1)).longitude * 100);
//          数据库下一条目是=地点坐标是否未可知
        boolean b3 = (Poetry.getLatlng(poetryList.get(num + 1)).latitude < 0) || (Poetry.getLatlng(poetryList.get(num + 1)).longitude < 0);
        while (((num >= 0) && (num < (sumNum - 1))) && ((((int) latLng.latitude * 100 == (int) Poetry.getLatlng(poetryList.get(num + 1)).latitude * 100) && ((int) latLng.longitude * 100 == (int) Poetry.getLatlng(poetryList.get(num + 1)).longitude * 100)) || ((Poetry.getLatlng(poetryList.get(num + 1)).latitude < 0) || (Poetry.getLatlng(poetryList.get(num + 1)).longitude < 0)))) {
//                添加上一个marker标记处的诗词id
//                markerMarkerPoetryInfoMap.get(markerStack.peek()).poetriesId.add(poetryList.get(num+1).getId());
            if (num + 1 == 1 && sumNum == 1) {
                System.out.println();
            }

            if (poetryList.get(num + 1).getTitle() != null && !poetryList.get(num + 1).getTitle().equals("")) {
                int poetryId = poetryList.get(num + 1).getId();
                markerPoetryInfo.getPoetriesId().add(poetryId);
                markerPoetryInfo.getPoetriesName().add(poetryList.get(num + 1).getTitle());

                markerPoetryInfo.addPoetryNum(1);
            }
            poetryList.remove(num + 1);
//                latLng = Poetry.getLatlng(poetryList.get(num));
            sumNum--;
        }

        }
        num++;
        //改变地图中心点
//        划线
            if (markerStack.size() >= 1) {
                NavigateArrow navigateArrow = drawLine.drawLineWithArrow(10, markerStack.peek().getOptions().getPosition(), latLng);
                if (!arrowLineStack.empty()) {
                    arrowLineStack.peek().setTopColor(0xffff0000);
                }
                arrowLineStack.push(navigateArrow);
            }
//        画点
        Marker marker = drawMark.drawMark(latLng);
            marker.getOptions().infoWindowEnable(true);//
            marker.setTitle(poetry.getCity_ancient()+"."+poetry.getCounty_ancient());
            mainc.setSnippet(poetry.getAgeOfPoet()+"岁\n"+"有诗"+markerPoetryInfo.getPoetryNum()+"首");
        markerMarkerPoetryInfoMap.put(marker, markerPoetryInfo);
        markerStack.push(marker);
        swiftmove(latLng,poetry.getCity_ancient()+"."+poetry.getCounty_ancient(),poetry.getAgeOfPoet()+"岁\n"+"有诗"+markerPoetryInfo.getPoetryNum()+"首");


    }

    @Override
    public void run() {
        //相应Marker的点击事件
        aMap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if(markerMarkerPoetryInfoMap.containsKey(marker)){
                    Toast.makeText(activity,markerMarkerPoetryInfoMap.get(marker).getPoetriesId().toString(),Toast.LENGTH_SHORT).show();
                   Intent intent=new Intent(activity,PoetryList.class);
                   intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra(POETRY_ID_LIST,markerMarkerPoetryInfoMap.get(marker).getPoetriesId());
                    intent.putExtra(POETRY_TITLE_LIST,markerMarkerPoetryInfoMap.get(marker).getPoetriesName());
                activity.startActivity(intent);
                    Message message=new Message();
                    message.obj=markerMarkerPoetryInfoMap.get(marker).getPoetrise();
                    handler.sendMessage(message);

                }
                if(app.isIswait()) {
                    Intent intent = new Intent(activity, PushStoryDemo.class);
                    activity.startActivity(intent);
                    app.setIswait(false);
                }
                return false;
            }
        });
        while (num < sumNum) {
            addNext();
            if (isWait) {
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    this.resumeThread();
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
//         当所有点描绘完毕时,线程不退出，等待进一步的操作
            while (num == sumNum) {
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 移除最近添加的一个点
     */
    public boolean remove1() {
        boolean b;
        if (markerStack.size() <= 0) {
            b = false;
        } else {
//            移除点
            markerStack.pop().remove();
            num--;
            b = true;
        }
        if (arrowLineStack.size() <= 0) {
        } else {
            arrowLineStack.pop().remove();
        }
        return b;
    }

    public void initborder() {
        borderUP = new double[poetryList.size()];
        borderDown = new double[poetryList.size()];
        borderLeft=new double[poetryList.size()];
        borderRight=new double[poetryList.size()];
        ArrayList<Integer> latqueue = new ArrayList<>();
        ArrayList<Integer> longqueue=new ArrayList<>();
        boolean islatwait=true;
        boolean islongwait=true;
        for (int i = 0; i < poetryList.size(); i++) {
            double latlng=poetryList.get(i).getLatitude();
            double longti=poetryList.get(i).getLongtitude();
            islatwait=true;
            islongwait=true;
            for(int j=0;j<latqueue.size();j++){
                if(latqueue.get(j)<=i-windows){
                    latqueue.remove(j);
                    longqueue.remove(j);
                }
                if(islatwait){
                    if(poetryList.get(latqueue.get(j)).getLatitude()>latlng){
                        continue;
                    }else{
                        latqueue.add(j,i);
                        islatwait=false;
                    }
                }
                if (islongwait) {
                    if(poetryList.get(longqueue.get(j)).getLongtitude()>longti){
                        continue;
                    }else{
                        longqueue.add(j,i);
                        islongwait=false;
                    }

                }
            }
            if(islatwait){
                latqueue.add(i);
            }
            if(islongwait){
                longqueue.add(i);
            }
            borderUP[i]=poetryList.get(latqueue.get(0)).getLatitude();
            borderDown[i]=poetryList.get(latqueue.get(latqueue.size()-1)).getLatitude();
            borderLeft[i]=poetryList.get(longqueue.get(longqueue.size()-1)).getLongtitude();
            borderDown[i]=poetryList.get(latqueue.get(0)).getLongtitude();
        }
    }
    public void swiftmove(LatLng lb){
        mainc.hideInfoWindow();//隐藏事件窗口
        TranslateAnimation ta=new TranslateAnimation(lb);//覆盖物滑动动画
        ta.setDuration(1000);//动画时长
        mainc.setAnimation(ta);
        mainc.startAnimation();
        LatLng la= aMap.getCameraPosition().target;
        LatLng newloc;
        double lat=(lb.latitude-la.latitude)/1000.0;
        double longti=(lb.longitude-la.longitude)/1000.0;
        LatLngBounds.Builder b=new LatLngBounds.Builder();
        for(int j=num;j>0&&j>num-windows;j--){
            b.include(new LatLng(poetryList.get(j).getLatitude(),poetryList.get(j).getLongtitude()));
        }
        LatLngBounds llb=b.build();
        aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(llb, 10));
        for(int i=0;i<500;i++){
            newloc=new LatLng(la.latitude+lat*i,la.longitude+longti*i);
            aMap.moveCamera(CameraUpdateFactory.changeLatLng(newloc));//移动屏幕中心


            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //


        mainc.showInfoWindow();
    }

    public void flip(){
        icons.clear();
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_7c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_8c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_9c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_10c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_11c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_12c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_13c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_14c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_15c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_16c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_1c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_2c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_3c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_4c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_5c));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_6c));
        mainc.setIcons(icons);
    }

    public void recover(){
        icons.clear();
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_7));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_8));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_9));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_10));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_11));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_12));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_13));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_14));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_15));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_16));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_1));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_2));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_3));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_4));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_5));
        icons.add(new BitmapDescriptorFactory().fromResource(R.drawable.little2_6));
        mainc.setIcons(icons);
    }

    public void swiftmove(LatLng lb,String title,String snippet){
        mainc.hideInfoWindow();//隐藏事件窗口
        mainc.setPeriod(ANIMATION_PERIOD);
        LatLng latLng=mainc.getPosition();
        TranslateAnimation ta=new TranslateAnimation(lb);//覆盖物滑动动画
        ta.setDuration(1000);//动画时长
        mainc.setAnimation(ta);
        mainc.startAnimation();
        LatLng la= aMap.getCameraPosition().target;
        LatLng newloc;
        double lat=(lb.latitude-la.latitude)/1000.0;
        double longti=(lb.longitude-la.longitude)/1000.0;
        if(lb.longitude<latLng.longitude){
            flip();
        }else{
            recover();
        }
        if(ismain) {
            LatLngBounds.Builder b = new LatLngBounds.Builder();
            for (int j = num; j >= 0 && j >= num - windows; j--) {
                b.include(new LatLng(poetryList.get(j).getLatitude(), poetryList.get(j).getLongtitude()));
            }
            LatLngBounds llb = b.build();

            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(llb, 100), 1000, cb);
        }
        for(int i=0;i<1000;i++){
            //newloc=new LatLng(la.latitude+lat*i,la.longitude+longti*i);
            //aMap.moveCamera(CameraUpdateFactory.changeLatLng(newloc));//移动屏幕中心

            try {
                Thread.sleep(1);
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
        //
       // poetry.period(Integer.MAX_VALUE);
        mainc.setTitle(title);
        mainc.setSnippet(snippet);
        mainc.setPeriod(Integer.MAX_VALUE);
        if(lb.longitude>latLng.longitude) {
            recover();
        }else{
            flip();
        }
        mainc.showInfoWindow();
    }
    /*移除所有的点*/
    public boolean removeall(){
        while(remove());
        return true;
    }
    /**
     * 移除最近添加的一个点
     */
    public boolean remove() {
        boolean boo = false;
        if (markerStack.size() <= 0) {
            boo = false;
        } else {
//            移除点
            markerStack.pop().remove();
            num--;
            boo = true;
        }
        if (arrowLineStack.size() <= 0) {
        } else {
            arrowLineStack.pop().remove();
        }
        if(num>0){
        swiftmove(Poetry.getLatlng(poetryList.get(num-1)));}
        return boo;
    }

    public void resumeThread() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }
}
