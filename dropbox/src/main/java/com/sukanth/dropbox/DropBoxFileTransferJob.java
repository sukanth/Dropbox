package com.sukanth.dropbox;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.io.FileOutputStream;

public class DropBoxFileTransferJob implements Runnable {
    String destinationLocation;
    ListFolderResult result;
    DbxClientV2 dropBoxClient;

    public DropBoxFileTransferJob(String destinationLocation, ListFolderResult result, DbxClientV2 dropBoxClient) {
        this.destinationLocation = destinationLocation;
        this.result = result;
        this.dropBoxClient = dropBoxClient;
    }

    @Override
    public void run() {
        for (Metadata entry : result.getEntries()) {
            if (entry instanceof FileMetadata) {
                String destinationFolderPath = destinationLocation.concat(entry.getPathDisplay());
                File file = new File(destinationFolderPath);
                if (!file.exists()) {
                    downloadFile(dropBoxClient, entry.getPathLower(), destinationFolderPath);
                }
            } else if (entry instanceof FolderMetadata) {

            } else {
                System.out.println("Neither a file not a folder" + entry.getPathLower());
            }
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
            System.out.println("Downloading .... " + dropBoxFilePath);
            //Add a progress Listener
            dl.download(new ProgressOutputStream(fOut, dl.getResult().getSize(), (long completed, long totalSize) -> System.out.println((completed * 100) / totalSize + " %")));
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
