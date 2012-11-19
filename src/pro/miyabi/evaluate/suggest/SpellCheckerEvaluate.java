/**
 * 
 */
package pro.miyabi.evaluate.suggest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ja.JapaneseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.HighFrequencyDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import pro.miyabi.evaluate.suggest.util.LoggerUtil;

/**
 * @author OGINO, Tadashi <tadashi.ogino@syncthought.com>
 *
 */
public class SpellCheckerEvaluate {
	
	private static final Logger LOGGER = LogManager.getLogger(SpellCheckerEvaluate.class);
	private static final Version VERSION = Version.LUCENE_40;
	
	private IndexWriter indexWriter;

	/**
	 * @throws IOException
	 */
	public SpellCheckerEvaluate() throws IOException {
		super();
		this.indexWriter = new IndexWriter(new RAMDirectory(), new IndexWriterConfig(VERSION, new JapaneseAnalyzer(VERSION)));
	}

	/**
	 * @param directory
	 * @param analyzer
	 * @throws IOException
	 */
	public SpellCheckerEvaluate(final Directory directory, final Analyzer analyzer) throws IOException {
		super();
		this.indexWriter = new IndexWriter(directory, new IndexWriterConfig(VERSION, analyzer));
	}

	/**
	 * @param indexWriter
	 */
	public SpellCheckerEvaluate(final IndexWriter indexWriter) {
		super();
		this.indexWriter = indexWriter;
	}

	/**
	 * @param field
	 * @param content
	 * @return
	 */
	public boolean createIndex(final String field, final String content) {
		Document document = new Document();
		FieldType fieldType = new FieldType();
		fieldType.setStored(true);
		fieldType.setIndexed(true);
		document.add(new Field(field, content, fieldType));
		return this.createIndex(document);
	}

	/**
	 * @param document
	 * @return
	 */
	private boolean createIndex(final Document document) {
		try {
			this.indexWriter.addDocument(document);
			this.indexWriter.close();
			return true;
		} catch (final IOException e) {
			LoggerUtil.logException(LOGGER, e);
		}
		return false;
	}

	private static final int SUGGEST_NUMBER = 10;

	/**
	 * @param text
	 * @param field
	 * @param score
	 * @return
	 */
	public List<String> find(final String text, final String field, float score) {
		try {
			List<String> suggestList = new ArrayList<String>();
			IndexReader indexReader = DirectoryReader.open(this.indexWriter.getDirectory());
			SpellChecker spellChecker = new SpellChecker(this.indexWriter.getDirectory());
			spellChecker.indexDictionary(new HighFrequencyDictionary(indexReader, field, score),
					new IndexWriterConfig(VERSION, new JapaneseAnalyzer(VERSION)), true);
			for (final String suggest : spellChecker.suggestSimilar(text, SUGGEST_NUMBER, score)) {
				if (!StringUtils.isEmpty(suggest)) suggestList.add(suggest);
			}
			spellChecker.close();
			return suggestList;
		} catch (final IOException e) {
			LoggerUtil.logException(LOGGER, e);
		}
		return null;
	}
}
