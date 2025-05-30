package cn.edu.bistu.cs.ir.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 面向新浪博客的模型类
 * @author chenruoyu
 */
@Getter
@Setter
public class Blog {

    /**
     * 页面的唯一ID
     */
    private String id;

    /**
     * 标题
     */
    private String title;

    /**
     * 日期
     */
    private long date;

    /**
     * 正文内容
     */
    private String content;

    /**
     * 作者
     */
    private String author;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 动态获取的博文信息
     */
    private BlogStats blogStats;
    private String url; // 👈 添加这个字段

    // Getter 和 Setter
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
