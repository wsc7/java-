package cn.edu.bistu.cs.ir.index;

import cn.edu.bistu.cs.ir.config.Config;
import cn.edu.bistu.cs.ir.utils.StringUtil;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 面向<a href="https://lucene.apache.org/">Lucene</a>
 * 索引读、写的服务类
 * @author chenruoyu
 */
@Component
public class IdxService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(IdxService.class);

    private static final Class<? extends Analyzer> DEFAULT_ANALYZER = StandardAnalyzer.class;

    private IndexWriter writer;

    public IdxService(@Autowired Config config) throws Exception {
        Analyzer analyzer = DEFAULT_ANALYZER.getConstructor().newInstance();
        Directory index;
        try {
            index = FSDirectory.open(Paths.get(config.getIdx()));
            IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
            writer = new IndexWriter(index, writerConfig);
            log.info("索引初始化完成，索引目录为:[{}]", config.getIdx());
        } catch (IOException e) {
            e.printStackTrace();
            log.error("无法初始化索引，请检查提供的索引目录是否可用:[{}]", config.getIdx());
            writer = null;
        }
    }

    public boolean addDocument(String idFld, String id, Document doc){
        if(writer==null||doc==null){
            log.error("Writer对象或文档对象为空，无法添加文档到索引中");
            return false;
        }
        if(StringUtil.isEmpty(idFld)||StringUtil.isEmpty(id)){
            log.error("ID字段名或ID字段值为空，无法添加文档到索引中");
            return false;
        }
        try {
            writer.updateDocument(new Term(idFld, id), doc);
            writer.commit();
            log.info("成功将ID为[{}]的文档加入索引", id);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            log.error("构建索引失败");
            return false;
        }
    }

    /**
     * 根据关键词对索引内容进行检索，并将检索结果返回
     * @param kw 待检索的关键词
     * @return 检索得到的文档列表
     */
    public List<Document> queryByKw(String kw, int pageNo, int pageSize) throws Exception {
        DirectoryReader reader = DirectoryReader.open(writer);
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = DEFAULT_ANALYZER.getConstructor().newInstance();
        QueryParser parser = new QueryParser("TITLE", analyzer);
        Query query = parser.parse(kw);

        int start = (pageNo - 1) * pageSize;
        TopDocs topDocs = searcher.search(query, start + pageSize);
        ScoreDoc[] hits = topDocs.scoreDocs;

        List<Document> results = new ArrayList<>();
        int end = Math.min(start + pageSize, hits.length);
        for (int i = start; i < end; i++) {
            results.add(searcher.doc(hits[i].doc));
        }
        return results;
    }

    //TODO 请大家在这里添加更多的检索函数，如针对发表时间的范围检索等，
    // 添加了检索函数后，还需要相应地在Controller中添加接口
    public int getTotalDocumentCount() throws IOException {
        try (DirectoryReader reader = DirectoryReader.open(writer)) {
            return reader.numDocs();
        }
    }
    public List<Document> queryByField(String field, String kw, int pageSize) throws Exception {
        DirectoryReader reader = DirectoryReader.open(writer);
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = DEFAULT_ANALYZER.getConstructor().newInstance();
        QueryParser parser = new QueryParser(field, analyzer);
        Query query = parser.parse(kw);
        TopDocs topDocs = searcher.search(query, pageSize);
        ScoreDoc[] hits = topDocs.scoreDocs;
        List<Document> results = new ArrayList<>();
        for (ScoreDoc doc : hits) {
            results.add(searcher.doc(doc.doc));
        }
        return results;
    }
    public List<Document> queryTitleAndContent(String kw, int pageSize) throws Exception {
        DirectoryReader reader = DirectoryReader.open(writer);
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = DEFAULT_ANALYZER.getConstructor().newInstance();

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        QueryParser titleParser = new QueryParser("TITLE", analyzer);
        Query titleQuery = titleParser.parse(kw);
        builder.add(titleQuery, BooleanClause.Occur.SHOULD);

        QueryParser contentParser = new QueryParser("CONTENT", analyzer);
        Query contentQuery = contentParser.parse(kw);
        builder.add(contentQuery, BooleanClause.Occur.SHOULD);

        BooleanQuery booleanQuery = builder.build();
        TopDocs topDocs = searcher.search(booleanQuery, pageSize);

        List<Document> results = new ArrayList<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            results.add(searcher.doc(scoreDoc.doc));
        }
        return results;
    }
    @Override
    public void destroy(){
        if(this.writer==null){
            return;
        }
        try {
            log.info("索引关闭");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            log.info("尝试关闭索引失败");
        }
    }
}
