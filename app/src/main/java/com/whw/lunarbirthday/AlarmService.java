package com.whw.lunarbirthday;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import java.util.*;

/** Playback exists only while a user-configured birthday alarm is actively ringing. */
public class AlarmService extends Service {
    static AlarmService instance;
    static final int FOREGROUND_ID=701;
    final LinkedHashMap<String,Birthdays.Entry> active=new LinkedHashMap<>();
    final HashMap<String,String> dates=new HashMap<>();
    final HashMap<String,Runnable> expiry=new HashMap<>();
    final Handler handler=new Handler(Looper.getMainLooper());
    MediaPlayer player; Vibrator vibrator; AudioFocusRequest focus; String foregroundKey;
    static void stop(Context c,String id){if(instance!=null)instance.remove(id);}
    static boolean ringing(String id){return instance!=null && instance.active.containsKey(id);}
    @Override public void onCreate(){super.onCreate();instance=this;}
    @Override public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        String id=intent.getStringExtra("id"),date=intent.getStringExtra("date");Birthdays.Entry entry=null;
        for(Birthdays.Entry e:Birthdays.load(this))if(e.id.equals(id)&&e.enabled&&e.mode==2)entry=e;
        if(entry==null || date==null || date.equals(Reminders.state(this).getString(id+".done","")) || !date.equals(Reminders.state(this).getString(id+".active",""))){if(active.isEmpty())stopSelf();return START_NOT_STICKY;}
        Reminders.channels(this);
        if(!Reminders.allowed(this) || getSystemService(NotificationManager.class).getNotificationChannel(Reminders.ALARM).getImportance()==NotificationManager.IMPORTANCE_NONE){stopSelf();return START_NOT_STICKY;}
        if(foregroundKey!=null && !foregroundKey.equals(id) && active.containsKey(foregroundKey))getSystemService(NotificationManager.class).notify(foregroundKey,1,Reminders.notification(this,active.get(foregroundKey),dates.get(foregroundKey),false,true,false));
        active.put(id,entry);dates.put(id,date);foregroundKey=id;
        Notification n=Reminders.notification(this,entry,date,false,true,false);
        getSystemService(NotificationManager.class).cancel(id,1);startForeground(FOREGROUND_ID,n);
        Runnable old=expiry.remove(id);if(old!=null)handler.removeCallbacks(old);
        Runnable end=()->{Birthdays.Entry e=active.get(id);String d=dates.get(id);remove(id);if(e!=null)getSystemService(NotificationManager.class).notify(id,1,Reminders.notification(this,e,d,false,false,false));};
        expiry.put(id,end);handler.postDelayed(end,10*60*1000);
        if(player==null)play();
        return START_NOT_STICKY;
    }
    void play(){
        AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        AudioManager audio=getSystemService(AudioManager.class);
        focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attributes).setOnAudioFocusChangeListener(change->{if(change==AudioManager.AUDIOFOCUS_LOSS || change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)stopSelf();}).build();
        if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){stopSelf();return;}
        try{
            player=new MediaPlayer();player.setAudioAttributes(attributes);
            android.net.Uri sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if(sound==null)sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            player.setDataSource(this,sound);player.setLooping(true);player.setWakeMode(this,PowerManager.PARTIAL_WAKE_LOCK);
            player.setOnPreparedListener(MediaPlayer::start);
            player.setOnErrorListener((p,what,extra)->{stopSelf();return true;});player.prepareAsync();
            vibrator=getSystemService(Vibrator.class);vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0,500,500,500,1500},0),attributes);
        }catch(Exception ex){stopSelf();}
    }
    void remove(String id){
        active.remove(id);dates.remove(id);Runnable r=expiry.remove(id);if(r!=null)handler.removeCallbacks(r);
        getSystemService(NotificationManager.class).cancel(id,1);
        if(active.isEmpty()){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();release();}
        else{String remaining=active.containsKey(foregroundKey)?foregroundKey:active.keySet().iterator().next();foregroundKey=remaining;getSystemService(NotificationManager.class).cancel(remaining,1);startForeground(FOREGROUND_ID,Reminders.notification(this,active.get(remaining),dates.get(remaining),false,true,false));}
    }
    void release(){if(player!=null){player.release();player=null;}if(vibrator!=null){vibrator.cancel();vibrator=null;}if(focus!=null){getSystemService(AudioManager.class).abandonAudioFocusRequest(focus);focus=null;}}
    @Override public void onDestroy(){
        handler.removeCallbacksAndMessages(null);release();
        for(String id:active.keySet())getSystemService(NotificationManager.class).notify(id,1,Reminders.notification(this,active.get(id),dates.get(id),false,false,false));
        active.clear();if(instance==this)instance=null;super.onDestroy();
    }
}
