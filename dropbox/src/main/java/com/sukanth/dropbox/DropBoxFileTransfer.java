package com.sukanth.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderBuilder;
import com.dropbox.core.v2.files.ListFolderResult;
import org.apache.log4j.Logger;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author sukanthgunda
 */
public class DropBoxFileTransfer {
    private static final String usrHome = System.getProperty("user.home");
    final static Logger LOG = Logger.getLogger(DropBoxFileTransfer.class);
    public static final List<String> failed = new ArrayList<>();
    public static final List<String> finalFailedList = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        String destinationLocation = usrHome.concat(File.separator.concat("Desktop/Test"));
        String sourceLocation = "/My Documents";
        LOG.info("Started transferring files in " + sourceLocation);
        ListFolderBuilder listFolderBuilder;
        ThreadPoolExecutor threadPoolExecutor = null;
        ListFolderResult result;
        String ACCESS_TOKEN = "ACCESS_TOKEN";
        String CLIENT_IDENTIFIER = "CLIENT_IDENTIFIER";
        LocalDateTime startTime = LocalDateTime.now();
        DropBoxFileTransferJob dropBoxFileTransferJob = null;
        DbxClientV2 dropboxClient = null;
        try {
            LOG.info("Transfer Start Time " + startTime);
            dropboxClient = authenticate(ACCESS_TOKEN, CLIENT_IDENTIFIER);

            LOG.info("Authenticated to User " + dropboxClient.users().getCurrentAccount().getName().getDisplayName());
            listFolderBuilder = dropboxClient.files().listFolderBuilder(sourceLocation);

            result = listFolderBuilder.withIncludeDeleted(false).withRecursive(true).withIncludeMediaInfo(false).start();
            threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(80);
            LOG.info("Thread Pool Size " + threadPoolExecutor.getMaximumPoolSize());
            while (true) {
                if (Objects.nonNull(result)) {
                    dropBoxFileTransferJob = new DropBoxFileTransferJob(destinationLocation, result, dropboxClient);
                    threadPoolExecutor.execute(dropBoxFileTransferJob);
                    if (!result.getHasMore()) {
                        break;
                    }
                }
                result = dropboxClient.files().listFolderContinue(result.getCursor());
            }
        } catch (DbxException e) {
            LOG.error(e);
        } finally {
            Objects.requireNonNull(threadPoolExecutor).shutdown();
            if (threadPoolExecutor.awaitTermination(60, TimeUnit.DAYS)) {
                if (failed.size() > 0) {
                    for (String failedFile : failed) {
                        LOG.warn("RETRYING FAILED TRANSFER " + failedFile);
                        if (Objects.nonNull(dropBoxFileTransferJob)) {
                            dropBoxFileTransferJob.downloadFile(dropboxClient, failedFile, destinationLocation.concat(failedFile), true);
                        }
                    }
                }
                if (finalFailedList.size() > 0) {
                    finalFailedList.stream().map(finalTry -> "NOT PROCESSED FILE " + finalTry).forEach(LOG::error);
                }
                Duration duration = Duration.between(startTime, LocalDateTime.now());
                LOG.info("Transfer Completed in " + duration.toHours() + " Hours " + duration.toMinutes() + " Minutes " + duration.toMillis() + " MilliSeconds");
            }
        }
    }

    /**
     * Method to authenticate to Dropbox.
     *
     * @return {@link DbxClientV2}
     */
    public static DbxClientV2 authenticate(String ACCESS_TOKEN, String CLIENT_IDENTIFIER) {
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
