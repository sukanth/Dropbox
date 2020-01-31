package com.sukanth.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderBuilder;
import com.dropbox.core.v2.files.ListFolderContinueErrorException;
import com.dropbox.core.v2.files.ListFolderErrorException;
import com.dropbox.core.v2.files.ListFolderResult;

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
        try{
            DbxClientV2 dropboxClient = authenticate();
            listFolderBuilder = dropboxClient.files().listFolderBuilder(sourceLocation);
            result = listFolderBuilder.withIncludeDeleted(false).withRecursive(true).withIncludeMediaInfo(false).start();
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(50);
            while(true){
                if(Objects.nonNull(result)){
                    if(result.getHasMore()){
                        DropBoxFileTransferJob dropBoxFileTransferJob = new DropBoxFileTransferJob(destinationLocation,result,dropboxClient);
                        threadPoolExecutor.execute(dropBoxFileTransferJob);
                    }else{
                        break;
                    }
                }
                result = dropboxClient.files().listFolderContinue(result.getCursor());
            }
        }
        catch (ListFolderContinueErrorException e) {
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
    public static DbxClientV2 authenticate() {
        String ACCESS_TOKEN = null;
        DbxRequestConfig config = null;
        try {
            ACCESS_TOKEN = "0sswgo6tiyQAAAAAAAEvJ3Qe11tAktcr_rWxu0RQu_0ndqM4hu5qsxFNqSstKikE";
            config = DbxRequestConfig.newBuilder("sukanth").build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new DbxClientV2(config, ACCESS_TOKEN);
    }
}
