package com.ruoyi.web.service.storage;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Primary
@Service
public class RoutingObjectStorageService implements ObjectStorageService
{
    private static final String TENCENT_COS = "tencent-cos";

    private final StorageProperties properties;

    private final LocalObjectStorageService localObjectStorageService;

    private final TencentCosObjectStorageService tencentCosObjectStorageService;

    public RoutingObjectStorageService(StorageProperties properties, LocalObjectStorageService localObjectStorageService,
            TencentCosObjectStorageService tencentCosObjectStorageService)
    {
        this.properties = properties;
        this.localObjectStorageService = localObjectStorageService;
        this.tencentCosObjectStorageService = tencentCosObjectStorageService;
    }

    @Override
    public StoredObject upload(MultipartFile file) throws Exception
    {
        return current().upload(file);
    }

    @Override
    public StoredObject upload(MultipartFile file, String[] allowedExtension, boolean useCustomNaming) throws Exception
    {
        return current().upload(file, allowedExtension, useCustomNaming);
    }

    @Override
    public StoredObject upload(String localBaseDir, MultipartFile file, String[] allowedExtension, boolean useCustomNaming)
            throws Exception
    {
        return current().upload(localBaseDir, file, allowedExtension, useCustomNaming);
    }

    @Override
    public String resolveUrl(String key) throws Exception
    {
        return current().resolveUrl(key);
    }

    private ObjectStorageService current()
    {
        if (TENCENT_COS.equalsIgnoreCase(properties.getProvider()))
        {
            return tencentCosObjectStorageService;
        }
        return localObjectStorageService;
    }
}
