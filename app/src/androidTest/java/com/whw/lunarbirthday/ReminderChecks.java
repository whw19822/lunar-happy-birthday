package com.whw.lunarbirthday;

import android.content.*;
import android.app.*;
import java.time.*;
import java.util.*;

final class ReminderChecks {
    static int count;
    static void check(boolean ok,String why){count++;if(!ok)throw new AssertionError(why);}
    static int run(Context c){
        count=0;Birthdays.Entry e=new Birthdays.Entry();e.month=1;e.day=1;e.lead=7;e.hour=9;e.mode=1;e.repeat=60;
        ZonedDateTime before=ZonedDateTime.of(2026,2,9,12,0,0,0,ZoneId.of("Asia/Shanghai"));
        ReminderPlan.Event pre=ReminderPlan.next(e,before,"");
        check(pre.preview && Instant.ofEpochMilli(pre.time).atZone(before.getZone()).toLocalDate().equals(LocalDate.of(2026,2,10)),"advance preview");
        ReminderPlan.Event day=ReminderPlan.next(e,before.plusDays(2),"");
        check(!day.preview && day.birthday.equals(LocalDate.of(2026,2,17)),"birthday still scheduled after preview");
        ZonedDateTime morning=before.withDayOfMonth(17).withHour(9).withMinute(1);
        check(Instant.ofEpochMilli(ReminderPlan.next(e,morning,"").time).atZone(before.getZone()).getHour()==10,"first repeat");
        check(Instant.ofEpochMilli(ReminderPlan.next(e,morning.plusHours(1),"").time).atZone(before.getZone()).getHour()==11,"second repeat");
        check(ReminderPlan.next(e,morning.plusHours(2),"").birthday.getYear()==2027,"two repeats maximum");
        check(ReminderPlan.next(e,morning,"2026-02-17").birthday.getYear()==2027,"completion skips occurrence");
        e.hour=23;e.minute=50;check(ReminderPlan.next(e,morning.withHour(23).withMinute(51),"").birthday.getYear()==2027,"repeats never cross midnight");
        e.hour=9;e.minute=0;e.mode=0;check(ReminderPlan.next(e,morning,"").birthday.getYear()==2027,"light mode never repeats");
        e.mode=2;e.repeat=0;check(ReminderPlan.next(e,morning,"").birthday.getYear()==2027,"strong mode honors no repeats");
        e.mode=1;e.repeat=60;
        List<Birthdays.Entry> original=Birthdays.load(c);List<Birthdays.Entry> all=new ArrayList<>(original);all.add(e);
        try{
            Birthdays.save(c,all);SharedPreferences s=Reminders.state(c);
            String date=Birthdays.next(e,LocalDate.now()).toString();
            s.edit().putString(e.id+".active",date).apply();
            check(Reminders.handleAction(c,e,date,"SNOOZE"),"snooze accepted");
            long snooze=s.getLong(e.id+".time",0);
            check(Math.abs(snooze-System.currentTimeMillis()-600000)<3000,"snooze is ten minutes");
            Reminders.schedule(c);check(s.getLong(e.id+".time",0)==snooze,"snooze persists across rescheduling");
            check(!Reminders.handleAction(c,e,"2000-01-01","DONE"),"stale action rejected");
            check(Reminders.handleAction(c,e,date,"DONE"),"completion accepted");
            check(s.getString(e.id+".done","").equals(date),"completion stored");
            check(!s.getString(e.id+".date","").equals(date),"completion cancels snooze and plans next year");
            check(!Reminders.handleAction(c,e,date,"SNOOZE"),"completed notification cannot snooze");
            e.enabled=false;Birthdays.save(c,all);check(s.getLong(e.id+".time",0)==0,"disabled reminder cancelled");
            e.enabled=true;Birthdays.save(c,all);check(!s.getString(e.id+".date","").equals(date),"re-enabling preserves completion");
            check(Birthdays.load(c).get(all.size()-1).mode==1,"mode persistence");
        }finally{
            Reminders.cancel(c,e);Birthdays.save(c,original);
            SharedPreferences.Editor editor=Reminders.state(c).edit();for(String key:Reminders.state(c).getAll().keySet())if(key.startsWith(e.id+"."))editor.remove(key);editor.apply();
        }
        return count;
    }
}
