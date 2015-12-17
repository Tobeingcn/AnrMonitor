package cn.tobeing.anrmonitor;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by sunzheng on 15/11/13.
 * 程序ANR的监听器，协助定位ANR的位置
 */
public class AnrMonitor {
    public static final String TAG="AnrMonitor";
    public static boolean isTest=true;
    public static AnrMonitor instance;
    private static final int MONITOR_INTERVAL=3000;
    private static final int MSG_HISTORY_COUNT=10;
    private Handler uiHandler;
    private Handler monitorHandler;
    private List<String> msgList;
    private int ANR_COUNT=5;
    private int mAnrCount=ANR_COUNT;
    private Thread mainThread;
    public static AnrMonitor getInstance(){
        if(instance==null){
            synchronized (AnrMonitor.class){
                if(instance==null){
                    instance=new AnrMonitor();
                }
            }
        }
        return instance;
    }
    private AnrMonitor(){
        uiHandler=new Handler(Looper.getMainLooper());
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                mainThread = Thread.currentThread();
            }
        });
        HandlerThread monitorThread=new HandlerThread("anr check");
        monitorThread.start();
        monitorHandler=new MonitorHandler(monitorThread.getLooper());
        msgList=new LinkedList<String>();
        startMonitor();
    }
    /**
     * start check
     */
    public void check(){
        if(checkMainThread()) {
            mAnrCount=ANR_COUNT;
            monitorHandler.removeMessages(MSG_START_MONITOR);
            monitorHandler.sendEmptyMessageDelayed(MSG_START_MONITOR, MONITOR_INTERVAL);
        }
    }
    /**
     * start check,the last 10 msgs whill be kepps
     */
    public void check(String msg){
        if(checkMainThread()) {
            check();
            msgList.add(addMessage(msg));
            if (msgList.size() > MSG_HISTORY_COUNT) {
                msgList.remove(0);
            }
        }
    }
    private String addMessage(String msg){
        return "["+getCurDateTimeString()+"]"+msg;
    }
    public void startMonitor(){
        check();
    }
    public void stopMonitor(){
        monitorHandler.removeMessages(MSG_START_MONITOR);
    }
    public void release(){
        instance=null;
        stopMonitor();
        monitorHandler.getLooper().quit();
    }
    public String getCurDateTimeString(){
        return new SimpleDateFormat("yyyy-MM-dd mm:ss:ms").format(new java.util.Date()); //格式化当前系统日期
    }
    private boolean checkMainThread(){
        return Looper.myLooper()== Looper.getMainLooper();
    }
    private final int MSG_START_MONITOR=0x0001;

    private boolean isANR=false;

    class MonitorHandler extends Handler {
        private MonitorHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_START_MONITOR:{
                    mAnrCount--;
                    isANR=true;
                    //if it is normail no message show be arrival here;
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            isANR=false;
                        }
                    });
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(isANR){
                        throw new RuntimeException("AnrMonitor Sucess"+getAnrMesage());
                    }else if(mAnrCount<ANR_COUNT&&mAnrCount>0){
                        sendEmptyMessageDelayed(MSG_START_MONITOR,MONITOR_INTERVAL);
                    }
                }
                break;
            }
        }
    }
    private String getAnrMesage(){
        List<String> tempList=new ArrayList<>();
        tempList.addAll(msgList);
        StringBuilder sb=new StringBuilder("msg:\n");
        if(mainThread!=null){
            StackTraceElement[] traceElements=mainThread.getStackTrace();
            sb.append("threadState:"+mainThread.getState());
            sb.append('\n');
            if(traceElements!=null){
                for (StackTraceElement element:traceElements){
                    sb.append("anr:");
                    sb.append(element.getClassName());
                    sb.append(".");
                    sb.append(element.getMethodName());
                    sb.append(":");
                    sb.append(element.getLineNumber());
                    sb.append('\n');
                }
            }
        }
        for (String msg:tempList){
            sb.append(msg);
            sb.append('\n');
        }
        tempList.clear();
        return sb.toString();
    }
}
