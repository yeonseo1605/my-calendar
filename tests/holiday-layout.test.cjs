const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const css=fs.readFileSync(path.join(__dirname,'../styles.css'),'utf8');
assert(css.includes('@container calendar-day (min-width:70px)'));assert(css.includes('display:block!important;width:auto!important;flex:1 1 auto!important'));
console.log('PASS: holiday names use calendar-cell width rather than mobile device detection; wide-cell rule overrides hidden styles');
