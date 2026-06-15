package com.ruoyi.web.service.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Storage abstraction for local and cloud-backed uploads.
 */
public interface ObjectStorageService
{
    StoredObject upload(MultipartFile file) throws Exception;

    StoredObject upload(MultipartFile file, String[] allowedExtension, boolean useCustomNaming) throws Exception;

    StoredObject upload(String localBaseDir, MultipartFile file, String[] allowedExtension, boolean useCustomNaming)
            throws Exception;

    String resolveUrl(String key) throws Exception;
}
