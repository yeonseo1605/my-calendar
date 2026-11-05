const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const source=fs.readFileSync(require('node:path').join(__dirname,'../app.js'),'utf8');
const listeners={},selects=Array.from({length:2},()=>({scrollTop:0,querySelectorAll(){return[]}})),panel={style:{},innerHTML:'',remove(){},querySelector(){return{}},querySelectorAll(selector){return selector==='.time-wheel'?selects:[]}};
const context={document:{addEventListener(type,fn){listeners[type]=fn},createElement(){return panel},body:{append(){}}},window:{addEventListener(){}},innerWidth:1000,innerHeight:900,parseDate:s=>new Date(s+'T00:00:00'),dateStr:d=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`,Date,Event:class{}};
vm.createContext(context);vm.runInContext(source.slice(source.indexOf('function enableWholeTimePickers(){'),source.indexOf('const HOLIDAY_COLOR')),context);context.enableWholeTimePickers();
const input={type:'date',value:'2026-11-05',getBoundingClientRect(){return{left:10,bottom:30}},closest(){return this}};
listeners.click({target:input});assert(panel.innerHTML.includes('2026년 11월'));
input.value='2025-02-15';listeners.input({target:input});assert(panel.innerHTML.includes('2025년 2월'));assert(panel.innerHTML.includes('data-date="2025-02-15" class="selected"'));assert(!panel.innerHTML.includes('2025-02-29'));
input.value='';listeners.input({target:input});assert(panel.innerHTML.includes('2025년 2월'));
listeners.keydown({key:'Escape'});input.type='time';input.value='09:00';listeners.click({target:input});input.value='14:37';listeners.input({target:input});assert.equal(selects[0].scrollTop,14*40);assert.equal(selects[1].scrollTop,37*40);
console.log('PASS: typed year/month/day updates visible month and selection; incomplete input tolerated; typed hours/minutes update time picker');

listeners.keydown({key:'Escape'});input.type='text';input.value='2025-11-05';input.selectionStart=0;input.matches=()=>true;input.setSelectionRange=(start,end)=>{input.selectionStart=start;input.selectionEnd=end};input.dispatchEvent=()=>{};
for(const key of '2026')listeners.keydown({key,target:input,preventDefault(){}});
assert.equal(input.value,'2026-11-05');assert.equal(input.selectionStart,5);
for(const key of '12')listeners.keydown({key,target:input,preventDefault(){}});
assert.equal(input.value,'2026-12-05');assert.equal(input.selectionStart,8);
console.log('PASS: four-digit year advances to month; two-digit month advances to day; scroll wheels track typed time');
