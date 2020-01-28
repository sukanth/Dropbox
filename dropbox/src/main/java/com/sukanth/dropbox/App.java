package com.sukanth.dropbox;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;

public class App {
    private static final String ACCESS_TOKEN = "0sswgo6tiyQAAAAAAAEvJ3Qe11tAktcr_rWxu0RQu_0ndqM4hu5qsxFNqSstKikE";

    public static void main(String args[]) throws DbxException, FileNotFoundException, IOException {
        // Create Dropbox client
        DbxRequestConfig config = DbxRequestConfig.newBuilder("sukanth").build();
        DbxClientV2 dropboxClient = new DbxClientV2(config, ACCESS_TOKEN);
        try {
			listFiles(dropboxClient);
		} catch (Exception e) {
			e.printStackTrace();
		}
        FullAccount account = dropboxClient.users().getCurrentAccount();
        System.out.println(account.getName().getDisplayName());
        System.out.println(dropboxClient.users().getSpaceUsage());
        
        ListFolderResult result = dropboxClient.files().listFolder("");
        while (true) {
            for (Metadata metadata : result.getEntries()) {
                System.out.println(metadata.getPathLower());
            }
            if (!result.getHasMore()) {
                break;
            }
            result = dropboxClient.files().listFolderContinue(result.getCursor());
        } 
       // Upload "test.txt" to Dropbox
        try (InputStream in = new FileInputStream("test.txt")) {
            FileMetadata metadata = dropboxClient.files().uploadBuilder("/test.txt")
                .uploadAndFinish(in);
        }
      
        //download
        DbxDownloader<FileMetadata> downloader = dropboxClient.files().download("/test.txt");
        
        try {
            FileOutputStream out = new FileOutputStream("test.txt");
            downloader.download(out);
            out.close();
        } catch (DbxException ex) {
            System.out.println(ex.getMessage());
        }
        
       /* ListFolderBuilder listFolderBuilder = dropboxClient.files().listFolderBuilder("");
    	ListFolderResult result1 = listFolderBuilder.withRecursive(true).start();
    	
    	while (true) {

    		if (result1 != null) {
    			for ( Metadata entry : result1.getEntries()) {
    				if (entry instanceof FileMetadata){
    					System.out.println("Added file: "+entry.getPathLower());
    				}
    			}

    			if (!result1.getHasMore()) {
    				System.out.println("GET LATEST CURSOR");
    				return;
    			}

    			try {
    				result = dropboxClient.files().listFolderContinue(result.getCursor());
    			} catch (DbxException e) {
    				System.out.println("Couldn't get listFolderContinue");
    			}
    		}
    	}*/

    }
    public static List<String> listFiles(DbxClientV2 dropboxClient) throws Exception {
    	 List<String> files = new ArrayList<String>();
    	    ListFolderResult listFolderResult = dropboxClient.files().listFolder("");
    	    for (Metadata metadata : listFolderResult.getEntries()) {
    	        String name = metadata.getName();
    	        if (name.endsWith(".backup")) {
    	            files.add(name);
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
    }