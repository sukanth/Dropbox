package com.sukanth.dropbox;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.RateLimitException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

/**
 * @author sukanthgunda
 * @apiNote Thread to copy files from dropbox to desired location.
 */
public class DropBoxFileTransferJob implements Runnable {
  private final String destinationLocation;
  private final ListFolderResult result;
  private final DbxClientV2 dropBoxClient;
  private static Logger logger = Logger.getLogger(DropBoxFileTransferJob.class);

  public DropBoxFileTransferJob(
      String destinationLocation, ListFolderResult result, DbxClientV2 dropBoxClient) {
    this.destinationLocation = destinationLocation;
    this.result = result;
    this.dropBoxClient = dropBoxClient;
  }

  @Override
  public void run() {
    try {
      logger.info("Processing Thread Entries " + result.getEntries().size());
      result
          .getEntries()
          .forEach(
              entry -> {
                if (entry instanceof FileMetadata) {
                  File destinationFilePath =
                      new File(destinationLocation.concat(entry.getPathDisplay()));
                  if (!destinationFilePath.exists()) {
                    try {
                      DropBoxFileTransfer.noOfFiles.add(entry.getPathDisplay());
                      downloadFile(
                          dropBoxClient,
                          entry.getPathLower(),
                          destinationFilePath.getAbsolutePath(),
                          false);
                    } catch (Exception e) {
                      logger.error(e);
                    }
                  } else {
                    updateFileOrSkipIfExists(entry, destinationFilePath);
                  }
                } else if (entry instanceof FolderMetadata) {
                  File destFolderPath =
                      new File(destinationLocation.concat(entry.getPathDisplay()));
                  if (!destFolderPath.exists()) {
                    boolean mkdirs = destFolderPath.mkdirs();
                    if (mkdirs) {
                      logger.info("Created Folder " + destFolderPath.getAbsolutePath());
                    } else {
                      logger.error("Can't create folder " + destFolderPath.getAbsolutePath());
                    }
                  } else {
                    logger.info(
                        "Folder Exists, skipping folder creation "
                            + destFolderPath.getAbsolutePath());
                  }
                } else if (entry instanceof DeletedMetadata) {
                  File destinationPathToDelete =
                          new File(destinationLocation.concat(entry.getPathDisplay()));
                  cleanUpDeletedFiles(destinationPathToDelete);
                }
                  else {
                  logger.error("Neither a file not a folder / deleted metadata" + entry.getPathLower());
                }
              });
    } catch (Exception e) {
      logger.error(e);
    }
  }

  /**
   * @param destinationPathToDelete
   * @see -deletes files/folders in the host machine that are not in sync with the dropbox.
   */
  private void cleanUpDeletedFiles(File destinationPathToDelete) {
    if (destinationPathToDelete.exists()) {
      if (destinationPathToDelete.isFile()) {
        destinationPathToDelete.delete();
        logger.info("Deleted File: " + destinationPathToDelete.getAbsolutePath());
      } else if (destinationPathToDelete.isDirectory()) {
        try {
          FileUtils.deleteDirectory(destinationPathToDelete);
          logger.info("Deleted Folder: " + destinationPathToDelete.getAbsolutePath());
        } catch (IOException e) {
          logger.error(e);
        }
      } else {
        logger.error("Neither a file not a folder to delete: " + destinationPathToDelete.getAbsolutePath());
      }
    }
  }

  /**
   * @param entry
   * @param destinationFilePath
   * @see -updates the file if the server version of file is latest otherwise skips downloading it-
   */
  private void updateFileOrSkipIfExists(Metadata entry, File destinationFilePath) {
    Date localDate;
    Date serverDate;
    SimpleDateFormat sdf;
    try {
      sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
      localDate = sdf.parse(sdf.format(destinationFilePath.lastModified()));
      serverDate = sdf.parse(sdf.format(((FileMetadata) entry).getClientModified()));
      if (serverDate.after(localDate)) {
        downloadFile(
            dropBoxClient, entry.getPathLower(), destinationFilePath.getAbsolutePath(), false);
        DropBoxFileTransfer.noOfUpdatedFiles.add(entry.getPathDisplay());
        logger.info("File Updated " + destinationFilePath.getAbsolutePath());
      } else {
        logger.info("File Exists, skipped file " + destinationFilePath.getAbsolutePath());
      }
    } catch (ParseException e) {
      logger.error(
          "ERROR PARSING localDate and serverDate while processing " + entry.getPathLower(), e);
    } catch (Exception e) {
      logger.error(e);
    }
  }

  /**
   * Download Dropbox File to Local Computer
   *
   * @param client Current connected client
   * @param dropBoxFilePath The file path on the Dropbox cloud server -> [/foldername/something.txt]
   * @param localFileAbsolutePath The absolute file path of the File on the Local File System
   */
  public void downloadFile(
      DbxClientV2 client, String dropBoxFilePath, String localFileAbsolutePath, boolean retry) {
    try (FileOutputStream fOut = new FileOutputStream(localFileAbsolutePath)) {
      if (retry) {
        File destPath = new File(localFileAbsolutePath);
        if (!destPath.exists()) {
          boolean mkdirs = destPath.mkdirs();
          if (mkdirs) {
            logger.info("Created Path " + destPath.getAbsolutePath());
          } else {
            logger.error("Can't create Path " + destPath.getAbsolutePath());
          }
        }
      }
      DbxDownloader<FileMetadata> dl = client.files().download(dropBoxFilePath);
      logger.info(
          "*** Downloading File from.... "
              + dropBoxFilePath
              + " ...To... "
              + localFileAbsolutePath
              + " ***");
      dl.download(fOut);
      fOut.flush();
    } catch (RateLimitException e) {
      try {
        logger.info(
            "Waiting... "
                + e.getBackoffMillis()
                + " MilliSeconds for RateLimit Backoff "
                + dropBoxFilePath);
        DropBoxFileTransfer.failed.add(dropBoxFilePath);
        Thread.sleep(e.getBackoffMillis());
        logger.info("Resuming Wait..");
      } catch (InterruptedException ex) {
        logger.error("ERROR : Thread Interrupted", e);
        Thread.currentThread().interrupt();
      }
    } catch (Exception e) {
      if (retry) {
        logger.error(
            "ERROR PROCESSING RETRY : From "
                + dropBoxFilePath
                + " ...To... "
                + localFileAbsolutePath,
            e);
      }
      if (!retry) {
        DropBoxFileTransfer.failed.add(dropBoxFilePath);
      } else {
        DropBoxFileTransfer.finalFailedList.add(dropBoxFilePath);
      }
    }
  }
}
