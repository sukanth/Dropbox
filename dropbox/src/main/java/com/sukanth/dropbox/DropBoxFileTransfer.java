package com.sukanth.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderBuilder;
import com.dropbox.core.v2.files.ListFolderResult;
import org.apache.log4j.Logger;

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
    protected static final List<String> failed = new ArrayList<>();
    protected static final List<String> finalFailedList = new ArrayList<>();
    protected static final List<String> noOfFiles = new ArrayList<>();
    protected static final List<String> noOfUpdatedFiles = new ArrayList<>();
    private static Logger logger = Logger.getLogger(DropBoxFileTransfer.class);

    public static void main(String[] args) throws InterruptedException {
        Properties properties;
        ThreadPoolExecutor threadPoolExecutor = null;
        ListFolderResult result;
        LocalDateTime startTime = LocalDateTime.now();
        DropBoxFileTransferJob dropBoxFileTransferJob = null;
        DbxClientV2 dropboxClient = null;
        properties = loadPropertiesFile();
        logger.info("Loaded Properties");
        String sourceLocation = properties.getProperty("SOURCE_LOCATION").trim();
        int threadPoolSize = Integer.parseInt(properties.getProperty("THREAD_POOL_SIZE"));
        String accessToken = properties.getProperty("ACCESS_TOKEN").trim();
        String clientIdentifier = properties.getProperty("CLIENT_IDENTIFIER").trim();
        String destinationLocation = properties.getProperty("DESTINATION_LOCATION").trim();
        ListFolderBuilder listFolderBuilder;
        try {
            logger.info("Transfer Start Time " + startTime);
            logger.info("Started transferring files in " + sourceLocation);
            dropboxClient = authenticate(accessToken, clientIdentifier);

            logger.info("Authenticated to User " + dropboxClient.users().getCurrentAccount().getName().getDisplayName());
            listFolderBuilder = dropboxClient.files().listFolderBuilder(sourceLocation);

            result = listFolderBuilder.withIncludeDeleted(false).withRecursive(true).withIncludeMediaInfo(false).start();
            threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadPoolSize);
            logger.info("Thread Pool Size " + threadPoolExecutor.getMaximumPoolSize());
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
            logger.error(e);
        } finally {
            if (Objects.nonNull(threadPoolExecutor)) {
                threadPoolExecutor.shutdown();
                if (threadPoolExecutor.awaitTermination(60, TimeUnit.DAYS)) {
                    if (!failed.isEmpty()) {
                        for (String failedFile : failed) {
                            logger.warn("RETRYING FAILED TRANSFER " + failedFile);
                            if (Objects.nonNull(dropBoxFileTransferJob)) {
                                dropBoxFileTransferJob.downloadFile(dropboxClient, failedFile, destinationLocation.concat(failedFile), true);
                            }
                        }
                    }
                    if (!finalFailedList.isEmpty()) {
                        finalFailedList.stream().map(finalTry -> "NOT PROCESSED FILE " + finalTry).forEach(logger::error);
                    }
                    if (!noOfUpdatedFiles.isEmpty()) {
                        noOfUpdatedFiles.stream().map(updatedFiles -> "UPDATED FILE " + updatedFiles).forEach(logger::warn);
                    }
                    Duration duration = Duration.between(startTime, LocalDateTime.now());
                    logger.info("Transfer Completed in " + duration.toHours() + " Hours/ " + duration.toMinutes() + " Minutes/ " + duration.toMillis() + " MilliSeconds");
                    logger.info((noOfFiles.size() - finalFailedList.size()) + " File/Files Processed " + finalFailedList.size() + " File/Files Failed " + noOfUpdatedFiles.size() + " File/Files Updated");
                }
            }
        }
    }

    /**
     * Method to authenticate to Dropbox.
     *
     * @return {@link DbxClientV2}
     */
    public static DbxClientV2 authenticate(String accessToken, String clientIdentifier) {
        DbxRequestConfig config = null;
        try {
            accessToken = accessToken.trim();
            config = DbxRequestConfig.newBuilder(clientIdentifier.trim()).build();
        } catch (Exception e) {
            logger.error(e);
        }
        return new DbxClientV2(Objects.requireNonNull(config), accessToken);
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
            logger.error(ex.getMessage(), ex);
            System.exit(0);
        }
        return prop;
    }
}
