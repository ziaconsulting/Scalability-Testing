<import resource="classpath:alfresco/extension/scripts/config.js">

var root = getRoot();
var folder;

if(root.children.length == 0){
	folder = root.createFolder(Packages.java.util.UUID.randomUUID().toString());
} else{
	folder = root.children[root.children.length - 1];
}

if(folder.children > 100){
	folder = root.createFolder(Packages.java.util.UUID.randomUUID().toString());
}

var file = folder.createFile(Packages.java.util.UUID.randomUUID().toString() + '.txt');
file.content = generateContent(file);
folder.name