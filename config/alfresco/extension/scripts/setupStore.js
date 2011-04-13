<import resource="classpath:alfresco/extension/scripts/config.js">
var store = new Packages.org.alfresco.service.cmr.repository.StoreRef('workspace', TEMP_STORE);

// Delete the root node
var r = getRoot();
if(r){
	r.remove();
}

var storeRoot = search.luceneSearch('workspace://' + TEMP_STORE, 'PATH:"/"')[0];
r = storeRoot.createNode(TEMP_ROOT, 'cm:folder', '{http://www.alfresco.org/model/system/1.0}children');
r.nodeRef
