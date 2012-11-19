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
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import pro.miyabi.evaluate.suggest.util.LoggerUtil;

/**
 * @author OGINO, Tadashi <tadashi.ogino@syncthought.com>
 *
 */
@RunWith(JMock.class)
public class SpellCheckerEvaluateTest {
	
	private static final Logger LOGGER = LogManager.getLogger(SpellCheckerEvaluateTest.class);
	
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
	 * Test method for {@link pro.miyabi.evaluate.suggest.SpellCheckerEvaluate#SpellCheckerEvaluate()}.
	 */
	@Test
	public void testSpellCheckerEvaluate() {
		try {
			SpellCheckerEvaluate evaluate = new SpellCheckerEvaluate();
			assertThat(evaluate, notNullValue());
		} catch (final IOException e) {
			LoggerUtil.logException(LOGGER, e);
			fail();
		}
	}

	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.SpellCheckerEvaluate#SpellCheckerEvaluate(org.apache.lucene.store.Directory, org.apache.lucene.analysis.Analyzer)}.
	 * @throws IOException 
	 */
	@Test
	public void testSpellCheckerEvaluateDirectoryAnalyzer() throws IOException {
		try {
			SpellCheckerEvaluate evaluate = new SpellCheckerEvaluate(new RAMDirectory(), new StandardAnalyzer(Version.LUCENE_40));
			assertThat(evaluate, notNullValue());
		} catch (final IOException e) {
			LoggerUtil.logException(LOGGER, e);
			fail();
		}
	}
	
	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.SpellCheckerEvaluate#SpellCheckerEvaluate(org.apache.lucene.store.Directory, org.apache.lucene.analysis.Analyzer)}.
	 * @throws IOException 
	 */
	@Test
	public void testSpellCheckerEvaluateDirectoryAnalyzer_Exception() throws IOException {
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
			new SpellCheckerEvaluate(directory, analyzer);
			fail();
		} catch (final IOException e) {
			LoggerUtil.logException(LOGGER, e);
			assertThat(e.getMessage(), notNullValue());
		}
	}

	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.SpellCheckerEvaluate#SpellCheckerEvaluate(org.apache.lucene.index.IndexWriter)}.
	 */
	@Test
	public void testSpellCheckerEvaluateIndexWriter() {
		final IndexWriter indexWriter = context.mock(IndexWriter.class, "testSpellCheckerEvaluateIndexWriter#IndexWriter");
		SpellCheckerEvaluate evaluate = new SpellCheckerEvaluate(indexWriter);
		assertThat(evaluate, notNullValue());
	}

	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.SpellCheckerEvaluate#createIndex(java.lang.String, java.lang.String)}.
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testCreateIndex() throws IOException {
		final IndexWriter indexWriter = context.mock(IndexWriter.class, "testCreateIndex#IndexWriter");
		context.checking(new Expectations(){{
			allowing(indexWriter).addDocument((Iterable<? extends IndexableField>) with(anything()));
			allowing(indexWriter).close();
		}});
		
		SpellCheckerEvaluate evaluate = new SpellCheckerEvaluate(indexWriter);
		boolean success = evaluate.createIndex("志茂", "東京都北区志茂");
		assertThat(success, is(true));
	}

	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.SpellCheckerEvaluate#find(java.lang.String, java.lang.String, float)}.
	 * @throws IOException 
	 */
	@Test
	public void testFind() throws IOException {
		Directory directory = new SimpleFSDirectory(this.dictDir);
		UserDictionary dictionary = new UserDictionary(
				new FileReader(new File("test/pro/miyabi/evaluate/suggest/userdict.txt")));
		Analyzer analyzer = new JapaneseAnalyzer(Version.LUCENE_40,	dictionary, Mode.EXTENDED,
				JapaneseAnalyzer.getDefaultStopSet(), JapaneseAnalyzer.getDefaultStopTags());
		SpellCheckerEvaluate evaluate = new SpellCheckerEvaluate(directory, analyzer);
		InputStream input = new FileInputStream("test/pro/miyabi/evaluate/suggest/kanzo_uchimura.txt");
		boolean success = evaluate.createIndex("field", IOUtils.toString(input));
		assertThat(success, is(true));
		List<String> suggestList = evaluate.find("内邑艦４", "field", 0.25f);
		assertThat(suggestList, notNullValue());
		assertThat(suggestList.size(), greaterThan(0));
		for (String suggest : suggestList) {
			assertThat(suggest, notNullValue());
			assertThat(suggest.length(), greaterThan(0));
			LOGGER.info("testFind - suggest: " + suggest);
		}
	}

	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.SpellCheckerEvaluate#find(java.lang.String, java.lang.String, float)}.
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
		}});
		
		SpellCheckerEvaluate evaluate = new SpellCheckerEvaluate(indexWriter);
		boolean success = evaluate.createIndex("追浜", "神奈川県横須賀市追浜本町");
		assertThat(success, is(true));
		List<String> suggestList = evaluate.find("ネクスト", "aa", 0.1f);
		assertThat(suggestList, notNullValue());
	}

	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.SpellCheckerEvaluate#find(java.lang.String, java.lang.String, float)}.
	 * @throws IOException 
	 */
	@Test
	public void testFind_SingleByteWords() throws IOException {
		SpellCheckerEvaluate evaluate = new SpellCheckerEvaluate();
		InputStream input = new FileInputStream("test/pro/miyabi/evaluate/suggest/mips_news.txt");
		boolean success = evaluate.createIndex("field", IOUtils.toString(input));
		assertThat(success, is(true));
		List<String> suggestList = evaluate.find("Technolojp", "field", 0.7f);
		assertThat(suggestList, notNullValue());
		assertThat(suggestList.size(), greaterThan(0));
		for (String suggest : suggestList) {
			assertThat(suggest, notNullValue());
			assertThat(suggest.length(), greaterThan(0));
			LOGGER.info("testFind_SingleByteWords - suggest: " + suggest);
		}
	}
	
	/**
	 * Test method for {@link pro.miyabi.evaluate.suggest.SpellCheckerEvaluate#find(java.lang.String, java.lang.String, float)}.
	 * @throws IOException 
	 */
	@Test
	public void testFind_WithFileDirectry() throws IOException {
		Directory directory = new SimpleFSDirectory(this.dictDir);
		UserDictionary dictionary = new UserDictionary(
				new FileReader(new File("test/pro/miyabi/evaluate/suggest/userdict.txt")));
		Analyzer analyzer = new JapaneseAnalyzer(Version.LUCENE_40,	dictionary, Mode.EXTENDED,
				JapaneseAnalyzer.getDefaultStopSet(), JapaneseAnalyzer.getDefaultStopTags());
		SpellCheckerEvaluate evaluate = new SpellCheckerEvaluate(directory, analyzer);
		InputStream input = new FileInputStream("test/pro/miyabi/evaluate/suggest/wagahaiwa_nekodearu.txt");
		boolean success = evaluate.createIndex("field", IOUtils.toString(input));
		assertThat(success, is(true));
		List<String> suggestList = evaluate.find("夏日総石", "field", 0.5f);
		assertThat(suggestList, notNullValue());
		assertThat(suggestList.size(), greaterThan(0));
		for (String suggest : suggestList) {
			assertThat(suggest, notNullValue());
			assertThat(suggest.length(), greaterThan(0));
			LOGGER.info("testFind_WithFileDirectry - suggest: " + suggest);
		}
	}
}
