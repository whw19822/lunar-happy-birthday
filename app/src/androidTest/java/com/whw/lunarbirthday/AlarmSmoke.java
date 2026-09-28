package com.whw.lunarbirthday;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import java.time.*;
import java.util.*;

/** Manual emulator smoke fixture. Never included in the application APK. */
public class AlarmSmoke {
    static Bundle run(Context c, String mode){SharedPreferences backup=c.getSharedPreferences("smoke_backup",0);Bundle result=new Bundle();
        try{
            if("cleanup".equals(mode)){
                Birthdays.Entry e=new Birthdays.Entry();e.id="alarm-smoke";Reminders.cancel(c,e);
                String saved=backup.getString("entries",null);if(saved!=null)c.getSharedPreferences("birthdays",0).edit().putString("entries",saved).commit();
                backup.edit().clear().commit();Reminders.schedule(c);result.putString("stream","Smoke fixture restored\n");
            }else{
                if(!Reminders.exact(c))throw new IllegalStateException("Enable alarm permission first");
                if(!backup.contains("entries"))backup.edit().putString("entries",c.getSharedPreferences("birthdays",0).getString("entries","[]")).commit();
                Birthdays.Entry e=new Birthdays.Entry();e.id="alarm-smoke";e.name="强提醒验证";e.mode=2;e.repeat=60;
                com.nlf.calendar.Lunar lunar=Birthdays.lunar(LocalDate.now());e.month=Math.abs(lunar.getMonth());e.leap=lunar.getMonth()<0;e.day=lunar.getDay();e.hour=LocalTime.now().getHour();e.minute=LocalTime.now().getMinute();
                List<Birthdays.Entry> all=Birthdays.load(c);all.removeIf(b->b.id.equals(e.id));all.add(e);Birthdays.save(c,all);
                Reminders.state(c).edit().remove(e.id+".done").remove(e.id+".active").putLong(e.id+".time",System.currentTimeMillis()+8000).putString(e.id+".date",LocalDate.now().toString()).putBoolean(e.id+".preview",false).commit();Reminders.schedule(c);
                result.putString("stream","Strong alarm scheduled in 8 seconds\n");
            }
            return result;
        }catch(Throwable t){result.putString("stream","FAIL: "+t);return result;}
    }
}
