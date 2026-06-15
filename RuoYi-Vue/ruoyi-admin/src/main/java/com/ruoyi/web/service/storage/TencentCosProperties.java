package com.ruoyi.web.service.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tencent.cos")
public class TencentCosProperties
{
    private String secretId;

    private String secretKey;

    private String region = "ap-beijing";

    private String bucketName = "homeimage-1419823100";

    private long presignedExpirationSeconds = 43200L;

    private String objectPrefix = "images";

    public String getSecretId()
    {
        return secretId;
    }

    public void setSecretId(String secretId)
    {
        this.secretId = secretId;
    }

    public String getSecretKey()
    {
        return secretKey;
    }

    public void setSecretKey(String secretKey)
    {
        this.secretKey = secretKey;
    }

    public String getRegion()
    {
        return region;
    }

    public void setRegion(String region)
    {
        this.region = region;
    }

    public String getBucketName()
    {
        return bucketName;
    }

    public void setBucketName(String bucketName)
    {
        this.bucketName = bucketName;
    }

    public long getPresignedExpirationSeconds()
    {
        return presignedExpirationSeconds;
    }

    public void setPresignedExpirationSeconds(long presignedExpirationSeconds)
    {
        this.presignedExpirationSeconds = presignedExpirationSeconds;
    }

    public String getObjectPrefix()
    {
        return objectPrefix;
    }

    public void setObjectPrefix(String objectPrefix)
    {
        this.objectPrefix = objectPrefix;
    }
}
