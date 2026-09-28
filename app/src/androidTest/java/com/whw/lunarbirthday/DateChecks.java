package com.whw.lunarbirthday;
import android.app.Instrumentation;
import android.os.Bundle;
import com.nlf.calendar.LunarMonth;
import java.time.*;
public class DateChecks extends Instrumentation {
    int checks=0; Bundle args;
    void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    @Override public void onCreate(Bundle args){super.onCreate(args);this.args=args;start();}
    @Override public void onStart(){if(args!=null&&args.containsKey("smoke")){finish(-1,AlarmSmoke.run(getTargetContext(),args.getString("smoke")));return;}Bundle result=new Bundle();try {
        Birthdays.Entry e=new Birthdays.Entry();
        check(Birthdays.next(e,LocalDate.of(2026,1,1)).equals(LocalDate.of(2026,2,17)),"2026 lunar new year");
        check(Birthdays.next(e,LocalDate.of(2026,2,17)).equals(LocalDate.of(2026,2,17)),"birthday today");
        check(Birthdays.next(e,LocalDate.of(2026,2,18)).equals(LocalDate.of(2027,2,6)),"next year: "+Birthdays.next(e,LocalDate.of(2026,2,18)));
        e.month=8;e.day=15;check(Birthdays.next(e,LocalDate.of(2026,1,1)).equals(LocalDate.of(2026,9,25)),"mid autumn");
        e.month=6;e.day=1;e.leap=true;check(Birthdays.next(e,LocalDate.of(2025,1,1)).equals(LocalDate.of(2025,7,25)),"leap sixth month");
        for(int year=2024;year<=2030;year++)for(int month=1;month<=12;month++)for(boolean leap:new boolean[]{false,true}){
            e.month=month;e.leap=leap;e.day=30;LocalDate d=Birthdays.next(e,LocalDate.of(year,1,1));com.nlf.calendar.Lunar c=Birthdays.lunar(d);
            check(Math.abs(c.getMonth())==month,"month fallback "+d);
            check(c.getDay()==LunarMonth.fromYm(c.getYear(),c.getMonth()).getDayCount(),"small month clamp "+d);
        }
        checks+=ReminderChecks.run(getTargetContext());
        result.putString("stream","PASS: "+checks+" date and reminder checks\n");finish(-1,result);
    }catch(Throwable t){result.putString("stream","FAIL: "+t);finish(0,result);}}
}
