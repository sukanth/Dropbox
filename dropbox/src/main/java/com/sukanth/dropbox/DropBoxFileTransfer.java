package com.sukanth.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderBuilder;
import com.dropbox.core.v2.files.ListFolderResult;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author sukanthgunda
 */
public class DropBoxFileTransfer {
    final static Logger LOG = Logger.getLogger(DropBoxFileTransfer.class);
    public static final List<String> failed = new ArrayList<>();
    public static final List<String> finalFailedList = new ArrayList<>();
    public static final List<String> noOfFiles = new ArrayList<>();
    public static void main(String[] args) throws InterruptedException {
        Properties properties;
        ThreadPoolExecutor threadPoolExecutor = null;
        ListFolderResult result;
        LocalDateTime startTime = LocalDateTime.now();
        DropBoxFileTransferJob dropBoxFileTransferJob = null;
        DbxClientV2 dropboxClient = null;
        properties = loadPropertiesFile();
        LOG.info("Loaded Properties");
        String SOURCE_LOCATION = properties.getProperty("SOURCE_LOCATION").trim();
        int THREAD_POOL_SIZE = Integer.parseInt(properties.getProperty("THREAD_POOL_SIZE"));
        String ACCESS_TOKEN = properties.getProperty("ACCESS_TOKEN").trim();
        String CLIENT_IDENTIFIER = properties.getProperty("CLIENT_IDENTIFIER").trim();
        String DESTINATION_LOCATION = properties.getProperty("DESTINATION_LOCATION").trim();
        ListFolderBuilder listFolderBuilder;
        try {
            LOG.info("Transfer Start Time " + startTime);
            LOG.info("Started transferring files in " + SOURCE_LOCATION);
            dropboxClient = authenticate(ACCESS_TOKEN, CLIENT_IDENTIFIER);

            LOG.info("Authenticated to User " + dropboxClient.users().getCurrentAccount().getName().getDisplayName());
            listFolderBuilder = dropboxClient.files().listFolderBuilder(SOURCE_LOCATION);

            result = listFolderBuilder.withIncludeDeleted(false).withRecursive(true).withIncludeMediaInfo(false).start();
            threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            LOG.info("Thread Pool Size " + threadPoolExecutor.getMaximumPoolSize());
            while (true) {
                if (Objects.nonNull(result)) {
                    dropBoxFileTransferJob = new DropBoxFileTransferJob(DESTINATION_LOCATION, result, dropboxClient);
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
           if(Objects.nonNull(threadPoolExecutor)){
               threadPoolExecutor.shutdown();
               if (threadPoolExecutor.awaitTermination(60, TimeUnit.DAYS)) {
                   if (failed.size() > 0) {
                       for (String failedFile : failed) {
                           LOG.warn("RETRYING FAILED TRANSFER " + failedFile);
                           if (Objects.nonNull(dropBoxFileTransferJob)) {
                               dropBoxFileTransferJob.downloadFile(dropboxClient, failedFile, DESTINATION_LOCATION.concat(failedFile), true);
                           }
                       }
                   }
                   if (finalFailedList.size() > 0) {
                       finalFailedList.stream().map(finalTry -> "NOT PROCESSED FILE " + finalTry).forEach(LOG::error);
                   }
                   Duration duration = Duration.between(startTime, LocalDateTime.now());
                   LOG.info("Transfer Completed in " + duration.toHours() + " Hours/ " + duration.toMinutes() + " Minutes/ " + duration.toMillis() + " MilliSeconds");
                   LOG.info((noOfFiles.size() - finalFailedList.size())+" Files Processed "+finalFailedList.size()+" Files Failed");
               }
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
            LOG.error(e);
        }
        return new DbxClientV2(Objects.requireNonNull(config), ACCESS_TOKEN);
    }

    /**
     * Method to load Properties file.
     *
     * @return prop
     */
    public static Properties loadPropertiesFile() {
        Properties prop = null;
        try (InputStream inputStream = DropBoxFileTransfer.class.getClassLoader().getResourceAsStream("config.properties")) {
            prop = new Properties();
            if (Objects.isNull(inputStream)) {
                throw new IOException("Unable to find config.properties");
            }
            prop.load(inputStream);
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
            System.exit(0);
        }
        return prop;
    }
}
