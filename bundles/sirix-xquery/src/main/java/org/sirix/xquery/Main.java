package org.sirix.xquery;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Random;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.XQuery;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.sirix.access.Databases;
import org.sirix.access.conf.SessionConfiguration;
import org.sirix.api.Database;
import org.sirix.api.NodeReadTrx;
import org.sirix.api.Session;
import org.sirix.axis.DescendantAxis;
import org.sirix.axis.NestedAxis;
import org.sirix.axis.concurrent.ConcurrentUnionAxis;
import org.sirix.axis.filter.ElementFilter;
import org.sirix.axis.filter.FilterAxis;
import org.sirix.axis.filter.NameFilter;
import org.sirix.axis.filter.PredicateFilterAxis;
import org.sirix.axis.filter.ValueFilter;
import org.sirix.exception.SirixException;
import org.sirix.service.xml.serialize.XMLSerializer;
import org.sirix.xquery.node.DBNode;
import org.sirix.xquery.node.DBStore;
import org.sirix.xquery.node.DBStore.Updating;

/**
 * A few examples (some taken from the official brackit examples).
 * 
 * @author Johannes Lichtenberger
 * 
 */
public class Main {

	/** User home directory. */
	private static final String USER_HOME = System.getProperty("user.home");

	/** Storage for databases: Sirix data in home directory. */
	private static final File LOCATION = new File(USER_HOME, "sirix-data");

	enum Severity {
		low, high, critical
	};

