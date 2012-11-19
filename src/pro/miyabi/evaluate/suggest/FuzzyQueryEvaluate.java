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
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import pro.miyabi.evaluate.suggest.util.LoggerUtil;

/**
 * @author OGINO, Tadashi <tadashi.ogino@syncthought.com>
 *
 */
public class FuzzyQueryEvaluate {
	
	private static final Logger LOGGER = LogManager.getLogger(FuzzyQueryEvaluate.class);
	private static final Version VERSION = Version.LUCENE_40;

	private IndexWriter indexWriter; 

	/**
	 * @throws IOException
	 */
	public FuzzyQueryEvaluate() throws IOException {
		super();
		this.indexWriter = new IndexWriter(new RAMDirectory(), new IndexWriterConfig(VERSION, new JapaneseAnalyzer(VERSION)));
	}
	
	/**
	 * @param directory
	 * @param analyzer
	 * @throws IOException 
	 */
	public FuzzyQueryEvaluate(final Directory directory, final Analyzer analyzer) throws IOException {
		super();
		this.indexWriter = new IndexWriter(directory, new IndexWriterConfig(VERSION, analyzer));
	}

	/**
	 * @param indexWriter
	 */
	public FuzzyQueryEvaluate(final IndexWriter indexWriter) {
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

	public List<String> find(final String text, final String field) {
		try {
			List<String> suggestList = new ArrayList<String>();
			IndexReader reader = DirectoryReader.open(this.indexWriter.getDirectory());
			FuzzyQuery fuzzyQuery = new FuzzyQuery(new Term(field, text));
			BooleanQuery query = (BooleanQuery) fuzzyQuery.rewrite(reader);
			for (BooleanClause clause : query.clauses()) {
				Term term = ((TermQuery) clause.getQuery()).getTerm();
				if (term == null) continue;
				String suggest =  term.text();
				if (!StringUtils.isEmpty(suggest)) suggestList.add(suggest);
			}
			return suggestList;
		} catch (final IOException e) {
			LoggerUtil.logException(LOGGER, e);
		}
		return null;
	}
}
