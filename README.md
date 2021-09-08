# Dropbox File Transfer Job:
> **Transfer your files from dropbox NAS (Network attached storage) or  to your local file system.**

>Before you run this job go to the [config.properties](https://github.com/sukanth/Dropbox/blob/master/dropbox/src/main/resources/config.properties) file and configure the below properties and run the [DropBoxFileTransfer.java](https://github.com/sukanth/Dropbox/blob/master/dropbox/src/main/java/com/sukanth/dropbox/DropBoxFileTransfer.java) file.

> Logs are created under **../resources/logs**

Property              | Description
-------------         | -------------
ACCESS_TOKEN          | DropBox Access token
CLIENT_IDENTIFIER     | DropBox Client Identifier
SOURCE_LOCATION       | location of the dropbox folder from where you want to copy files
THREAD_POOL_SIZE      | mention the size of the thread pool depending upon the requirement (Used to speed up the process)
DESTINATION_LOCATION  | Absolute path of the  destination location.
INCLUDE_DELETED       | Boolean value True/false to include / not include deleted files on dropbox.


Any Questions can be redirected to [contact.sukanth@gmail.com](contact.sukanth@gmail.com)  
