package com.sukanth.dropbox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

public class DropBox {
	
	private static final String ACCESS_TOKEN = "0sswgo6tiyQAAAAAAAEvJ3Qe11tAktcr_rWxu0RQu_0ndqM4hu5qsxFNqSstKikE";
	public static void main(String[] args) {
		List<String> files = null;
		String folderPath = "/Trash";
		DbxRequestConfig config = DbxRequestConfig.newBuilder("sukanth").build();
        DbxClientV2 dropboxClient = new DbxClientV2(config, ACCESS_TOKEN);
        try {
        	downloadFile(dropboxClient, "/Trash/IMG_4330.JPG", "/Users/sukanthgunda/Desktop/IMG_4330.JPG");
			files = listFiles(dropboxClient,folderPath);
			for(int i=0;i<files.size();i++) {
				System.out.println(files.get(i).toString());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param dropboxClient
	 * @param folderPath
	 * @return
	 * @throws Exception
	 */
	 public static List<String> listFiles(DbxClientV2 dropboxClient,String folderPath) throws Exception {
    	 List<String> files = new ArrayList<String>();
    	    ListFolderResult listFolderResult = dropboxClient.files().listFolder(folderPath);
    	    for (Metadata metadata : listFolderResult.getEntries()) {
    	        String filepath = metadata.getPathDisplay();
    	        File file = new File(filepath);
    	        if (file.exists()) {
    	            files.add(filepath);
    	        }
    	    }
    	    Collections.sort(files, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    return s2.compareTo(s1);
                }
            });
            return files;
        } 
	 
	 /**
	  * Download Dropbox File to Local Computer
	  * 
	  * @param client
	  *            Current connected client
	  * @param dropBoxFilePath
	  *            The file path on the Dropbox cloud server -> [/foldername/something.txt]
	  * @param localFileAbsolutePath
	  *            The absolute file path of the File on the Local File System
	  * @throws DbxException
	  * @throws DownloadErrorException
	  * @throws IOException
	  */
	  public static void downloadFile(DbxClientV2 client , String dropBoxFilePath , String localFileAbsolutePath) throws DownloadErrorException , DbxException , IOException {
	 				
	 	//Create DbxDownloader
	 	DbxDownloader<FileMetadata> dl = client.files().download(dropBoxFilePath);
	 				
	 	//FileOutputStream
	 	FileOutputStream fOut = new FileOutputStream(localFileAbsolutePath);
	 	System.out.println("Downloading .... " + dropBoxFilePath);
	 				
	 				
	 	//Add a progress Listener
	 	dl.download(new ProgressOutputStream(fOut, dl.getResult().getSize(), (long completed , long totalSize) -> {

	 		System.out.println( ( completed * 100 ) / totalSize + " %");
	 					
	 	}));
	 }
}