	/**
	 * Main method.
	 * 
	 * @param args
	 *          not used
	 */
	public static void main(final String[] args) throws SirixException {
		try {
			loadDocumentAndQuery();
			System.out.println();
			loadDocumentAndUpdate();
			System.out.println();
			loadCollectionAndQuery();
			System.out.println();
			loadDocumentAndQueryTemporal();
		} catch (IOException e) {
			System.err.print("I/O error: ");
			System.err.println(e.getMessage());
		} catch (QueryException e) {
			System.err.print("XQuery error ");
			System.err.print(e.getCode());
			System.err.print(": ");
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Load a document and query it.
	 */
	private static void loadDocumentAndQuery() throws QueryException,
			IOException, SirixException {
		// prepare sample document
		// File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		// File doc = generateSampleDoc(tmpDir, "sample", 0);
		// doc.deleteOnExit();
		final File doc = new File(new StringBuilder("src")
				.append(File.separator).append("test").append(File.separator)
				.append("resources").append(File.separator).append("test.xml").toString());

		// Initialize query context and store.
		final DBStore store = new DBStore();
		QueryContext ctx = new QueryContext(store);

		// Use XQuery to load sample document into store.
		System.out.println("Loading document:");
		String xq1 = String.format("bit:load('mydoc.xml', '%s')", doc);
		System.out.println(xq1);
		new XQuery(xq1).evaluate(ctx);

		try (final Database database = Databases.openDatabase(new File(
				new StringBuilder(3).append(LOCATION).append(File.separator)
						.append("mydoc.xml").toString()))) {
			final Session session = database.getSession(SessionConfiguration.builder(
					"shredded").build());
			final NodeReadTrx rtx = session.beginNodeReadTrx();
			final NodeReadTrx firstPredicateRtx = session.beginNodeReadTrx();
			final NodeReadTrx secondPredicateRtx = session.beginNodeReadTrx();

			// Not exactly the same as the next "real" query.
			for (final long nodeKey : new NestedAxis(new FilterAxis(
					new DescendantAxis(rtx), new ElementFilter(rtx), new NameFilter(rtx,
							"nachricht")), new ConcurrentUnionAxis(rtx,
					new PredicateFilterAxis(firstPredicateRtx, new FilterAxis(
							new DescendantAxis(firstPredicateRtx), new ValueFilter(
									firstPredicateRtx, "sommer"))), new PredicateFilterAxis(
							secondPredicateRtx, new FilterAxis(new DescendantAxis(
									secondPredicateRtx), new ValueFilter(secondPredicateRtx,
									"strand")))))) {
				final OutputStream out = new ByteArrayOutputStream();
				XMLSerializer.builder(session, out).startNodeKey(nodeKey)
						.doIndend(true).setDeclaration(false).build().call();
				System.out.println(out.toString());
			}

			// Reuse store and query loaded document.
			final QueryContext ctx2 = new QueryContext(store);
			System.out.println();
			System.out.println("Query loaded document:");
			final String xq2 = "doc('mydoc.xml')/nachrichten/nachricht[betreff/text()='sommer' or betreff/text()='strand' or text/text()='sommer' or text/text()='strand']";
			System.out.println(xq2);
			XQuery query = new XQuery(xq2);
			query.setPrettyPrint(true).serialize(ctx2, System.out);
			final Sequence result = query.evaluate(ctx2);
			final Iter iterator = result.iterate();
			Item item;
			while ((item = iterator.next()) != null) {
				final DBNode node = (DBNode) item;
				final OutputStream out = new ByteArrayOutputStream();
				XMLSerializer.builder(session, out).startNodeKey(node.getNodeKey())
						.doIndend(true).setDeclaration(false).build().call();
				System.out.println(out.toString());
			}

			try (final PrintStream out = new PrintStream(new FileOutputStream(
					new File(new StringBuilder(LOCATION.getAbsolutePath())
							.append(File.separator).append("output.xml").toString())))) {
				String xq3 = String.format("bit:serialize(doc('mydoc.xml'))");
				query = new XQuery(xq3);
				query.setPrettyPrint(true).serialize(ctx2, out);
			}
		}

		System.out.println();
		store.close();
	}

	/**
	 * Load a document and update it.
	 */
	private static void loadDocumentAndUpdate() throws QueryException,
			IOException {
		// Prepare sample document.
		final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		final File doc = generateSampleDoc(tmpDir, "sample");
		doc.deleteOnExit();

		// Initialize query context and store.
		try (final DBStore store = new DBStore(Updating.YES)) {
			final QueryContext ctx = new QueryContext(store);

			// Use XQuery to load sample document into store.
			System.out.println("Loading document:");
			final String xq1 = String.format("bit:load('mydoc.xml', '%s')", doc);
			System.out.println(xq1);
			new XQuery(xq1).evaluate(ctx);

			// Reuse store and query loaded document.
			final QueryContext ctx2 = new QueryContext(store);
			System.out.println();
			System.out.println("Query loaded document:");
			final String xq2 = "insert nodes <a><b/></a> into doc('mydoc.xml')/log";
			System.out.println(xq2);
			new XQuery(xq2).execute(ctx2);
			store.commitAll();
			System.out.println();
		}
	}

	/**
	 * Load a collection and query it.
	 */
	private static void loadCollectionAndQuery() throws QueryException,
			IOException {
		// Prepare directory with sample documents.
		File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		File dir = new File(tmpDir + File.separator + "docs"
				+ System.currentTimeMillis());
		if (!dir.mkdir()) {
			throw new IOException("Directory " + dir + " already exists");
		}
		dir.deleteOnExit();
		for (int i = 0; i < 10; i++) {
			generateSampleDoc(dir, "sample");
		}

		// Initialize query context and store.
		try (final DBStore store = new DBStore()) {
			final QueryContext ctx = new QueryContext(store);

			// Use XQuery to load all sample documents into store.
			System.out.println("Load collection from files:");
			final String xq1 = String.format(
					"bit:load('mydocs.col', io:ls('%s', '\\.xml$'))", dir);
			System.out.println(xq1);
			new XQuery(xq1).evaluate(ctx);

			// Reuse store and query loaded collection.
			final QueryContext ctx2 = new QueryContext(store);
			System.out.println();
			System.out.println("Query loaded collection:");
			final String xq2 = "for $log in collection('mydocs.col')/log\n"
					+ "where $log/@severity='critical'\n" + "return\n" + "<message>\n"
					+ "  <from>{$log/src/text()}</from>\n"
					+ "  <body>{$log/msg/text()}</body>\n" + "</message>\n";
			System.out.println(xq2);
			final XQuery q = new XQuery(xq2);
			q.setPrettyPrint(true);
			q.serialize(ctx2, System.out);
			System.out.println();
			
			// Use XQuery to load all sample documents once more into store.
			System.out.println("Load collection from files:");
			final String xq3 = String.format(
					"bit:load('mydocs.col', io:ls('%s', '\\.xml$'), fn:false())", dir);
			System.out.println(xq3);
			new XQuery(xq3).evaluate(ctx);
		}
	}

	/**
	 * Load a document and query it (temporal).
	 */
	private static void loadDocumentAndQueryTemporal() throws QueryException,
			IOException {
		// Prepare sample document.
		File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		File doc = generateSampleDoc(tmpDir, "sample");
		doc.deleteOnExit();

		// Initialize query context and store.
		try (final DBStore store = new DBStore(Updating.YES)) {
			final QueryContext ctx = new QueryContext(store);

			// Use XQuery to load sample document into store.
			System.out.println("Loading document:");
			final String xq1 = String.format("bit:load('mydoc.xml', '%s')", doc);
			System.out.println(xq1);
			new XQuery(xq1).evaluate(ctx);

			// Reuse store and query loaded document.
			final QueryContext ctx2 = new QueryContext(store);
			System.out.println();
			System.out.println("Query loaded document:");
			final String xq2 = "insert nodes <a><b/></a> into doc('mydoc.xml')/log";
			System.out.println(xq2);
			// final Sequence seq = new XQuery(xq2).evaluate(ctx2);
			new XQuery(xq2).execute(ctx2);
			store.commitAll();
			System.out.println();
		}
		try (final DBStore store = new DBStore()) {
			final QueryContext ctx3 = new QueryContext(store);
			System.out.println();
			System.out.println("Query loaded document:");
			final String xq3 = "doc('mydoc.xml', 0)/log/all-time::*/*";
			System.out.println(xq3);
			XQuery q = new XQuery(xq3);
			q.setPrettyPrint(true);
			q.serialize(ctx3, System.out);

			final QueryContext ctx4 = new QueryContext(store);
			final String xq4 = "bit:serialize(doc('mydoc.xml', 0))";
			q = new XQuery(xq4);
			try (final PrintStream out = new PrintStream(new FileOutputStream(
					new File(new StringBuilder(LOCATION.getAbsolutePath())
							.append(File.separator).append("output-revision-0.xml")
							.toString())))) {
				q.setPrettyPrint(true).serialize(ctx4, out);
			}
			System.out.println();
			final QueryContext ctx5 = new QueryContext(store);
			final String xq5 = "bit:serialize(doc('mydoc.xml', 1))";
			q = new XQuery(xq5);
			try (final PrintStream out = new PrintStream(new FileOutputStream(
					new File(new StringBuilder(LOCATION.getAbsolutePath())
							.append(File.separator).append(File.separator)
							.append("output-revision-1.xml").toString())))) {
				q.setPrettyPrint(true).serialize(ctx5, out);
			}
			System.out.println();
		}
	}

	/**
	 * Generate a small sample document.
	 * 
	 * @param dir
	 *          the directory
	 * @param prefix
	 *          prefix of name to use
	 * @return the generated file
	 * @throws IOException
	 *           if any I/O exception occured
	 */
	private static File generateSampleDoc(final File dir, final String prefix) throws IOException {
		File file = File.createTempFile(prefix, ".xml", dir);
		file.deleteOnExit();
		PrintStream out = new PrintStream(new FileOutputStream(file));
		Random rnd = new Random();
		long now = System.currentTimeMillis();
		int diff = rnd.nextInt(6000 * 60 * 24 * 7);
		Date tst = new Date(now - diff);
		Severity sev = Severity.values()[rnd.nextInt(3)];
		String src = "192.168." + (1 + rnd.nextInt(254)) + "."
				+ (1 + rnd.nextInt(254));
		int mlen = 10 + rnd.nextInt(70);
		byte[] bytes = new byte[mlen];
		int i = 0;
		while (i < mlen) {
			int wlen = 1 + rnd.nextInt(8);
			int j = i;
			while (j < Math.min(i + wlen, mlen)) {
				bytes[j++] = (byte) ('a' + rnd.nextInt('z' - 'a' + 1));
			}
			i = j;
			if (i < mlen - 1) {
				bytes[i++] = ' ';
			}
		}
		String msg = new String(bytes);
		out.print("<?xml version='1.0'?>");
		out.print(String.format("<log tstamp='%s' severity='%s'>", tst, sev));
		out.print(String.format("<src>%s</src>", src));
		out.print(String.format("<msg>%s</msg>", msg));
		out.print("</log>");
		out.close();
		return file;
	}

}