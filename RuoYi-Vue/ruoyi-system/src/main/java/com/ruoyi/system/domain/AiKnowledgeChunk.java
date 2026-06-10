package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI知识库分片对象 ai_knowledge_chunk
 * 
 * @author ruoyi
 * @date 2026-06-10
 */
public class AiKnowledgeChunk extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 分片ID */
    private Long chunkId;

    /** 文档ID */
    @Excel(name = "文档ID")
    private Long docId;

    /** 分片序号 */
    @Excel(name = "分片序号")
    private Long chunkNo;

    /** 分片内容 */
    @Excel(name = "分片内容")
    private String content;

    /** Token数量 */
    @Excel(name = "Token数量")
    private Long tokenCount;

    /** 向量状态 */
    @Excel(name = "向量状态")
    private String vectorStatus;

    public void setChunkId(Long chunkId) 
    {
        this.chunkId = chunkId;
    }

    public Long getChunkId() 
    {
        return chunkId;
    }

    public void setDocId(Long docId) 
    {
        this.docId = docId;
    }

    public Long getDocId() 
    {
        return docId;
    }

    public void setChunkNo(Long chunkNo) 
    {
        this.chunkNo = chunkNo;
    }

    public Long getChunkNo() 
    {
        return chunkNo;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setTokenCount(Long tokenCount) 
    {
        this.tokenCount = tokenCount;
    }

    public Long getTokenCount() 
    {
        return tokenCount;
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
            .append("chunkId", getChunkId())
            .append("docId", getDocId())
            .append("chunkNo", getChunkNo())
            .append("content", getContent())
            .append("tokenCount", getTokenCount())
            .append("vectorStatus", getVectorStatus())
            .append("createTime", getCreateTime())
            .toString();
    }
}
