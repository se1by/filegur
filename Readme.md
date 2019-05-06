# Filegur - File hosting as simple as Imgur

Upload a file, get a link to share, simple as that.
Optional downloads and request tracking. Add a proper GDPR notice and set the `filegur.gdprNoticeProvided` property in `application.conf` or the environment variable `GDPR_NOTICE_PROVIDED` to `true` to activate.

## Warning:
Don't give anyone an account you wouldn't trust with your latchkey as as of now there is no proper input sanitization!

## Setup:
A user `admin` with password `filegurdump` will be created automatically if no other user exists.
The same applies to the group `admin`. Users, groups, download metadata, request log and download statistics are saved  in an sqlite database called `filegur.db`.
The actual download files are saved in the subfolder `files` and named `<currentTimeMillis>-<fileName>.<fileExtension>`.

## Endpoints:

### /upload [Authenticated]
Upload a file. Replies with the download link once the upload is complete.

### /downloads/\<downloadID\> [Public]
Access to the download with the obfuscated name \<downloadID\>.
Tracks time, URI (without query), IP, username (if authenticated) and user agent if tracking is activated.

### /statistics/\<downloadID\> [Authenticated]
Access to said download statistics. Only accessible to the owner of the download as well as members of groups that have the `canViewAllDownloadStatistics` permission.

### /account [Authenticated]
Overview of the current users uploaded files.

### /account/settings [Authenticated]
Settings regarding the current user. Only "change password" at the moment.

### /admin/users [Authenticated]
User management (overview & creation as of now). Accessible to members whose groups have the `canCreateUsers` permission.

### /admin/groups [Authenticated]
Group management (overview & creation as of now). Accessible to members whose groups have the `canCreateGroups` permission.

### /admin/requestlog [Authenticated]
Overview of all requests to this filegur instance.
