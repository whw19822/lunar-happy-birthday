package com.whw.lunarbirthday;
import android.content.*;
import java.time.*;
public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        for(Birthdays.Entry e:Birthdays.load(c))if(e.id.equals(i.getStringExtra("id"))){
            if(i.getAction()!=null){Reminders.handleAction(c,e,i.getStringExtra("date"),i.getAction());return;}
            SharedPreferences s=Reminders.state(c);long due=i.getLongExtra("time",0),now=System.currentTimeMillis();
            if(!e.enabled || due<=0 || due!=s.getLong(e.id+".time",0) || due>now)return;
            String date=s.getString(e.id+".date","");
            boolean preview=s.getBoolean(e.id+".preview",false),precise=s.getBoolean(e.id+".exact",false);
            s.edit().remove(e.id+".time").apply();
            if(!date.equals(s.getString(e.id+".done","")) && Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalDate().equals(LocalDate.now()))Reminders.deliver(c,e,date,preview,precise);
            Reminders.schedule(c);return;
        }
    }
}
