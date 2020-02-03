package com.sukanth.dropbox;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.RateLimitException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;

/**
 * @author sukanthgunda
 * @apiNote Thread to copy files from dropbox to desired location.
 */
public class DropBoxFileTransferJob implements Runnable {
    final static Logger LOG = Logger.getLogger(DropBoxFileTransferJob.class);
    final String destinationLocation;
    final ListFolderResult result;
    final DbxClientV2 dropBoxClient;

    public DropBoxFileTransferJob(String destinationLocation, ListFolderResult result, DbxClientV2 dropBoxClient) {
        this.destinationLocation = destinationLocation;
        this.result = result;
        this.dropBoxClient = dropBoxClient;
    }

    @Override
    public void run() {
        try {
            LOG.info("Processing Thread Entries " + result.getEntries().size());
            result.getEntries().forEach(entry -> {
                if (entry instanceof FileMetadata) {
                    File destinationFilePath = new File(destinationLocation.concat(entry.getPathDisplay()));
                    if (!destinationFilePath.exists()) {
                        downloadFile(dropBoxClient, entry.getPathLower(), destinationFilePath.getAbsolutePath(), false);
                    } else {
                        LOG.info("File Exists, skipped file " + destinationFilePath.getAbsolutePath());
                    }
                } else if (entry instanceof FolderMetadata) {
                    File destFolderPath = new File(destinationLocation.concat(entry.getPathDisplay()));
                    if (!destFolderPath.exists()) {
                        boolean mkdirs = destFolderPath.mkdirs();
                        if (mkdirs) {
                            LOG.info("Created Folder " + destFolderPath.getAbsolutePath());
                        } else {
                            LOG.error("Can't create folder " + destFolderPath.getAbsolutePath());
                        }
                    } else {
                        LOG.info("Folder Exists, skipping folder creation " + destFolderPath.getAbsolutePath());
                    }
                } else {
                    LOG.error("Neither a file not a folder" + entry.getPathLower());
                }
            });
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    /**
     * Download Dropbox File to Local Computer
     *
     * @param client                Current connected client
     * @param dropBoxFilePath       The file path on the Dropbox cloud server -> [/foldername/something.txt]
     * @param localFileAbsolutePath The absolute file path of the File on the Local File System
     */
    public void downloadFile(DbxClientV2 client, String dropBoxFilePath, String localFileAbsolutePath, boolean retry) {
        try {
            if (retry) {
                File destPath = new File(localFileAbsolutePath);
                if (!destPath.exists()) {
                    boolean mkdirs = destPath.mkdirs();
                    if (mkdirs) {
                        LOG.info("Created Path " + destPath.getAbsolutePath());
                    } else {
                        LOG.error("Can't create Path " + destPath.getAbsolutePath());
                    }
                }
            }
            DbxDownloader<FileMetadata> dl = client.files().download(dropBoxFilePath);
            FileOutputStream fOut = new FileOutputStream(localFileAbsolutePath);
            LOG.info("*** Downloading File from.... " + dropBoxFilePath + " ...To... " + localFileAbsolutePath + " ***");
            dl.download(fOut);
            fOut.flush();
            fOut.close();
        } catch (RateLimitException e) {
            try {
                LOG.info("Waiting... " + e.getBackoffMillis() + " MilliSeconds for RateLimit Backoff " + dropBoxFilePath);
                DropBoxFileTransfer.failed.add(dropBoxFilePath);
                Thread.sleep(e.getBackoffMillis());
                LOG.info("Resuming Wait..");
            } catch (InterruptedException ex) {
                LOG.error(ex);
            }
        } catch (Exception e) {
            if (retry) {
                LOG.error("ERROR PROCESSING RETRY : From " + dropBoxFilePath + " ...To... " + localFileAbsolutePath, e);
            }
            if (!retry) {
                DropBoxFileTransfer.failed.add(dropBoxFilePath);
            } else {
                DropBoxFileTransfer.finalFailedList.add(dropBoxFilePath);
            }
        }
    }
}
