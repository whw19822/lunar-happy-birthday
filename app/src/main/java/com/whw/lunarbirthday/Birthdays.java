package com.whw.lunarbirthday;

import android.content.Context;
import com.nlf.calendar.Lunar;
import com.nlf.calendar.LunarMonth;
import com.nlf.calendar.Solar;
import org.json.*;
import java.time.*;
import java.util.*;

final class Birthdays {
    static final String[] MONTHS = {"正月","二月","三月","四月","五月","六月","七月","八月","九月","十月","冬月","腊月"};
    static final String[] DAYS = {"初一","初二","初三","初四","初五","初六","初七","初八","初九","初十","十一","十二","十三","十四","十五","十六","十七","十八","十九","二十","廿一","廿二","廿三","廿四","廿五","廿六","廿七","廿八","廿九","三十"};
    static class Entry {
        String id = UUID.randomUUID().toString(), name = "";
        int month = 1, day = 1, lead = 0, hour = 9, minute = 0, mode = 1, repeat = 60;
        boolean leap = false, enabled = true;
        String label() { return "农历 " + (leap ? "闰" : "") + MONTHS[month-1] + DAYS[day-1]; }
        JSONObject json() throws JSONException { return new JSONObject().put("id",id).put("name",name).put("month",month).put("day",day).put("lead",lead).put("hour",hour).put("minute",minute).put("leap",leap).put("enabled",enabled).put("mode",mode).put("repeat",repeat); }
    }
    static List<Entry> load(Context c) {
        List<Entry> result = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(c.getSharedPreferences("birthdays",0).getString("entries","[]"));
            for (int i=0;i<a.length();i++) {
                JSONObject o=a.getJSONObject(i); Entry e=new Entry(); e.id=o.getString("id"); e.name=o.getString("name"); e.month=o.getInt("month"); e.day=o.getInt("day"); e.lead=o.optInt("lead",0); e.hour=o.optInt("hour",9); e.minute=o.optInt("minute",0); e.leap=o.optBoolean("leap"); e.enabled=o.optBoolean("enabled",true); e.mode=o.optInt("mode",1); e.repeat=o.optInt("repeat",60); result.add(e);
            }
        } catch(JSONException ex) { throw new IllegalStateException("生日数据读取失败",ex); }
        return result;
    }
    static void save(Context c,List<Entry> entries) {
        JSONArray a=new JSONArray(); try { for(Entry e:entries) a.put(e.json()); } catch(JSONException ex) { throw new IllegalStateException(ex); }
        c.getSharedPreferences("birthdays",0).edit().putString("entries",a.toString()).apply();
        Reminders.schedule(c);
    }
    static Lunar lunar(LocalDate date) {
        return Solar.fromYmd(date.getYear(),date.getMonthValue(),date.getDayOfMonth()).getLunar();
    }
    static LocalDate inYear(Entry e,int year) {
        int month=e.leap?-e.month:e.month;
        LunarMonth lm=LunarMonth.fromYm(year,month);
        if(lm==null){month=e.month;lm=LunarMonth.fromYm(year,month);}
        Solar solar=Lunar.fromYmd(year,month,Math.min(e.day,lm.getDayCount())).getSolar();
        return LocalDate.of(solar.getYear(),solar.getMonth(),solar.getDay());
    }
    static LocalDate next(Entry e,LocalDate from) {
        int year=lunar(from).getYear();
        for(int y=year;y<year+4;y++) { LocalDate d=inYear(e,y); if(!d.isBefore(from)) return d; }
        throw new IllegalStateException("无法计算生日");
    }
}
