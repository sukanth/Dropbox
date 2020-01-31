package com.sukanth.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class DropBoxFileTransfer {
    private static final String usrHome = System.getProperty("user.home");

    public static void main(String[] args) {
        String destinationLocation = "/Volumes/TimeCapsule/Sukanth/Photos";//usrHome.concat(File.separator.concat("Desktop/Test"));
        String sourceLocation = "/Photos";
        ListFolderBuilder listFolderBuilder = null;
        ListFolderResult result = null;
        try {
            DbxClientV2 dropboxClient = authenticate();
            listFolderBuilder = dropboxClient.files().listFolderBuilder(sourceLocation);
            result = listFolderBuilder.withIncludeDeleted(false).withRecursive(true).withIncludeMediaInfo(false).start();
            createFolders(result, dropboxClient, destinationLocation);
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(80);
            while (true) {
                if (Objects.nonNull(result)) {
                        DropBoxFileTransferJob dropBoxFileTransferJob = new DropBoxFileTransferJob(destinationLocation, result, dropboxClient);
                        threadPoolExecutor.execute(dropBoxFileTransferJob);
                     if(!result.getHasMore()) {
                        break;
                    }
                }
                result = dropboxClient.files().listFolderContinue(result.getCursor());
            }
        } catch (ListFolderContinueErrorException e) {
            e.printStackTrace();
        } catch (ListFolderErrorException e) {
            e.printStackTrace();
        } catch (DbxException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param result
     * @param dropboxClient
     * @param destinationLocation
     */
    private static void createFolders(ListFolderResult result, DbxClientV2 dropboxClient, String destinationLocation) {
        try {
            while (true) {
                if (Objects.nonNull(result)) {
                        for (Metadata metadataEntry : result.getEntries()) {
                            if (metadataEntry instanceof FolderMetadata) {
                                File file = new File(metadataEntry.getPathLower());
                                String destinationFolderPath = destinationLocation.concat(metadataEntry.getPathDisplay());
                                File destPath = new File(destinationFolderPath);
                                if (!destPath.exists()) {
                                    destPath.mkdirs();
                                }
                            }
                        }
                     if(!result.getHasMore()) {
                        break;
                    }
                }
                result = dropboxClient.files().listFolderContinue(result.getCursor());
            }
        } catch (Exception e) {

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
