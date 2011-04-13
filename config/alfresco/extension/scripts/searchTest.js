<import resource="classpath:alfresco/extension/scripts/config.js">

var root = getRoot();

// Pick a random top folder
var folder;
do{
	folder = root.children[ Math.floor(Math.random() * root.children.length)];
} while(folder.length > 0);

var file = folder.children[ Math.floor(Math.random() * folder.children.length)];

// Get a random word combined with the node name for a search
var word = getSpecialFileName(file) + words[Math.floor(Math.random() * words.length)];

search.luceneSearch('workspace://' + TEMP_STORE, 'TEXT:"' + word + '"');