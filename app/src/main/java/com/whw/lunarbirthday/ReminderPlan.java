package com.whw.lunarbirthday;

import java.time.*;
import java.util.*;

/** One persisted event per person, with a birthday occurrence key. */
final class ReminderPlan {
    static final class Event {
        final long time;
        final LocalDate birthday;
        final boolean preview;
        Event(long time, LocalDate birthday, boolean preview) {
            this.time=time; this.birthday=birthday; this.preview=preview;
        }
    }
    static Event next(Birthdays.Entry e, ZonedDateTime now, String completed) {
        LocalDate from=now.toLocalDate();
        for(int year=0;year<4;year++) {
            LocalDate birthday=Birthdays.next(e,from);
            if(!birthday.toString().equals(completed)) {
                List<ZonedDateTime> times=new ArrayList<>();
                ZonedDateTime day=birthday.atTime(e.hour,e.minute).atZone(now.getZone());
                if(e.lead>0) times.add(day.minusDays(e.lead));
                times.add(day);
                if(e.mode>0 && e.repeat>0) for(int n=1;n<=2;n++) {
                    ZonedDateTime repeat=day.plusMinutes((long)e.repeat*n);
                    if(repeat.toLocalDate().equals(birthday)) times.add(repeat);
                }
                for(ZonedDateTime t:times) if(t.isAfter(now))
                    return new Event(t.toInstant().toEpochMilli(),birthday,t.toLocalDate().isBefore(birthday));
            }
            from=birthday.plusDays(1);
        }
        throw new IllegalStateException("无法安排提醒");
    }
}
