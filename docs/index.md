## Introduction

This job can be leveraged to run when users want to take a backup/sync files that are present in dropbox account

## Purpose
The purpose of this is job is to transfer files from dropbox to the desired location on the host system / any NAS on the host network. 

## Execution
>Before you run this job go to the [config.properties](https://github.com/sukanth/Dropbox/blob/master/dropbox/src/main/resources/config.properties) file and configure the below properties and run the [DropBoxFileTransfer.java](https://github.com/sukanth/Dropbox/blob/master/dropbox/src/main/java/com/sukanth/dropbox/DropBoxFileTransfer.java) file.

> Logs are created under **../resources/logs**

Property              | Description
-------------         | -------------
ACCESS_TOKEN          | DropBox Access token
CLIENT_IDENTIFIER     | DropBox Client Identifier
SOURCE_LOCATION       | location of the dropbox folder from where you want to copy files from
THREAD_POOL_SIZE      | mention the size of the thread pool depending upon the requirement (Used to speed up the process)
DESTINATION_LOCATION  | Absolute path of the  destination location.
INCLUDE_DELETED       | Boolean value True/false to include / not include deleted files on dropbox.

### Support or Contact

Having trouble with something? questions can be redirected to [contact.sukanth@gmail.com](contact.sukanth@gmail.com)  

