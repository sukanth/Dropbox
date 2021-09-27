package com.sukanth.dropbox;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import org.apache.log4j.Logger;

/**
 * @author Sukanth Gunda.
 * @see - Class to resolve content hash for file based on dropbox hashing algorithm
 */
public class LocalContentHashResolver {
  private static Logger logger = Logger.getLogger(LocalContentHashResolver.class);

  static final char[] HEX_DIGITS =
      new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

  /**
   * @param data
   * @return
   */
  public static String hex(byte[] data) {
    char[] buf = new char[2 * data.length];
    int i = 0;
    for (byte b : data) {
      buf[i++] = HEX_DIGITS[(b & 0xf0) >>> 4];
      buf[i++] = HEX_DIGITS[b & 0x0f];
    }
    return new String(buf);
  }

  /**
   * @param filePath
   * @return
   * @throws IOException
   * @see - Method to generate dropbox content hash for local file based on Dropbox content hash
   *     Algorithm
   */
  public static synchronized String generateLocalContentHash(String filePath)
      throws FileNotFoundException {
    MessageDigest contentHasher = new DropboxContentHasher();
    byte[] buf = new byte[1024];
    InputStream in = new FileInputStream(filePath);
    try {
      while (true) {
        int n = in.read(buf);
        if (n < 0) break; // EOF
        contentHasher.update(buf, 0, n);
      }
    } catch (Exception e) {
      logger.error("Something went wrong while generating local hash ", e);
    } finally {
      try {
        in.close();
      } catch (IOException ioException) {
        logger.error(ioException);
      }
    }
    return hex(contentHasher.digest());
  }
}
