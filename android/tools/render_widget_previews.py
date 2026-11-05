"""Render shared, proportional widget chooser/launcher illustration assets."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parents[1] / 'app/src/main/res/drawable-nodpi'
OUT.mkdir(parents=True, exist_ok=True)
WHITE, MUTED = '#f4f3f7', '#b3b4c1'
COLORS = ['#bd75ef', '#6dd35a', '#70a4ff', '#f5ad56']
def font(size, bold=False):
    return ImageFont.truetype('C:/Windows/Fonts/malgun'+('bd' if bold else '')+'.ttf', size)
def label(d, xy, text, size=28, color=WHITE, bold=False, anchor=None):
    d.text(xy, text, font=font(size,bold), fill=color, anchor=anchor)
def surface(w,h):
    im=Image.new('RGBA',(w,h)); d=ImageDraw.Draw(im)
    d.rounded_rectangle((0,0,w-1,h-1),radius=30,fill='#1d1d25')
    return im,d
def event(d,x,y,title,time,color,width):
    d.rounded_rectangle((x,y+4,x+9,y+57),radius=4,fill=color)
    label(d,(x+25,y),title,30,bold=True)
    label(d,(x+width,y+8),time,24,anchor='rt')
def calendar(im,x,y,w,h):
    d=ImageDraw.Draw(im); label(d,(x+w/2,y+8),'2026년 11월',32,bold=True,anchor='mt')
    label(d,(x+20,y+8),'‹',36); label(d,(x+w-34,y+8),'›',36)
    step=w/7; top=y+72; rowh=(h-106)/5
    for c,day in enumerate('일월화수목금토'):
        label(d,(x+step*(c+.5),top),day,22, '#e88a92' if c==0 else '#8fa6de' if c==6 else MUTED,anchor='mt',bold=True)
    sample={3:('회의',0),6:('연습',1),11:('병원',2),14:('약속',3),18:('회의',0),23:('연습',1),27:('연습',1)}
    for day in range(1,31):
        row,c=divmod(day-1,7); cx=x+step*(c+.5); cy=top+43+row*rowh
        if day==18:d.rounded_rectangle((cx-25,cy-3,cx+25,cy+34),radius=12,fill=COLORS[0])
        label(d,(cx,cy),str(day),25, '#e88a92' if c==0 else '#8fa6de' if c==6 else WHITE,anchor='mt',bold=day==18)
        if day in sample:
            title,ci=sample[day]; left=x+c*step+3
            d.rounded_rectangle((left,cy+32,left+step-6,cy+54),radius=5,fill=COLORS[ci])
            label(d,(cx,cy+33),title,16,'#ffffff',True,anchor='mt')

im,d=surface(600,230)
label(d,(28,55),'11월 18일 (수)',22,bold=True);label(d,(28,91),'오늘',44,bold=True)
d.line((235,35,235,194),fill='#4b4b58',width=2)
event(d,260,40,'회의','',COLORS[0],310);label(d,(285,80),'10:00 – 11:00',24)
event(d,260,125,'연습','',COLORS[1],310);label(d,(285,165),'19:00 – 21:00',24)
im.save(OUT/'chooser_today.png')
im,d=surface(600,280);label(d,(300,12),'11월 15일 – 21일',28,bold=True,anchor='mt')
for c,day in enumerate('일월화수목금토'):
    x=20+c*80;label(d,(x+35,62),day,21,MUTED,anchor='mt');label(d,(x+35,98),str(15+c),24,anchor='mt')
    if c in [1,3,5]:
        d.rounded_rectangle((x,143,x+72,222),radius=8,fill=COLORS[c%4]);label(d,(x+36,150),['회의','연습','병원'][c//2],21,anchor='mt',bold=True);label(d,(x+36,183),'10:00',18,anchor='mt')
im.save(OUT/'chooser_week.png')
im,d=surface(600,440);calendar(im,18,10,564,420);im.save(OUT/'chooser_month.png')
im,d=surface(600,440);label(d,(24,22),'11월 18일 (수)',30,bold=True)
d.line((24,76,576,76),fill='#484852',width=2)
for i,(title,time) in enumerate([('회의','10:00 – 11:00'),('병원','13:30 – 14:30'),('연습','19:00 – 21:00'),('약속','21:30 – 22:30')]):event(d,28,100+i*80,title,time,COLORS[i],540)
im.save(OUT/'chooser_agenda.png')
im,d=surface(1200,440);calendar(im,12,10,570,420)
d.line((605,28,605,412),fill='#484852',width=2);label(d,(635,25),'11월 18일 (수) 일정',28,bold=True)
for i,(title,time) in enumerate([('회의','10:00 – 11:00'),('병원','13:30 – 14:30'),('연습','19:00 – 21:00'),('약속','21:30 – 22:30')]):event(d,635,100+i*80,title,time,COLORS[i],535)
im.save(OUT/'chooser_month_agenda.png')
