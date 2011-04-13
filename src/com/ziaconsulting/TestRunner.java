package com.ziaconsulting;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.domain.node.Transaction;
import org.alfresco.repo.node.index.AbstractReindexComponent.InIndex;
import org.alfresco.repo.node.index.IndexTransactionTracker;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.springframework.context.Lifecycle;

import com.ziaconsulting.alfresco.AlfUtil;

public class TestRunner implements Lifecycle {

	private static File output = new File("/opt/alfresco/testrun.tsv");
	private static File log = new File("/opt/alfresco/testrun.log");
	private static StoreRef[] targetStores = { TestUtils.tempStore };
	private static Map<StoreRef, Long> storeSize = new HashMap<StoreRef, Long>();
	private static long batchSize = 10000;
	// private static long batchSize = 1000000;
	private static int maxFolderSize = 1000;

	// This should give 1,000,000,000 nodes with no folders more than 1000
	private static int maxDepth = 3;

	private FileWriter logWriter = null;
	private FileWriter outputWriter = null;

	private NodeService ns = AlfUtil.services().getNodeService();
	private FileFolderService ffs = AlfUtil.services().getFileFolderService();
	private RetryingTransactionHelper txn = AlfUtil.services()
			.getTransactionService().getRetryingTransactionHelper();

	private Map<String, Long> starttimes = new HashMap<String, Long>();
	private Map<String, Long> stoptimes = new HashMap<String, Long>();

	private NodeRef root = null;

	public TestRunner() {

		try {
			if (!log.exists()) {
				log.createNewFile();
			}

			if (!output.exists()) {
				output.createNewFile();
			}
			logWriter = new FileWriter(log);
			outputWriter = new FileWriter(output);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		for (StoreRef s : targetStores) {
			storeSize.put(s, 0l);
		}
	}

	public void run() {
		AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>() {
			@Override
			public Object doWork() throws Exception {
				runElevated();
				return null;
			}
		}, AuthenticationUtil.getSystemUserName());

	}

	private void runElevated() {
		// Check to see if the target store exist
		writeLog("Start");

		final StoreRef s = getTarget();
		if (!ns.exists(s)) {
			txn.doInTransaction(new RetryingTransactionCallback<Object>() {

				@Override
				public Object execute() throws Throwable {
					ns.createStore(s.getProtocol(), s.getIdentifier());
					writeLog("Created store: %s", s.toString());
					return null;
				}
			}, false, true);
		}

		final NodeRef root = TestUtils.getRoot();

		for (int i = 1; i < 3; i++) {
			FileInfo batchFolder = txn.doInTransaction(
					new RetryingTransactionCallback<FileInfo>() {

						@Override
						public FileInfo execute() throws Throwable {
							return ffs.create(root, UUID.randomUUID()
									.toString(), ContentModel.TYPE_FOLDER);
						}
					}, false, true);

			starttime("createBatch");
			createBatch(batchFolder.getNodeRef());
			stoptime("createBatch");
			storeSize.put(s, storeSize.get(s) + batchSize);
			writeLog("Created %s nodes in %s sec, store size is %s", batchSize,
					gettime("createBatch"), storeSize.get(s));

			while (!isIndexerDone()) {
				try {
					writeLog("Indexer isn't done");
					Thread.sleep(1000);
				} catch (Exception e) {
					throw new RuntimeException("Sleep interrupted");
				}
			}

			starttime("search");
			testSearch();
			stoptime("search");
			writeOutput(starttimes.get("search"), stoptimes.get("search"), s);

			// snapShotInstance();
			// writeLog("Snapshotted instance");
		}

		writeLog("Done");
	}

	private void createBatch(final NodeRef batchFolder) {
		for (int i = 0; i < batchSize / maxFolderSize; i++) {
			NodeRef newFolder = txn.doInTransaction(
					new RetryingTransactionCallback<NodeRef>() {

						@Override
						public NodeRef execute() throws Throwable {
							return ffs.create(batchFolder,
									UUID.randomUUID().toString(),
									ContentModel.TYPE_FOLDER).getNodeRef();
						}
					}, false, true);

			starttime("newFolder");
			fillFolderWithNodes(newFolder);
			stoptime("newFolder");
			writeLog("Filled folder %s in %s ms", newFolder.toString(),
					gettime("newFolder"));
		}
	}

	private Integer fillFolderWithNodes(final NodeRef folder) {

		return AlfUtil.services().getTransactionService()
				.getRetryingTransactionHelper()
				.doInTransaction(new RetryingTransactionCallback<Integer>() {
					@Override
					public Integer execute() throws Throwable {
						for (int i = 0; i < maxFolderSize; i++) {
							FileInfo file = ffs.create(folder, UUID
									.randomUUID().toString(),
									ContentModel.TYPE_CONTENT);

							ContentWriter cw = AlfUtil
									.services()
									.getContentService()
									.getWriter(file.getNodeRef(),
											ContentModel.PROP_CONTENT, true);
							String s = TestUtils.generateContent(file);
							cw.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
							cw.putContent(s);
						}
						return maxFolderSize;
					}
				}, false, true);
	}

	private boolean isIndexerDone() {
		NodeDAO nodedao = (NodeDAO) AlfUtil.getSpringBean("nodeDAO");
		long now = System.currentTimeMillis();

		List<Transaction> txns = nodedao.getTxnsByCommitTimeDescending(0l, now,
				1, null, false);

		if (txns.size() == 0) {
			return true;
		} else {
			IndexTransactionTracker itt = (IndexTransactionTracker) AlfUtil
					.getSpringBean("admIndexTrackerComponent");

			return itt.isTxnPresentInIndex(txns.get(0)).equals(InIndex.YES);
		}
	}

	private void testSearch() {

	}

	private void snapShotInstance() {

	}

	private StoreRef getTarget() {
		return targetStores[0];
	}

	private void writeLog(String msg, Object... args) {
		try {
			String s = String.format("%tF %<tR::%s", Calendar.getInstance(),
					String.format(msg, args));
			logWriter.append(s).append("\n");
			System.out.println(s);
			logWriter.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void writeOutput(long start, long stop, StoreRef s) {
		StringBuffer sb = new StringBuffer();
		sb.append(start);
		sb.append("\t");
		sb.append(stop);
		sb.append("\t");
		sb.append(s.toString());
		sb.append("\t");
		sb.append(storeSize.get(sb));
		sb.append("\n");
		try {
			outputWriter.append(sb.toString());
			outputWriter.flush();
		} catch (IOException e) {
			throw new RuntimeException("Can't write to file");
		}
	}

	@Override
	public void start() {
		// run();
	}

	@Override
	public void stop() {

	}

	public void starttime(String name) {
		starttimes.put(name, System.currentTimeMillis());
	}

	public void stoptime(String name) {
		stoptimes.put(name, System.currentTimeMillis());
	}

	public long gettime(String name) {
		return stoptimes.get(name) - starttimes.get(name);
	}

	@Override
	public boolean isRunning() {
		return false;
	}

}
