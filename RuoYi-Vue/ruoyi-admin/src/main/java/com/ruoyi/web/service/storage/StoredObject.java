package com.ruoyi.web.service.storage;

/**
 * Uploaded object metadata returned to existing upload clients.
 */
public class StoredObject
{
    private String url;

    private String fileName;

    private String newFileName;

    private String originalFilename;

    public StoredObject()
    {
    }

    public StoredObject(String url, String fileName, String newFileName, String originalFilename)
    {
        this.url = url;
        this.fileName = fileName;
        this.newFileName = newFileName;
        this.originalFilename = originalFilename;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    public String getNewFileName()
    {
        return newFileName;
    }

    public void setNewFileName(String newFileName)
    {
        this.newFileName = newFileName;
    }

    public String getOriginalFilename()
    {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename)
    {
        this.originalFilename = originalFilename;
    }
}
