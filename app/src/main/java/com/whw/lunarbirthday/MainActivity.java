package com.whw.lunarbirthday;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class MainActivity extends Activity {
    final int BG=Color.rgb(248,245,238), INK=Color.rgb(39,57,47), GREEN=Color.rgb(56,99,79), MUTED=Color.rgb(115,119,108);
    LinearLayout page; java.util.List<Birthdays.Entry> entries;
    int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(3),1);return t;}
    GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    void gap(LinearLayout l,int n){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,dp(n)));}
    void bold(TextView t){t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);}
    Button button(String title){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextColor(GREEN);return b;}
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);}
    @Override public void onResume(){super.onResume();Reminders.schedule(this);render();}
    String date(LocalDate d){return d.format(DateTimeFormatter.ofPattern("yyyy年M月d日 · E",Locale.CHINA));}
    void render(){
        entries=Birthdays.load(this);entries.sort(Comparator.comparing(e->Birthdays.next(e,LocalDate.now())));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        page=column();page.setPadding(dp(24),dp(20),dp(24),dp(24));scroll.addView(page);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(i.left,i.top,i.right,i.bottom);}else{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());}return insets;});
        setContentView(scroll);
        TextView brand=text("岁 岁  /  LUNAR BIRTHDAY",12,GREEN);bold(brand);page.addView(brand);gap(page,18);
        TextView title=text("把重要的人，\n记在心上。",32,INK);bold(title);page.addView(title);gap(page,10);
        page.addView(text("农历的日子，每年都替你记得。",14,MUTED));gap(page,24);
        LinearLayout hero=column();hero.setPadding(dp(22),dp(22),dp(22),dp(22));hero.setBackground(shape(GREEN,24));page.addView(hero);
        hero.addView(text(entries.isEmpty()?"☾  心意，从一个生日开始":"☾  下一份值得期待的心意",13,0xffe3ddc3));gap(hero,16);
        if(entries.isEmpty()) {TextView h=text("年年有今日\n岁岁有欢喜",28,Color.WHITE);bold(h);hero.addView(h);gap(hero,12);hero.addView(text("添加亲友的农历生日，让牵挂如期而至。",14,0xffe3ddc3));}
        else {Birthdays.Entry e=entries.get(0);LocalDate d=Birthdays.next(e,LocalDate.now());long days=ChronoUnit.DAYS.between(LocalDate.now(),d);TextView h=text(e.name,30,Color.WHITE);bold(h);hero.addView(h);gap(hero,8);hero.addView(text(days==0?"就是今天 · 生日快乐": "还有 "+days+" 天",22,0xfff4dda2));gap(hero,14);hero.addView(text(e.label()+"\n"+date(d),14,0xffe3ddc3));}
        gap(page,18);Button add=button("＋  添加农历生日");add.setTextColor(Color.WHITE);add.setBackground(shape(GREEN,16));page.addView(add,new LinearLayout.LayoutParams(-1,dp(54)));add.setOnClickListener(v->edit(null));gap(page,22);
        LinearLayout row=new LinearLayout(this);TextView heading=text("生日簿",20,INK);bold(heading);row.addView(heading,new LinearLayout.LayoutParams(0,-2,1));row.addView(text(entries.size()+" 位重要的人",13,MUTED));page.addView(row);gap(page,12);
        if(entries.isEmpty()) {page.addView(text("还没有记录\n点上方按钮，记下第一个重要日子。",15,MUTED));gap(page,16);}
        for(Birthdays.Entry e:entries) {
            LinearLayout card=column();card.setPadding(dp(18),dp(16),dp(18),dp(16));card.setBackground(shape(Color.WHITE,18));page.addView(card);gap(page,10);
            LinearLayout top=new LinearLayout(this);TextView name=text(e.name,20,INK);bold(name);top.addView(name,new LinearLayout.LayoutParams(0,-2,1));Switch enabled=new Switch(this);enabled.setChecked(e.enabled);enabled.setContentDescription(e.name+"的生日提醒");top.addView(enabled);card.addView(top);
            enabled.setOnCheckedChangeListener((b,on)->{e.enabled=on;Birthdays.save(this,entries);if(on)requestNotifications();});
            gap(card,7);card.addView(text(e.label()+"  ·  "+date(Birthdays.next(e,LocalDate.now())),13,MUTED));gap(card,8);
            card.addView(text((e.lead==0?"当天": "提前 "+e.lead+" 天")+String.format(Locale.CHINA," %02d:%02d",e.hour,e.minute)+" · "+Reminders.MODES[e.mode]+" · 点击编辑",12,GREEN));
            String active=Reminders.state(this).getString(e.id+".active","");
            String done=Reminders.state(this).getString(e.id+".done","");
            if(done.equals(Birthdays.next(e,LocalDate.now()).toString()))card.addView(text("✓ 已送祝福 · 本次后续提醒已停止",13,GREEN));
            if(!active.isEmpty() && !LocalDate.parse(active).isBefore(LocalDate.now())){
                LinearLayout actions=new LinearLayout(this);Button later=button("10 分钟后"),sent=button("已送祝福");actions.addView(later,new LinearLayout.LayoutParams(0,-2,1));actions.addView(sent,new LinearLayout.LayoutParams(0,-2,1));card.addView(actions);
                later.setOnClickListener(v->{Reminders.handleAction(this,e,active,"SNOOZE");render();});sent.setOnClickListener(v->{Reminders.handleAction(this,e,active,"DONE");render();});
                if(AlarmService.ringing(e.id)){Button stop=button("停止响铃");card.addView(stop);stop.setOnClickListener(v->{Reminders.handleAction(this,e,active,"STOP");render();});}
            }
            card.setOnClickListener(v->edit(e));
        }
        gap(page,16);TextView settings=text("提醒设置  →",16,GREEN);bold(settings);page.addView(settings);settings.setPadding(0,dp(8),0,dp(8));settings.setOnClickListener(v->settings());
        boolean allowed=getSystemService(NotificationManager.class).areNotificationsEnabled();page.addView(text(allowed?"通知已开启 · 生日仅保存在本机":"通知未开启 · 点击提醒设置开启通知",12,MUTED));gap(page,16);
        page.addView(text(Reminders.exact(this)?"闹钟权限已开启":"闹钟权限未开启 · 强提醒暂以通知提示",12,MUTED));
        page.addView(text("闰月生日：无对应闰月时按普通月份过生日。\n农历三十：遇小月按廿九过生日。",12,MUTED));
    }
    void requestNotifications(){if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);render();}
    Spinner spinner(String[] items){Spinner s=new Spinner(this);ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items);s.setAdapter(a);return s;}
    void edit(Birthdays.Entry old){
        Birthdays.Entry e=new Birthdays.Entry();if(old!=null){e.id=old.id;e.name=old.name;e.month=old.month;e.day=old.day;e.leap=old.leap;e.lead=old.lead;e.hour=old.hour;e.minute=old.minute;e.enabled=old.enabled;e.mode=old.mode;e.repeat=old.repeat;}
        LinearLayout form=column();form.setPadding(dp(22),dp(8),dp(22),dp(16));ScrollView sc=new ScrollView(this);sc.addView(form);
        form.addView(text("称呼",13,MUTED));EditText name=new EditText(this);name.setSingleLine(true);name.setHint("例如：妈妈");name.setText(e.name);name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(30)});form.addView(name);gap(form,14);
        form.addView(text("农历生日",13,MUTED));LinearLayout dates=new LinearLayout(this);Spinner month=spinner(Birthdays.MONTHS),day=spinner(Birthdays.DAYS);month.setSelection(e.month-1);day.setSelection(e.day-1);dates.addView(month,new LinearLayout.LayoutParams(0,dp(52),1));dates.addView(day,new LinearLayout.LayoutParams(0,dp(52),1));form.addView(dates);
        CheckBox leap=new CheckBox(this);leap.setText("这是闰月生日");leap.setChecked(e.leap);form.addView(leap);form.addView(text("无对应闰月按普通月；三十遇小月按廿九。",12,MUTED));gap(form,18);
        form.addView(text("提醒力度",13,MUTED));Spinner mode=spinner(Reminders.MODES);mode.setSelection(e.mode);form.addView(mode);
        form.addView(text("轻提醒：安静通知。重要提醒：声音和振动。强提醒：生日当天持续响铃，最长 10 分钟。",12,MUTED));gap(form,10);
        form.addView(text("当天未确认时（最多追加两次）",13,MUTED));Spinner repeat=spinner(new String[]{"不重复","每隔 1 小时","每隔 3 小时"});repeat.setSelection(e.repeat==0?0:e.repeat==60?1:2);form.addView(repeat);
        mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int position,long id){repeat.setEnabled(position>0);}public void onNothingSelected(AdapterView<?> p){}});
        form.addView(text("什么时候提醒",13,MUTED));int[] leads={0,1,3,7,14};Spinner lead=spinner(new String[]{"生日当天","提前 1 天","提前 3 天","提前 7 天","提前 14 天"});for(int i=0;i<leads.length;i++)if(leads[i]==e.lead)lead.setSelection(i);form.addView(lead);form.addView(text("选择提前提醒后，生日当天仍会提醒。",12,MUTED));gap(form,10);
        Button time=button(String.format(Locale.CHINA,"提醒时间   %02d:%02d",e.hour,e.minute));form.addView(time);time.setOnClickListener(v->new TimePickerDialog(this,(view,h,m)->{e.hour=h;e.minute=m;time.setText(String.format(Locale.CHINA,"提醒时间   %02d:%02d",h,m));},e.hour,e.minute,true).show());
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(old==null?"记下一个生日":"编辑生日").setView(sc).setNegativeButton("取消",null).setPositiveButton("保存",null).setNeutralButton(old==null?null:"删除",null).create();
        dialog.setOnShowListener(v->{dialog.getButton(-1).setOnClickListener(w->{if(name.getText().toString().trim().isEmpty()){name.setError("请填写称呼");return;}e.name=name.getText().toString().trim();e.month=month.getSelectedItemPosition()+1;e.day=day.getSelectedItemPosition()+1;e.leap=leap.isChecked();e.lead=leads[lead.getSelectedItemPosition()];e.mode=mode.getSelectedItemPosition();e.repeat=new int[]{0,60,180}[repeat.getSelectedItemPosition()];if(old!=null)entries.remove(old);entries.add(e);Birthdays.save(this,entries);dialog.dismiss();render();if(e.enabled){requestNotifications();if(e.mode==2&&!Reminders.exact(this))Toast.makeText(this,"请到提醒设置开启闹钟权限，才能持续响铃",Toast.LENGTH_LONG).show();}});if(old!=null)dialog.getButton(-3).setOnClickListener(w->new AlertDialog.Builder(this).setTitle("删除"+old.name+"的生日？").setMessage("删除后将同时取消这条生日提醒。").setNegativeButton("保留",null).setPositiveButton("删除",(d,which)->{Reminders.cancel(this,old);entries.remove(old);Birthdays.save(this,entries);dialog.dismiss();render();}).show());});dialog.show();
    }
    void settings(){
        new AlertDialog.Builder(this).setTitle("提醒设置").setItems(new String[]{"通知权限与声音设置","闹钟权限 · "+(Reminders.exact(this)?"已开启":"未开启"),"发送测试通知","了解提醒规则"},(d,w)->{
            if(w==0)startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
            if(w==1){if(Build.VERSION.SDK_INT>=31)startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,android.net.Uri.parse("package:"+getPackageName())));else Toast.makeText(this,"此版本安卓无需额外闹钟权限",Toast.LENGTH_SHORT).show();}
            if(w==2){if(!Reminders.allowed(this)){requestNotifications();Toast.makeText(this,"请先开启通知权限",Toast.LENGTH_SHORT).show();}else{Reminders.notify(this,new Birthdays.Entry(),true);Toast.makeText(this,"测试通知已发送",Toast.LENGTH_SHORT).show();}}
            if(w==3)new AlertDialog.Builder(this).setTitle("让心意如期抵达").setMessage("重要提醒默认开启：提前预告、当天提醒；未确认时按设置最多追加两次，不跨到第二天。\n\n已送祝福会停止本次生日的后续提醒，明年自动恢复。10 分钟后会暂停当前提醒；停止响铃只停止当前声音。\n\n强提醒使用闹钟音量，最长响铃 10 分钟；需开启通知和闹钟权限。权限不足时只能通知提示。静音、免打扰和厂商后台管理仍可能影响送达，不会强行修改手机音量。\n\n提醒按手机本地时间安排。重启后自动恢复；强行停止后需重新打开。数据仅在本机，卸载会清除。").setPositiveButton("知道了",null).show();
        }).setNegativeButton("关闭",null).show();
    }
}
