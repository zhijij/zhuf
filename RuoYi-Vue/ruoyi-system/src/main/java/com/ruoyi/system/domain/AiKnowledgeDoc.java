package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI知识库文档对象 ai_knowledge_doc
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public class AiKnowledgeDoc extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文档ID */
    private Long docId;

    /** 标题 */
    @Excel(name = "标题")
    private String title;

    /** 分类:faq/policy/contract/platform_rule */
    @Excel(name = "分类:faq/policy/contract/platform_rule")
    private String category;

    /** 内容 */
    @Excel(name = "内容")
    private String content;

    /** 版本 */
    @Excel(name = "版本")
    private String version;

    /** 是否启用 */
    @Excel(name = "是否启用")
    private String enabled;

    /** 向量状态 */
    @Excel(name = "向量状态")
    private String vectorStatus;

    public void setDocId(Long docId) 
    {
        this.docId = docId;
    }

    public Long getDocId() 
    {
        return docId;
    }

    public void setTitle(String title) 
    {
        this.title = title;
    }

    public String getTitle() 
    {
        return title;
    }

    public void setCategory(String category) 
    {
        this.category = category;
    }

    public String getCategory() 
    {
        return category;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setVersion(String version) 
    {
        this.version = version;
    }

    public String getVersion() 
    {
        return version;
    }

    public void setEnabled(String enabled) 
    {
        this.enabled = enabled;
    }

    public String getEnabled() 
    {
        return enabled;
    }

    public void setVectorStatus(String vectorStatus) 
    {
        this.vectorStatus = vectorStatus;
    }

    public String getVectorStatus() 
    {
        return vectorStatus;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("docId", getDocId())
            .append("title", getTitle())
            .append("category", getCategory())
            .append("content", getContent())
            .append("version", getVersion())
            .append("enabled", getEnabled())
            .append("vectorStatus", getVectorStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
