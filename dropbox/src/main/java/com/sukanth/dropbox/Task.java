package com.sukanth.dropbox;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sukanth Gunda
 */
public class Task implements Runnable {

    String destinationLocation;
    Metadata folder;
    DbxClientV2 dropboxClient;


    public Task(String destinationLocation, Metadata folder, DbxClientV2 dropboxClient) {
        this.destinationLocation = destinationLocation;
        this.folder = folder;
        this.dropboxClient = dropboxClient;
    }

    @Override
    public void run() {
        String destinationFolderPath = destinationLocation.concat(File.separator).concat(folder.getName());
        File destPath = new File(destinationFolderPath);
        try {
            if (destPath.exists()) {
                List<File> filesInFolder = listFiles(folder.getPathLower());
                for (File fileInFolder : filesInFolder) {
                    File destinationFile = new File(destinationFolderPath.concat(File.separator).concat(fileInFolder.getName()));
                    if (!destinationFile.exists()) {
                        downloadFile(dropboxClient, fileInFolder.getAbsolutePath(), destinationFolderPath.concat(File.separator).concat(fileInFolder.getName()));
                    }
                }
            } else {
                if (destPath.mkdir()) {
                    List<File> filesInFolder = listFiles(folder.getPathLower());
                    for (File fileInFolder : filesInFolder) {
                        downloadFile(dropboxClient, fileInFolder.getAbsolutePath(), destinationFolderPath.concat(File.separator).concat(fileInFolder.getName()));
                    }
                } else {
                    System.out.println("Can not create folder in path :  " + destPath.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DownloadErrorException e) {
            e.printStackTrace();
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Method to list files in a folder
     *
     * @param sourcePath
     * @return files
     */
    public List<File> listFiles(String sourcePath) {
        List<File> files = null;
        try {
            files = new ArrayList<>();
            ListFolderResult listFolderResult = dropboxClient.files().listFolder(sourcePath);
            for (Metadata metadata : listFolderResult.getEntries()) {
                String filePath = metadata.getPathLower();
                files.add(new File(filePath));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    /**
     * Download Dropbox File to Local Computer
     *
     * @param client                Current connected client
     * @param dropBoxFilePath       The file path on the Dropbox cloud server -> [/foldername/something.txt]
     * @param localFileAbsolutePath The absolute file path of the File on the Local File System
     * @throws DbxException
     * @throws DownloadErrorException
     * @throws IOException
     */
    public void downloadFile(DbxClientV2 client, String dropBoxFilePath, String localFileAbsolutePath) throws DownloadErrorException, DbxException, IOException {

        //Create DbxDownloader
        DbxDownloader<FileMetadata> dl = client.files().download(dropBoxFilePath);

        //FileOutputStream
        FileOutputStream fOut = new FileOutputStream(localFileAbsolutePath);
        System.out.println("Downloading .... " + dropBoxFilePath);


        //Add a progress Listener
        dl.download(new ProgressOutputStream(fOut, dl.getResult().getSize(), (long completed, long totalSize) -> System.out.println((completed * 100) / totalSize + " %")));
    }
}
