const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const source=fs.readFileSync(require('node:path').join(__dirname,'../app.js'),'utf8');
const name={style:{},scrollWidth:44,getBoundingClientRect(){return{width:this.scrollWidth}} },date={getBoundingClientRect:()=>({width:45})},line={clientWidth:70,querySelector:selector=>selector==='.holiday-date-label'?name:selector==='.day-number'?date:null},grid={querySelectorAll:()=>[line]};
const context={$:()=>grid,window:{},holidayNameObserver:null};vm.createContext(context);vm.runInContext(source.split('\n').find(line=>line.startsWith('function fitHolidayNames(')),context);
context.fitHolidayNames();assert.equal(name.style.display,'none');line.clientWidth=110;context.fitHolidayNames();assert.equal(name.style.display,'block');line.clientWidth=70;context.fitHolidayNames();assert.equal(name.style.display,'none');line.clientWidth=110;name.scrollWidth=80;context.fitHolidayNames();assert.equal(name.style.display,'none');
console.log('PASS: narrow hides; unfolded width shows; folding hides again; long names hide when they would overlap lunar dates');
