package com.sukanth.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
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
import com.dropbox.core.v2.files.Metadata;
import com.sukanth.dropbox.pojo.DropBoxApplicationProperties;
import org.apache.log4j.Logger;

/** @author sukanthgunda */
public class DropBoxFileTransfer {
  protected static final List<String> failed = new ArrayList<>();
  protected static final List<String> finalFailedList = new ArrayList<>();
  protected static final List<String> noOfFiles = new ArrayList<>();
  protected static final List<String> noOfUpdatedFiles = new ArrayList<>();
  protected static final List<String> noOfFoldersDeleted = new ArrayList<>();
  protected static final List<String> noOfFilesDeleted = new ArrayList<>();
  private static Logger logger = Logger.getLogger(DropBoxFileTransfer.class);

  public static void main(String[] args) throws InterruptedException {
    DropBoxApplicationProperties properties;
    ThreadPoolExecutor threadPoolExecutor = null;
    ListFolderResult result;
    LocalDateTime startTime = LocalDateTime.now();
    DropBoxFileTransferJob dropBoxFileTransferJob = null;
    DbxClientV2 dropboxClient = null;
    properties = loadPropertiesFile();
    logger.info("Loaded Properties");

    List<Metadata> listFolderResultsMetadata = new ArrayList<>();
    try {
      logger.info("Transfer Start Time " + startTime);
      logger.info("Started transferring files in " + properties.getSourceLocation());
      dropboxClient = authenticate(properties.getAccessToken(), properties.getAccessToken());

      logger.info(
          "Authenticated to User "
              + dropboxClient.users().getCurrentAccount().getName().getDisplayName());
      result =
          dropboxClient
              .files()
              .listFolderBuilder(loadPropertiesFile().getSourceLocation())
              .withIncludeDeleted(properties.isIncludedDeleted())
              .withRecursive(true)
              .withIncludeMediaInfo(false)
              .start();

      threadPoolExecutor =
          (ThreadPoolExecutor) Executors.newFixedThreadPool(properties.getThreadPooSize());
      logger.info("Thread Pool Size " + threadPoolExecutor.getMaximumPoolSize());
      while (true) {
        if (Objects.nonNull(result)) {
          List<Metadata> metadataList = result.getEntries();
          for (Metadata resultMetaData : metadataList) {
            listFolderResultsMetadata.add(resultMetaData);
            dropBoxFileTransferJob =
                new DropBoxFileTransferJob(
                    properties.getDestinationLocation(), resultMetaData, dropboxClient);
            threadPoolExecutor.execute(dropBoxFileTransferJob);
          }
          if (!result.getHasMore()) {
            break;
          }
        }
        result = dropboxClient.files().listFolderContinue(result.getCursor());
      }
      logger.info("Total no of files to be processed: " + listFolderResultsMetadata.size());

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
                dropBoxFileTransferJob.downloadFile(
                    dropboxClient,
                    failedFile,
                    loadPropertiesFile().getDestinationLocation().concat(failedFile),
                    true);
              }
            }
          }
          if (!finalFailedList.isEmpty()) {
            finalFailedList.stream()
                .map(finalTry -> "NOT PROCESSED FILE " + finalTry)
                .forEach(logger::error);
          }
          if (!noOfUpdatedFiles.isEmpty()) {
            noOfUpdatedFiles.stream()
                .map(updatedFiles -> "UPDATED FILE " + updatedFiles)
                .forEach(logger::warn);
          }
          Duration duration = Duration.between(startTime, LocalDateTime.now());
          logger.info(
              "Transfer Completed in "
                  + duration.toHours()
                  + " Hours/ "
                  + duration.toMinutes()
                  + " Minutes/ "
                  + duration.toMillis()
                  + " MilliSeconds");
          logger.info((noOfFiles.size() - finalFailedList.size() + " File/Files Processed "));
          logger.info(finalFailedList.size() + " File/Files Failed ");
          logger.info(noOfUpdatedFiles.size() + " File/Files Updated ");
          logger.info(noOfFilesDeleted.size() + " File/s Deleted");
          logger.info(noOfFoldersDeleted.size() + " Folder/s Deleted");
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
  public static DropBoxApplicationProperties loadPropertiesFile() {
    Properties prop;
    DropBoxApplicationProperties dropBoxApplicationProperties = new DropBoxApplicationProperties();
    try (InputStream inputStream =
        DropBoxFileTransfer.class.getClassLoader().getResourceAsStream("config.properties")) {
      prop = new Properties();
      if (Objects.isNull(inputStream)) {
        throw new IOException("Unable to find config.properties");
      }
      prop.load(inputStream);

      dropBoxApplicationProperties.setAccessToken(prop.getProperty("ACCESS_TOKEN").trim());
      dropBoxApplicationProperties.setClientIdentifier(
          prop.getProperty("CLIENT_IDENTIFIER").trim());
      dropBoxApplicationProperties.setSourceLocation(prop.getProperty("SOURCE_LOCATION").trim());
      dropBoxApplicationProperties.setIncludedDeleted(
          Boolean.parseBoolean(prop.getProperty("INCLUDE_DELETED").trim()));
      dropBoxApplicationProperties.setThreadPooSize(
          Integer.parseInt(prop.getProperty("THREAD_POOL_SIZE")));
      dropBoxApplicationProperties.setDestinationLocation(
          prop.getProperty("DESTINATION_LOCATION").trim());

    } catch (IOException ex) {
      logger.error(ex.getMessage(), ex);
      System.exit(0);
    }
    return dropBoxApplicationProperties;
  }
}
