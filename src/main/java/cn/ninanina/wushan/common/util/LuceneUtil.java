package cn.ninanina.wushan.common.util;

import cn.ninanina.wushan.common.Constant;
import lombok.SneakyThrows;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;

public class LuceneUtil {
    private static final LuceneUtil instance = new LuceneUtil();

    private final Analyzer analyzer = new IKAnalyzer();
    private final FSDirectory directory;
    private final FSDirectory tagIndexDirectory;
    private IndexReader indexReader;
    private IndexReader tagIndexReader;
    private IndexSearcher indexSearcher;
    private IndexSearcher tagIndexSearcher;
    private IndexWriter indexWriter;
    private IndexWriter tagIndexWriter;

    @SneakyThrows
    private LuceneUtil() {
        directory = FSDirectory.open(new File(Constant.INDEX_DIR));
        tagIndexDirectory = FSDirectory.open(new File(Constant.TAG_INDEX_DIR));
    }

    public static LuceneUtil get() {
        return instance;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public FSDirectory getDirectory() {
        return directory;
    }

    public void setIndexReader(IndexReader indexReader) {
        this.indexReader = indexReader;
    }

    public synchronized IndexSearcher getIndexSearcher() throws IOException {
        if (indexSearcher == null) {
            indexReader = DirectoryReader.open(directory);
            indexSearcher = new IndexSearcher(indexReader);
        }
        return indexSearcher;
    }

    public void setIndexSearcher(IndexSearcher indexSearcher) {
        this.indexSearcher = indexSearcher;
    }

    public synchronized IndexWriter getIndexWriter() throws IOException {
        if (indexWriter == null) {
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_4, analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            indexWriter = new IndexWriter(directory, config);
        }
        return indexWriter;
    }

    public synchronized IndexSearcher getTagIndexSearcher() throws IOException {
        if (tagIndexSearcher == null) {
            tagIndexReader = DirectoryReader.open(tagIndexDirectory);
            tagIndexSearcher = new IndexSearcher(tagIndexReader);
        }
        return tagIndexSearcher;
    }

    public synchronized IndexWriter getTagIndexWriter() throws IOException {
        if (tagIndexWriter == null) {
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_4, analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            tagIndexWriter = new IndexWriter(tagIndexDirectory, config);
        }
        return tagIndexWriter;
    }

}
