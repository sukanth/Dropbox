package com.sukanth.dropbox;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Sukanth Gunda
 * Program to transfer files from dropbox to desired location.
 */
public class FileTransfer {
    private static final String usrHome = System.getProperty("user.home");

    public static void main(String[] args) {
        String destinationLocation = usrHome.concat(File.separator.concat("Desktop"));
        try {
            DbxClientV2 dropboxClient = authenticate();
            List<Metadata> foldersInPhotos = dropboxClient.files().listFolder(File.separator.concat("Photos")).getEntries();
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
            for (Metadata folder : foldersInPhotos) {
                Task task = new Task(destinationLocation, folder, authenticate());
                threadPoolExecutor.execute(task);
            }
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
}
