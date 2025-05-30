package cn.edu.bistu.cs.ir.index;

import cn.edu.bistu.cs.ir.crawler.SinaBlogCrawler;
import cn.edu.bistu.cs.ir.model.Blog;
import org.apache.lucene.document.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import java.util.List;

/**
 * 基于Lucene的WebMagic Pipeline,
 * 用于将抓取的数据写入本地的Lucene索引
 * @author ruoyuchen
 */
public class LucenePipeline implements Pipeline {

    private static final Logger log = LoggerFactory.getLogger(LucenePipeline.class);

    private final IdxService idxService;
    public LucenePipeline(IdxService idxService){
        log.info("初始化LucenePipeline模块");
        this.idxService = idxService;
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        Blog blog = resultItems.get(SinaBlogCrawler.RESULT_ITEM_KEY);
        if(blog==null){
            log.error("无法从爬取的结果中提取到Blog对象");
            return;
        }
        String id = blog.getId();
        Document doc = toDoc(blog);
        boolean result = idxService.addDocument("ID", id, doc);
        if(!result){
            log.error("无法将ID为[{}]的博客内容写入索引", id);
        }
    }

    private Document toDoc(Blog blog){
        Document document = new Document();
        //页面ID
        document.add(new StringField("ID", blog.getId(), Field.Store.YES));
        //页面标题
        document.add(new TextField("TITLE", blog.getTitle(), Field.Store.YES));
        //页面内容全文
        document.add(new TextField("CONTENT", blog.getContent(), Field.Store.YES));
        //TODO 下面请同学们补充其他的待检索字段，如发布时间、标签、作者等，并思考应该选择什么字段类型
        // 添加作者字段
        document.add(new StringField("AUTHOR", blog.getAuthor(), Field.Store.YES));

        // 处理时间字段（直接使用Blog类的date字段）
        long publishTime = blog.getDate(); // 👈 使用getDate()方法
        document.add(new LongPoint("PUBLISH_TIME", publishTime));
        document.add(new StoredField("PUBLISH_TIME_STORE", publishTime));

        // 处理标签（转换为逗号分隔的字符串）
        List<String> tags = blog.getTags();
        if (tags != null && !tags.isEmpty()) {
            String tagStr = String.join(",", tags);
            document.add(new TextField("TAGS", tagStr, Field.Store.YES));
        }

        // 添加URL字段
        document.add(new StringField("URL", blog.getUrl(), Field.Store.YES));

        return document;
    }}
