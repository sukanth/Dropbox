package com.sukanth.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderBuilder;
import com.dropbox.core.v2.files.ListFolderContinueErrorException;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import org.apache.log4j.Logger;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class DropBoxFileTransfer {
    private static final String usrHome = System.getProperty("user.home");
    final static Logger LOG = Logger.getLogger(DropBoxFileTransfer.class);
    public static void main(String[] args) {
        String destinationLocation = usrHome.concat(File.separator.concat("Desktop/Test"));
        String sourceLocation = "/Photos";
        LOG.info("File Transfer started from SourcePath "+sourceLocation);
        ListFolderBuilder listFolderBuilder;
        ListFolderResult result;
        String ACCESS_TOKEN = "ACCESS_TOKEN";
        String CLIENT_IDENTIFIER = "CLIENT_IDENTIFIER";
        try {
            LOG.info("Transfer Start Time "+ LocalDateTime.now());
            DbxClientV2 dropboxClient = authenticate(ACCESS_TOKEN,CLIENT_IDENTIFIER);

            LOG.info("Authenticated to User "+dropboxClient.users().getCurrentAccount().getName().getDisplayName());
            listFolderBuilder = dropboxClient.files().listFolderBuilder(sourceLocation);

            result = listFolderBuilder.withIncludeDeleted(false).withRecursive(true).withIncludeMediaInfo(false).start();
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(80);
            LOG.info("Thread Pool Size "+threadPoolExecutor.getMaximumPoolSize());
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
            LOG.info("Transfer End Time "+LocalDateTime.now());
        } catch (ListFolderContinueErrorException e) {
            LOG.error(e.getStackTrace());
        } catch (ListFolderErrorException e) {
            LOG.error(e.getStackTrace());
        } catch (DbxException e) {
            LOG.error(e.getStackTrace());
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
