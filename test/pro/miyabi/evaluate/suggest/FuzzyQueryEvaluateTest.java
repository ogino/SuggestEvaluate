/**
 * 
 */
package pro.miyabi.evaluate.suggest;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.analysis.ja.dict.UserDictionary;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import pro.miyabi.evaluate.suggest.util.LoggerUtil;

/**
 * @author OGINO, Tadashi <tadashi.ogino@syncthought.com>
 *
 */
public class FuzzyQueryEvaluateTest {
	
	private static final Logger LOGGER = LogManager.getLogger(FuzzyQueryEvaluateTest.class);

	private Mockery context = new JUnit4Mockery() {{
		setImposteriser(ClassImposteriser.INSTANCE);
	}};

	private File dictDir;

	private static final String dictPath = "dictPath";

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		if (this.dictDir == null) this.dictDir = new File(dictPath);
		FileUtils.forceMkdir(this.dictDir);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		FileUtils.cleanDirectory(this.dictDir);
		FileUtils.deleteDirectory(this.dictDir);
	}

	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.FuzzyQueryEvaluate#FuzzyQueryEvaluate()}.
	 */
	@Test
	public void testFuzzyQueryEvaluate() {
		try {
			FuzzyQueryEvaluate evaluate = new FuzzyQueryEvaluate();
			assertThat(evaluate, notNullValue());
		} catch (final IOException e) {
			LoggerUtil.logException(LOGGER, e);
			fail();
		}
	}

	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.FuzzyQueryEvaluate#FuzzyQueryEvaluate(org.apache.lucene.store.Directory, org.apache.lucene.analysis.Analyzer)}.
	 * @throws IOException 
	 */
	@Test
	public void testFuzzyQueryEvaluateDirectoryAnalyzer_Exception() throws IOException {
		final Directory directory = context.mock(Directory.class, "testFuzzyQueryEvaluateDirectoryAnalyzer#Directry");
		final Analyzer analyzer = context.mock(Analyzer.class, "testFuzzyQueryEvaluateDirectoryAnalyzer#Analyzer");
		final Lock lock = context.mock(Lock.class, "testFuzzyQueryEvaluateDirectoryAnalyzer#Lock");
		context.checking(new Expectations() {{
			allowing(directory).makeLock(with(any(String.class)));
			will(returnValue(lock));
			allowing(lock).obtain(with(any(Long.class)));
			will(returnValue(true));
			allowing(directory).listAll();
			will(throwException(new IOException("例外")));
		}});
		try {
			new FuzzyQueryEvaluate(directory, analyzer);
			fail();
		} catch (final IOException e) {
			LoggerUtil.logException(LOGGER, e);
			assertThat(e.getMessage(), notNullValue());
		}
	}
	
	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.FuzzyQueryEvaluate#FuzzyQueryEvaluate(org.apache.lucene.store.Directory, org.apache.lucene.analysis.Analyzer)}.
	 */
	@Test
	public void testFuzzyQueryEvaluateDirectoryAnalyzer() {
		Directory directory = new RAMDirectory();
		Analyzer analyzer = new JapaneseAnalyzer(Version.LUCENE_40);
		try {
			FuzzyQueryEvaluate evaluate = new FuzzyQueryEvaluate(directory, analyzer);
			assertThat(evaluate, notNullValue());
		} catch (final IOException e) {
			fail();
		}
		
	}

	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.FuzzyQueryEvaluate#FuzzyQueryEvaluate(org.apache.lucene.index.IndexWriter)}.
	 */
	@Test
	public void testFuzzyQueryEvaluateIndexWriter() {
		final IndexWriter writer = context.mock(IndexWriter.class, "testFuzzyQueryEvaluateIndexWriter#IndexWriter");
		FuzzyQueryEvaluate evaluate = new FuzzyQueryEvaluate(writer);
		assertThat(evaluate, notNullValue());
	}

	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.FuzzyQueryEvaluate#createIndex(java.lang.String, java.lang.String)}.
	 * @throws IOException 
	 */
	@Test
	public void testCreateIndex() throws IOException {
		FuzzyQueryEvaluate evaluate = new FuzzyQueryEvaluate();
		boolean success = evaluate.createIndex("TEST", "神奈川県平塚市老松町");
		assertThat(success, is(true));
	}

	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.FuzzyQueryEvaluate#find(java.lang.String, java.lang.String)}.
	 * @throws IOException 
	 */
	@Test
	public void testFind() throws IOException {
		UserDictionary dictionary = new UserDictionary(
				new FileReader(new File("test/pro/miyabi/evaluate/suggest/userdict.txt")));
		Analyzer analyzer = new JapaneseAnalyzer(Version.LUCENE_40,	dictionary, Mode.EXTENDED,
				JapaneseAnalyzer.getDefaultStopSet(), JapaneseAnalyzer.getDefaultStopTags());
		Directory directory = new SimpleFSDirectory(this.dictDir);
		FuzzyQueryEvaluate evaluate = new FuzzyQueryEvaluate(directory, analyzer);
		InputStream input = new FileInputStream("test/pro/miyabi/evaluate/suggest/kanzo_uchimura.txt");
		boolean success = evaluate.createIndex("field", IOUtils.toString(input));
		assertThat(success, is(true));
		List<String> suggestList = evaluate.find("内村鑑六", "field");
		assertThat(suggestList, notNullValue());
		assertThat(suggestList.size(), greaterThan(0));
		for (String suggest : suggestList) {
			assertThat(suggest, notNullValue());
			assertThat(suggest.length(), greaterThan(0));
			LOGGER.info("testFind - suggest: " + suggest);
		}
	}
	
	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.FuzzyQueryEvaluate#find(java.lang.String, java.lang.String)}.
	 * @throws IOException 
	 */
	@Test
	public void testFind_SOSEKI() throws IOException {
		UserDictionary dictionary = new UserDictionary(
				new FileReader(new File("test/pro/miyabi/evaluate/suggest/userdict.txt")));
		Analyzer analyzer = new JapaneseAnalyzer(Version.LUCENE_40,	dictionary, Mode.EXTENDED,
				JapaneseAnalyzer.getDefaultStopSet(), JapaneseAnalyzer.getDefaultStopTags());
		Directory directory = new SimpleFSDirectory(this.dictDir);
		FuzzyQueryEvaluate evaluate = new FuzzyQueryEvaluate(directory, analyzer);
		InputStream input = new FileInputStream("test/pro/miyabi/evaluate/suggest/wagahaiwa_nekodearu.txt");
		boolean success = evaluate.createIndex("field", IOUtils.toString(input));
		assertThat(success, is(true));
		List<String> suggestList = evaluate.find("夏日総石", "field");
		assertThat(suggestList, notNullValue());
		assertThat(suggestList.size(), greaterThan(0));
		for (String suggest : suggestList) {
			assertThat(suggest, notNullValue());
			assertThat(suggest.length(), greaterThan(0));
			LOGGER.info("testFind_SOSEKI - suggest: " + suggest);
		}
	}

	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.FuzzyQueryEvaluate#find(java.lang.String, java.lang.String)}.
	 * @throws IOException 
	 */
	@Test
	public void testFind_WithDefaultDictionary() throws IOException {
		FuzzyQueryEvaluate evaluate = new FuzzyQueryEvaluate();
		InputStream input = new FileInputStream("test/pro/miyabi/evaluate/suggest/kanzo_uchimura.txt");
		boolean success = evaluate.createIndex("field", IOUtils.toString(input));
		assertThat(success, is(true));
		List<String> suggestList = evaluate.find("ネンソあ", "field");
		assertThat(suggestList, notNullValue());
		assertThat(suggestList.size(), greaterThan(0));
		for (String suggest : suggestList) {
			assertThat(suggest, notNullValue());
			assertThat(suggest.length(), greaterThan(0));
			LOGGER.info("testFind_WithDefaultDictionary - suggest: " + suggest);
		}
	}
	
	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.FuzzyQueryEvaluate#find(java.lang.String, java.lang.String)}.
	 * @throws IOException 
	 */
	@Test
	public void testFind_SigleByteWords() throws IOException {
		FuzzyQueryEvaluate evaluate = new FuzzyQueryEvaluate();
		InputStream input = new FileInputStream("test/pro/miyabi/evaluate/suggest/mips_news.txt");
		boolean success = evaluate.createIndex("field", IOUtils.toString(input));
		assertThat(success, is(true));
		List<String> suggestList = evaluate.find("techmolog", "field");
		assertThat(suggestList, notNullValue());
		assertThat(suggestList.size(), greaterThan(0));
		for (String suggest : suggestList) {
			assertThat(suggest, notNullValue());
			assertThat(suggest.length(), greaterThan(0));
			LOGGER.info("testFind_SigleByteWords - suggest: " + suggest);
		}
	}
	
	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.FuzzyQueryEvaluate#find(java.lang.String, java.lang.String)}.
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	@Ignore
	@Test
	public void testFind_Mockery() throws IOException {
		final IndexWriter indexWriter = context.mock(IndexWriter.class, "testFind_Mockery#IndexWriter");
		final Directory directory = context.mock(Directory.class, "testFind_Mockery#Directry");
		context.checking(new Expectations(){{
			allowing(indexWriter).addDocument((Iterable<? extends IndexableField>) with(anything()));
			allowing(indexWriter).close();
			allowing(indexWriter).getDirectory();
			will(returnValue(directory));
			allowing(directory).listAll();
			will(returnValue(null));
		}});
		
		FuzzyQueryEvaluate evaluate = new FuzzyQueryEvaluate(indexWriter);
		boolean success = evaluate.createIndex("field", "TEST");
		assertThat(success, is(true));
		List<String> suggestList = evaluate.find("ネンソあ", "field");
		assertThat(suggestList, notNullValue());
	}
	
	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.FuzzyQueryEvaluate#find(java.lang.String, java.lang.String)}.
	 * @throws IOException 
	 */
	@Test
	public void testFind_Muliply() throws IOException {
		FuzzyQueryEvaluate evaluate = new FuzzyQueryEvaluate();
		InputStream input = new FileInputStream("test/pro/miyabi/evaluate/suggest/kanzo_uchimura.txt");
		boolean success = evaluate.createIndex("field", IOUtils.toString(input));
		assertThat(success, is(true));
		List<String> suggestList = evaluate.find("憂国", "field");
		assertThat(suggestList, notNullValue());
		assertThat(suggestList.size(), greaterThan(0));
		for (String suggest : suggestList) {
			assertThat(suggest, notNullValue());
			assertThat(suggest.length(), greaterThan(0));
			LOGGER.info("testFind_Muliply - suggest: " + suggest);
		}
	}
	
	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.FuzzyQueryEvaluate#find(java.lang.String, java.lang.String)}.
	 * @throws IOException 
	 */
	@Test
	public void testFind_JapaneseMultiByteWords() throws IOException {
		FuzzyQueryEvaluate evaluate = new FuzzyQueryEvaluate();
		InputStream input = new FileInputStream("test/pro/miyabi/evaluate/suggest/wagahaiwa_nekodearu.txt");
		boolean success = evaluate.createIndex("field", IOUtils.toString(input));
		assertThat(success, is(true));
		List<String> suggestList = evaluate.find("漱じ", "field");
		assertThat(suggestList, notNullValue());
		assertThat(suggestList.size(), greaterThan(0));
		for (String suggest : suggestList) {
			assertThat(suggest, notNullValue());
			assertThat(suggest.length(), greaterThan(0));
			LOGGER.info("testFind_JapaneseMultiByteWords - suggest: " + suggest);
		}
	}

	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.FuzzyQueryEvaluate#find(java.lang.String, java.lang.String)}.
	 * @throws IOException 
	 */
	@Test
	public void testFind_WithFileDirectry() throws IOException {
		Directory directory = new SimpleFSDirectory(this.dictDir);
		Analyzer analyzer = new JapaneseAnalyzer(Version.LUCENE_40);
		FuzzyQueryEvaluate evaluate = new FuzzyQueryEvaluate(directory, analyzer);
		InputStream input = new FileInputStream("test/pro/miyabi/evaluate/suggest/kanzo_uchimura.txt");
		boolean success = evaluate.createIndex("field", IOUtils.toString(input));
		assertThat(success, is(true));
		List<String> suggestList = evaluate.find("大海原", "field");
		assertThat(suggestList, notNullValue());
		assertThat(suggestList.size(), greaterThan(0));
		for (String suggest : suggestList) {
			assertThat(suggest, notNullValue());
			assertThat(suggest.length(), greaterThan(0));
			LOGGER.info("testFind_WithFileDirectry - suggest: " + suggest);
		}
	}
}
