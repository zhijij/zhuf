package com.ruoyi.web.service.storage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.framework.config.ServerConfig;

@Service
public class TencentCosObjectStorageService implements ObjectStorageService
{
    private static final Logger log = LoggerFactory.getLogger(TencentCosObjectStorageService.class);

    private final TencentCosProperties properties;

    private final ServerConfig serverConfig;

    public TencentCosObjectStorageService(TencentCosProperties properties, ServerConfig serverConfig)
    {
        this.properties = properties;
        this.serverConfig = serverConfig;
    }

    @Override
    public StoredObject upload(MultipartFile file) throws Exception
    {
        return upload(file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION, false);
    }

    @Override
    public StoredObject upload(MultipartFile file, String[] allowedExtension, boolean useCustomNaming) throws Exception
    {
        return upload(null, file, allowedExtension, useCustomNaming);
    }

    @Override
    public StoredObject upload(String localBaseDir, MultipartFile file, String[] allowedExtension, boolean useCustomNaming)
            throws Exception
    {
        validateConfig();
        FileUploadUtils.assertAllowed(file, allowedExtension);

        String key = buildObjectKey(file, useCustomNaming);
        COSClient cosClient = createClient();
        try
        {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            if (StringUtils.isNotEmpty(file.getContentType()))
            {
                metadata.setContentType(file.getContentType());
            }
            PutObjectRequest request = new PutObjectRequest(properties.getBucketName(), key, file.getInputStream(), metadata);
            cosClient.putObject(request);
        }
        catch (CosServiceException e)
        {
            log.error("Tencent COS service error, bucket={}, region={}, key={}, statusCode={}, errorCode={}, requestId={}",
                    properties.getBucketName(), properties.getRegion(), key, e.getStatusCode(), e.getErrorCode(),
                    e.getRequestId(), e);
            throw e;
        }
        catch (CosClientException e)
        {
            log.error("Tencent COS client error, bucket={}, region={}, key={}", properties.getBucketName(),
                    properties.getRegion(), key, e);
            throw e;
        }
        finally
        {
            cosClient.shutdown();
        }

        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        String fileName = "/common/storage?key=" + encodedKey;
        String stableUrl = serverConfig.getUrl() + fileName;
        return new StoredObject(stableUrl, fileName, FileUtils.getName(key), file.getOriginalFilename());
    }

    @Override
    public String resolveUrl(String key) throws Exception
    {
        validateConfig();
        String safeKey = normalizeKey(key);
        if (properties.getPresignedExpirationSeconds() <= 0)
        {
            return publicUrl(safeKey);
        }

        COSClient cosClient = createClient();
        try
        {
            Date expiration = new Date(System.currentTimeMillis() + properties.getPresignedExpirationSeconds() * 1000L);
            return cosClient.generatePresignedUrl(properties.getBucketName(), safeKey, expiration, HttpMethodName.GET)
                    .toString();
        }
        finally
        {
            cosClient.shutdown();
        }
    }

    private COSClient createClient()
    {
        COSCredentials credentials = new BasicCOSCredentials(properties.getSecretId(), properties.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(properties.getRegion()));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        clientConfig.setConnectionTimeout(10_000);
        clientConfig.setSocketTimeout(30_000);
        return new COSClient(credentials, clientConfig);
    }

    private String buildObjectKey(MultipartFile file, boolean useCustomNaming)
    {
        String extension = FileUploadUtils.getExtension(file);
        String objectName = IdUtils.fastSimpleUUID();
        return normalizePrefix(properties.getObjectPrefix()) + "/" + DateUtils.datePath() + "/" + objectName + "."
                + extension;
    }

    private String publicUrl(String key)
    {
        return "https://" + properties.getBucketName() + ".cos." + properties.getRegion() + ".myqcloud.com/" + key;
    }

    private String normalizePrefix(String prefix)
    {
        if (StringUtils.isEmpty(prefix))
        {
            return "images";
        }
        String normalized = prefix.trim().replace("\\", "/");
        while (normalized.startsWith("/"))
        {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/"))
        {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (StringUtils.isEmpty(normalized) || normalized.contains(".."))
        {
            return "images";
        }
        return normalized;
    }

    private String normalizeKey(String key)
    {
        if (StringUtils.isEmpty(key) || key.startsWith("/") || key.contains("..") || key.contains("\\"))
        {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return key;
    }

    private void validateConfig()
    {
        if (StringUtils.isEmpty(properties.getSecretId()) || StringUtils.isEmpty(properties.getSecretKey())
                || StringUtils.isEmpty(properties.getRegion()) || StringUtils.isEmpty(properties.getBucketName()))
        {
            throw new IllegalStateException("Tencent COS is not fully configured");
        }
    }
}
