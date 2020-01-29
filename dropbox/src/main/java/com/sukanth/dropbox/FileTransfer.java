package com.sukanth.dropbox;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author Sukanth Gunda
 * Program to transfer files from dropbox to desired location.
 */
public class FileTransfer {
    private static DbxClientV2 dropboxClient = null;
    private static String destinationLocation = null;
    private static final String usrHome = System.getProperty("user.home");

    public static void main(String[] args) {
        destinationLocation = usrHome.concat(File.separator.concat("Desktop"));
        try {
            dropboxClient = authenticate();
            List<Metadata> foldersInPhotos = dropboxClient.files().listFolder(File.separator.concat("Photos")).getEntries();
            for (Metadata folder : foldersInPhotos) {
                String destinationFolderPath = destinationLocation.concat(File.separator).concat(folder.getName());
                File destPath = new File(destinationFolderPath);
                if (destPath.exists()) {
                    List<File> filesInFolder = listFiles(folder.getPathLower());
                    for (File fileInFolder : filesInFolder) {
                        File destinationFile = new File(destinationFolderPath.concat(File.separator).concat(fileInFolder.getName()));
                        if(!destinationFile.exists()){
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
            }
        } catch (ListFolderErrorException e) {
            e.printStackTrace();
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to authenticate to Dropbox.
     *
     * @return {@link DbxClientV2}
     */
    public static DbxClientV2 authenticate() {
        String ACCESS_TOKEN = null;
        DbxRequestConfig config = null;
        try {
            ACCESS_TOKEN = "0sswgo6tiyQAAAAAAAEvJ3Qe11tAktcr_rWxu0RQu_0ndqM4hu5qsxFNqSstKikE";
            config = DbxRequestConfig.newBuilder("sukanth").build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DbxClientV2(config, ACCESS_TOKEN);
    }

    /**
     * Method to list files in a folder
     *
     * @param sourcePath
     * @return files
     * @throws Exception
     */
    public static List<File> listFiles(String sourcePath) throws Exception {
        List<File> files = null;
        try {
            files = new ArrayList<File>();
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
    public static void downloadFile(DbxClientV2 client, String dropBoxFilePath, String localFileAbsolutePath) throws DownloadErrorException, DbxException, IOException {

        //Create DbxDownloader
        DbxDownloader<FileMetadata> dl = client.files().download(dropBoxFilePath);

        //FileOutputStream
        FileOutputStream fOut = new FileOutputStream(localFileAbsolutePath);
        System.out.println("Downloading .... " + dropBoxFilePath);


        //Add a progress Listener
        dl.download(new ProgressOutputStream(fOut, dl.getResult().getSize(), (long completed, long totalSize) -> {

            System.out.println((completed * 100) / totalSize + " %");

        }));
    }
}
