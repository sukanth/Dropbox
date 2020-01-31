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
        String destinationLocation = usrHome.concat(File.separator.concat("Desktop/Test"));
        String sourceLocation = "/Photos";
        ListFolderBuilder listFolderBuilder = null;
        ListFolderResult result = null;
        String ACCESS_TOKEN = "ACCESS_TOKEN";
        String CLIENT_IDENTIFIER = "CLIENT_IDENTIFIER";
        try {
            DbxClientV2 dropboxClient = authenticate(ACCESS_TOKEN,CLIENT_IDENTIFIER);
            listFolderBuilder = dropboxClient.files().listFolderBuilder(sourceLocation);
            result = listFolderBuilder.withIncludeDeleted(false).withRecursive(true).withIncludeMediaInfo(false).start();
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
     * Method to authenticate to Dropbox.
     *
     * @return {@link DbxClientV2}
     */
    public static DbxClientV2 authenticate(String ACCESS_TOKEN,String CLIENT_IDENTIFIER) {
        DbxRequestConfig config = null;
        try {
            ACCESS_TOKEN = ACCESS_TOKEN.trim();
            config = DbxRequestConfig.newBuilder(CLIENT_IDENTIFIER.trim()).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DbxClientV2(config, ACCESS_TOKEN);
    }
}
