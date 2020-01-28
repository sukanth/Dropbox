package com.sukanth.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
                File file = new File(destinationFolderPath);
                if (file.exists()) {
                    List<String> strings = listFiles(folder.getPathLower());
                } else {
                    if (file.mkdir()) {
                        List<String> strings = listFiles(folder.getPathLower());
                    } else {
                        System.out.println("Can not create folder in path :  " + file.getAbsolutePath());
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
     * @param folderPath
     * @return files
     * @throws Exception
     */
    public static List<String> listFiles(String folderPath) throws Exception {
        List<String> files = null;
        try {
            files = new ArrayList<String>();
            ListFolderResult listFolderResult = dropboxClient.files().listFolder(folderPath);
            for (Metadata metadata : listFolderResult.getEntries()) {
                String filePath = metadata.getPathLower();
                files.add(filePath);
            }
            Collections.sort(files, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    return s2.compareTo(s1);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }
}
