package com.sukanth.dropbox;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;

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
            for (Metadata entry : result.getEntries()) {
                if (entry instanceof FileMetadata) {
                    String destinationFilePath = destinationLocation.concat(entry.getPathDisplay());
                    File file = new File(destinationFilePath);
                    if (!file.exists()) {
                        downloadFile(dropBoxClient, entry.getPathLower(), destinationFilePath);
                    } else {
                        LOG.info("File Exists, skipped file " + destinationFilePath);
                    }
                } else if (entry instanceof FolderMetadata) {
                        File file = new File(entry.getPathLower());
                        String destinationFolderPath = destinationLocation.concat(entry.getPathDisplay());
                        File destPath = new File(destinationFolderPath);
                        if (!destPath.exists()) {
                            boolean mkdirs = destPath.mkdirs();
                            if (mkdirs) {
                                LOG.info("Created Folder " + destPath.getAbsolutePath());
                            } else {
                                LOG.error("Can't create folder " + destPath.getAbsolutePath());
                            }
                        } else {
                            LOG.info("Folder Exists, skipping folder creation " + destPath.getAbsolutePath());
                        }
                } else {
                    LOG.info("Neither a file not a folder" + entry.getPathLower());
                }
            }
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
    public static void downloadFile(DbxClientV2 client, String dropBoxFilePath, String localFileAbsolutePath) {
        try {
            //Create DbxDownloader
            DbxDownloader<FileMetadata> dl = client.files().download(dropBoxFilePath);
            //FileOutputStream
            FileOutputStream fOut = new FileOutputStream(localFileAbsolutePath);
            LOG.info("*** Downloading File from.... " + dropBoxFilePath + " ---To--- " + localFileAbsolutePath + " ***");
            //Add a progress Listener
            dl.download(fOut);
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            LOG.error(e);
        }
    }
}
