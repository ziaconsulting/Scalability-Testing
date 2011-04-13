package com.ziaconsulting;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;

import com.ziaconsulting.alfresco.AlfUtil;

public class TestUtils {
	static public StoreRef tempStore = new StoreRef("workspace", "temp");
	static final private String[] dictionary = { "the", "quick", "brown",
			"fox", "jumps", "over", "the", "lazy", "dog" };

	public static NodeRef getRoot() {
		NodeRef root = null;

		ResultSet rs = AlfUtil
				.services()
				.getSearchService()
				.query(tempStore, SearchService.LANGUAGE_FTS_ALFRESCO,
						"name:\"root\"");

		List<NodeRef> results = rs.getNodeRefs();
		rs.close();
		if (results.size() == 0) {
			final NodeRef storeRoot = AlfUtil.ns().getRootNode(tempStore);
			root = AlfUtil
					.services()
					.getTransactionService()
					.getRetryingTransactionHelper()
					.doInTransaction(
							new RetryingTransactionCallback<NodeRef>() {

								@Override
								public NodeRef execute() throws Throwable {
									NodeRef n = AlfUtil.ns().createNode(storeRoot,
											ContentModel.ASSOC_CHILDREN,
											QName.createQName("cm", "root", AlfUtil.services().getNamespaceService()),
											ContentModel.TYPE_FOLDER).getChildRef();
									AlfUtil.ns().setProperty(n, ContentModel.PROP_NAME, "root");
									return n;
								}
							}, false, true);
			if(!AlfUtil.ns().exists(root)){
				throw new RuntimeException("Root not created");
			}
		} else {
			root = results.get(0);
		}

		return root;
	}

	public static String generateContent(FileInfo f) {
		StringBuffer sb = new StringBuffer();
		String name = f.getName().replace("-", "");

		for (String word : dictionary) {
			if (sb.toString().length() != 0) {
				sb.append(" ");
			}
			sb.append(name);
			sb.append(word);
		}

		return sb.toString();
	}
}
