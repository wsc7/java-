package cn.edu.bistu.cs.ir.crawler;

import cn.edu.bistu.cs.ir.model.Blog;
import cn.edu.bistu.cs.ir.model.BlogStats;
import cn.edu.bistu.cs.ir.utils.HttpUtils;
import cn.edu.bistu.cs.ir.utils.StringUtil;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 面向博客园（cnblogs.com）的爬虫
 * 例如：<a href="https://www.cnblogs.com/tencent-cloud-native/">腾讯云原生</a>
 * @author chenruoyu
 */
public class CnBlogsCrawler implements PageProcessor {


    private final Site site;

    /**
     * 博主的ID
     */
    private final String bloggerId;

    private static final Logger log = LoggerFactory.getLogger(CnBlogsCrawler.class);

    public static final String RESULT_ITEM_KEY = "BLOG_INFO";

    /**
     * 当前博主的博文目录页URL前缀
     */
    private final String list_prefix;

    /**
     * 当前博主的博文内容页URL前缀
     */
    private final String blog_prefix;


    public CnBlogsCrawler(Site site, String bloggerId) {
        this.site = site;
        this.bloggerId = bloggerId;
        //https://www.cnblogs.com/tencent-cloud-native/default.html?page=1
        //https://www.cnblogs.com/tencent-cloud-native/p/14913423.html
        this.list_prefix = String.format("https://www.cnblogs.com/%s/default.html", bloggerId);
        this.blog_prefix = String.format("https://www.cnblogs.com/%s/p/", bloggerId);
    }

    private final SimpleDateFormat sdf = new SimpleDateFormat("uuuu-MM-dd HH:mm");

    @Override

    public void process(Page page) {
        String url = page.getRequest().getUrl();
        if (url.startsWith(list_prefix)) {
            log.info("解析博客目录页[{}]", url);
            List<String> blogs = page.getHtml()
                    .xpath("//div[@class='forFlow']//div[@class='postTitle']/a/@href")
                    .all();
            log.info("获取博文内容页地址[{}]条", blogs.size());
            page.addTargetRequests(blogs);

            // 处理分页：尝试抓取下一页链接（注意第一页结构特殊）
            String nextPage = page.getHtml().xpath("//div[@id='nav_next']/a/@href").get();
            if (nextPage != null && !nextPage.isEmpty()) {
                page.addTargetRequest(nextPage);
            }

            page.setSkip(true);
        } else if (url.startsWith(blog_prefix)) {
            log.info("解析博客内容页[{}]", url);

            String id = url.replace(blog_prefix, "").replace(".html", "");
            String title = page.getHtml().xpath("//div[@class='post']/h1[@class='postTitle']//span/text()").get();
            String time = page.getHtml().xpath("//div[@class='postDesc']/span[@id='post-date']/text()").get();
            String content = page.getHtml().xpath("//div[@class='post']//div[@id='cnblogs_post_body']").get();

            // 抓取标签列表（可能为空）
            List<String> tags = page.getHtml().xpath("//div[@id='EntryTag']/a/text()").all();

            Blog blog = new Blog();
            blog.setId(id);
            blog.setTitle(title);
            blog.setContent(content);
            blog.setAuthor(bloggerId);
            blog.setTags(tags); // Blog 类中应有 List<String> tags 字段
            blog.setUrl(url);

            // 获取动态数据（阅读数、评论数、推荐、反对）
            HttpUtils httpUtils = new HttpUtils();
            String json = httpUtils.postJson(
                    String.format("https://www.cnblogs.com/%s/ajax/GetPostStat", bloggerId),
                    String.format("[%s]", id),
                    null);

            if (!StringUtil.isEmpty(json)) {
                List<BlogStats> blogStats = JSONObject.parseArray(json, BlogStats.class);
                if (blogStats != null && blogStats.size() > 0) {
                    log.info("成功获取博文[{}]的阅读数等信息:[{}]", id, json);
                    blog.setBlogStats(blogStats.get(0));
                }
            }

            try {
                blog.setDate(sdf.parse(time).getTime());
            } catch (ParseException e) {
                log.warn("解析时间出错: {}", time);
            }

            page.putField(RESULT_ITEM_KEY, blog);
        }
    }

}