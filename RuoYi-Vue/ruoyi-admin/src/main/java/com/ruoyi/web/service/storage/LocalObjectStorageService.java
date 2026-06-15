package com.ruoyi.web.service.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.framework.config.ServerConfig;

@Service
public class LocalObjectStorageService implements ObjectStorageService
{
    private final ServerConfig serverConfig;

    public LocalObjectStorageService(ServerConfig serverConfig)
    {
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
        return upload(RuoYiConfig.getUploadPath(), file, allowedExtension, useCustomNaming);
    }

    @Override
    public StoredObject upload(String localBaseDir, MultipartFile file, String[] allowedExtension, boolean useCustomNaming)
            throws Exception
    {
        String fileName = FileUploadUtils.upload(localBaseDir, file, allowedExtension, useCustomNaming);
        String url = serverConfig.getUrl() + fileName;
        return new StoredObject(url, fileName, FileUtils.getName(fileName), file.getOriginalFilename());
    }

    @Override
    public String resolveUrl(String key)
    {
        if (key == null)
        {
            return "";
        }
        return key;
    }
}
