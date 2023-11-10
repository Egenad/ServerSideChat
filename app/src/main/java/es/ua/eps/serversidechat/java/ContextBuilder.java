package es.ua.eps.serversidechat.java;

import android.app.Application;

public class ContextBuilder extends Application {

    private static android.content.Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }


    public static android.content.Context getContext(){
        return mContext;
    }
}
