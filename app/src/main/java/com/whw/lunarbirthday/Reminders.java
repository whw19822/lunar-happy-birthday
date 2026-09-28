package com.whw.lunarbirthday;

import android.app.*;
import android.content.*;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import java.time.*;

final class Reminders {
    static final String[] MODES={"轻提醒","重要提醒","强提醒"};
    static final String LIGHT="birthday_light_v2", IMPORTANT="birthday_important_v2", ALARM="birthday_alarm_v2";
    static SharedPreferences state(Context c){return c.getSharedPreferences("reminder_state",0);}
    static boolean exact(Context c){return Build.VERSION.SDK_INT<31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms();}
    static void channels(Context c){
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        NotificationChannel light=new NotificationChannel(LIGHT,"轻提醒 · 安静通知",NotificationManager.IMPORTANCE_LOW);
        light.setSound(null,null);light.enableVibration(false);nm.createNotificationChannel(light);
        NotificationChannel important=new NotificationChannel(IMPORTANT,"重要提醒 · 声音和振动",NotificationManager.IMPORTANCE_HIGH);
        important.enableVibration(true);important.setVibrationPattern(new long[]{0,300,200,300});nm.createNotificationChannel(important);
        NotificationChannel alarm=new NotificationChannel(ALARM,"强提醒 · 响铃控制",NotificationManager.IMPORTANCE_HIGH);
        alarm.setSound(null,new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());nm.createNotificationChannel(alarm);
    }
    static PendingIntent pending(Context c,Birthdays.Entry e,long time){
        Intent i=new Intent(c,ReminderReceiver.class).setData(Uri.parse("birthday://"+e.id)).putExtra("id",e.id).putExtra("time",time);
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static String signature(Birthdays.Entry e){return e.month+":"+e.day+":"+e.leap+":"+e.lead+":"+e.hour+":"+e.minute+":"+e.mode+":"+e.repeat;}
    static void cancel(Context c,Birthdays.Entry e){
        c.getSystemService(AlarmManager.class).cancel(pending(c,e,0));
        state(c).edit().remove(e.id+".time").remove(e.id+".active").apply();
        c.getSystemService(NotificationManager.class).cancel(e.id,1);AlarmService.stop(c,e.id);
    }
    static void schedule(Context c){
        channels(c);SharedPreferences s=state(c);long now=System.currentTimeMillis();
        for(Birthdays.Entry e:Birthdays.load(c)){
            if(!e.enabled){cancel(c,e);continue;}
            String sig=signature(e);
            if(!sig.equals(s.getString(e.id+".signature",""))){
                cancel(c,e);s.edit().putString(e.id+".signature",sig).apply();
                String birthKey=e.month+":"+e.day+":"+e.leap;
                if(!birthKey.equals(s.getString(e.id+".birthKey","")))s.edit().putString(e.id+".birthKey",birthKey).remove(e.id+".done").apply();
            }
            String zone=ZoneId.systemDefault().getId();
            if(!zone.equals(s.getString(e.id+".zone",""))){
                if(!s.getBoolean(e.id+".snooze",false))s.edit().remove(e.id+".time").apply();
                s.edit().putString(e.id+".zone",zone).apply();
            }
            long time=s.getLong(e.id+".time",0);String date=s.getString(e.id+".date","");
            // Preserve an already scheduled event when returning to the app or rebooting.
            // Catch up only on its civil day; never ring yesterday's alarm.
            boolean valid=time>0 && !date.equals(s.getString(e.id+".done","")) &&
                !Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate().isBefore(LocalDate.now());
            if(!valid){
                ReminderPlan.Event event=ReminderPlan.next(e,ZonedDateTime.now(),s.getString(e.id+".done",""));
                time=event.time;date=event.birthday.toString();
                s.edit().putLong(e.id+".time",time).putString(e.id+".date",date).putBoolean(e.id+".preview",event.preview).putBoolean(e.id+".snooze",false).apply();
            }
            AlarmManager am=c.getSystemService(AlarmManager.class);PendingIntent pi=pending(c,e,time);am.cancel(pi);
            boolean precise=e.mode>0 && exact(c);s.edit().putBoolean(e.id+".exact",precise).apply();
            long trigger=Math.max(time,now+1000);
            if(precise){try{am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pi);}catch(SecurityException denied){s.edit().putBoolean(e.id+".exact",false).apply();am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pi);}}
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pi);
        }
    }
    static PendingIntent action(Context c,Birthdays.Entry e,String date,String action){
        Intent i=new Intent(c,ReminderReceiver.class).setAction(action).setData(Uri.parse("birthday://"+e.id+"/"+action+"/"+date)).putExtra("id",e.id).putExtra("date",date);
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static Notification notification(Context c,Birthdays.Entry e,String date,boolean preview,boolean ringing,boolean test){
        PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        long days=java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(),LocalDate.parse(date));
        String body=test?"这是一条测试通知。":(days==0?"今天":days+" 天后")+"是"+e.name+"的生日 · "+e.label();
        if(ringing)body+="\n正在持续响铃 · 10 分钟后自动停止";
        else if(e.mode==2 && !preview)body+="\n未持续响铃时，请检查闹钟权限及系统限制。";
        Notification.Builder b=new Notification.Builder(c,ringing?ALARM:e.mode==0?LIGHT:IMPORTANT)
            .setSmallIcon(R.drawable.ic_moon).setContentTitle(test?"岁岁 · 测试提醒":e.name+"的生日提醒")
            .setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(open)
            .setCategory(ringing?Notification.CATEGORY_ALARM:Notification.CATEGORY_REMINDER).setAutoCancel(!ringing).setOngoing(ringing);
        if(Build.VERSION.SDK_INT>=31)b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        if(!test){
            b.addAction(new Notification.Action.Builder(null,"10 分钟后",action(c,e,date,"SNOOZE")).build());
            b.addAction(new Notification.Action.Builder(null,"已送祝福",action(c,e,date,"DONE")).build());
            if(ringing)b.addAction(new Notification.Action.Builder(null,"停止响铃",action(c,e,date,"STOP")).build());
        }
        return b.build();
    }
    static boolean allowed(Context c){return c.getSystemService(NotificationManager.class).areNotificationsEnabled();}
    static void notify(Context c,Birthdays.Entry e,boolean test){
        channels(c);if(allowed(c))c.getSystemService(NotificationManager.class).notify(e.id,1,notification(c,e,Birthdays.next(e,LocalDate.now()).toString(),false,false,test));
    }
    static void deliver(Context c,Birthdays.Entry e,String date,boolean preview,boolean precise){
        if(!allowed(c))return;
        state(c).edit().putString(e.id+".active",date).apply();
        if(e.mode==2 && !preview && precise){
            try{c.startForegroundService(new Intent(c,AlarmService.class).putExtra("id",e.id).putExtra("date",date));return;}
            catch(IllegalStateException|SecurityException ignored){/* Keep an actionable notification if background playback is restricted. */}
        }
        c.getSystemService(NotificationManager.class).notify(e.id,1,notification(c,e,date,preview,false,false));
    }
    static boolean handleAction(Context c,Birthdays.Entry e,String date,String action){
        SharedPreferences s=state(c);
        if(!e.enabled || date==null || !date.equals(s.getString(e.id+".active","")) || date.equals(s.getString(e.id+".done","")))return false;
        if(!action.equals("DONE")&&!action.equals("STOP")&&!action.equals("SNOOZE"))return false;
        AlarmService.stop(c,e.id);c.getSystemService(NotificationManager.class).cancel(e.id,1);
        if(action.equals("DONE")){
            c.getSystemService(AlarmManager.class).cancel(pending(c,e,0));
            s.edit().putString(e.id+".done",date).remove(e.id+".time").remove(e.id+".active").apply();
        }else if(action.equals("SNOOZE")){
            ZonedDateTime next=ZonedDateTime.now().plusMinutes(10);
            if(!next.toLocalDate().isAfter(LocalDate.parse(date)))s.edit().putLong(e.id+".time",next.toInstant().toEpochMilli()).putString(e.id+".date",date).putBoolean(e.id+".preview",next.toLocalDate().isBefore(LocalDate.parse(date))).putBoolean(e.id+".snooze",true).apply();
            else android.widget.Toast.makeText(c,"已停止：今天已结束，不再延后提醒",android.widget.Toast.LENGTH_LONG).show();
        }
        schedule(c);return true;
    }
}
