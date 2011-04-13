var TEMP_STORE = 'temp';
var TEMP_ROOT = 'root';
var words = ['the', 'quick', 'brown', 'fox', 'jumps', 'over', 'the', 'lazy', 'dog'];

function getRoot(){
	return search.luceneSearch('workspace://' + TEMP_STORE, '@cm\\\:name:"' + TEMP_ROOT + '"')[0];
}

function generateContent(node){
	var nodeName = getSpecialFileName(node);
	return words.reduce(function(previousValue, currentValue, index, array){
		return (index == 1 )?
				nodeName + previousValue + ' ' + nodeName + currentValue
				: previousValue + ' ' + nodeName + currentValue;
	});
}

function squishuuid(uuid){
	return uuid.replaceAll('-','');
}

function getSpecialFileName(node){
	return squishuuid(node.name).replace('.txt','');
}


// For 1.8 compatibility
if (!Array.prototype.reduce)
{
  Array.prototype.reduce = function(fun /*, initialValue */)
  {
    "use strict";

    if (this === void 0 || this === null)
      throw new TypeError();

    var t = Object(this);
    var len = t.length >>> 0;
    if (typeof fun !== "function")
      throw new TypeError();

    // no value to return if no initial value and an empty array
    if (len == 0 && arguments.length == 1)
      throw new TypeError();

    var k = 0;
    var accumulator;
    if (arguments.length >= 2)
    {
      accumulator = arguments[1];
    }
    else
    {
      do
      {
        if (k in t)
        {
          accumulator = t[k++];
          break;
        }

        // if array contains no values, no initial value to return
        if (++k >= len)
          throw new TypeError();
      }
      while (true);
    }

    while (k < len)
    {
      if (k in t)
        accumulator = fun.call(undefined, accumulator, t[k], k, t);
      k++;
    }

    return accumulator;
  };
}