/*
 * Copyright (c) 2009-2011 Dropbox, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.dropbox.client2;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.SyncFailedException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import com.dropbox.client2.ProgressListener.ProgressHttpEntity;
import com.dropbox.client2.RESTUtility.RequestMethod;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxFileSizeException;
import com.dropbox.client2.exception.DropboxIOException;
import com.dropbox.client2.exception.DropboxLocalStorageFullException;
import com.dropbox.client2.exception.DropboxParseException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.jsonextract.JsonExtractionException;
import com.dropbox.client2.jsonextract.JsonExtractor;
import com.dropbox.client2.jsonextract.JsonList;
import com.dropbox.client2.jsonextract.JsonMap;
import com.dropbox.client2.jsonextract.JsonThing;
import com.dropbox.client2.session.Session;

/**
 * Location of the Dropbox API functions.
 *
 * The class is parameterized with the type of session it uses. This will be
 * the same as the type of session you pass into the constructor.
 */
public class DropboxAPI<SESS_T extends Session> {

    /**
     * The version of the API that this code uses.
     */
    public static final int VERSION = 1;

    /**
     * The version of this Dropbox SDK.
     */
    public static final String SDK_VERSION = SdkVersion.get();

    /**
     * The max upload file size that Dropbox servers can handle, in bytes.
     */
    public static final long MAX_UPLOAD_SIZE = 180 * 1024 * 1024; // 180MiB

    protected static final int METADATA_DEFAULT_LIMIT = 25000;
    private static final int REVISION_DEFAULT_LIMIT = 1000;
    private static final int SEARCH_DEFAULT_LIMIT = 10000;
    private static final int UPLOAD_SO_TIMEOUT_MS = 3 * 60 * 1000; // 3 minutes

    protected final SESS_T session;

    public DropboxAPI(SESS_T session) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null.");
        }
        this.session = session;
    }

    /**
     * Information about a user's account.
     */
    public static class Account implements Serializable {

        private static final long serialVersionUID = 2097522622341535732L;

        /** The user's ISO country code. */
        public final String country;

        /** The user's "real" name. */
        public final String displayName;

        /** The user's quota, in bytes. */
        public final long quota;

        /** The user's quota excluding shared files. */
        public final long quotaNormal;

        /** The user's quota of shared files. */
        public final long quotaShared;

        /** The user's account ID. */
        public final long uid;

        /** The url the user can give to get referral credit. */
        public final String referralLink;

        /**
         * Creates an account from a Map.
         *
         * @param map a Map that looks like:
         * <pre>
         * {"country": "",
         *  "display_name": "John Q. User",
         *  "quota_info": {
         *    "shared": 37378890,
         *    "quota": 62277025792,
         *    "normal": 263758550
         *   },
         *  "uid": "174"}
         * </pre>
         */
        protected Account(Map<String, Object> map) {
            country = (String) map.get("country");
            displayName = (String) map.get("display_name");
            uid = getFromMapAsLong(map, "uid");
            referralLink = (String) map.get("referral_link");

            Object quotaInfo = map.get("quota_info");
            @SuppressWarnings("unchecked")
            Map<String, Object> quotamap = (Map<String, Object>) quotaInfo;
            quota = getFromMapAsLong(quotamap, "quota");
            quotaNormal = getFromMapAsLong(quotamap, "normal");
            quotaShared = getFromMapAsLong(quotamap, "shared");
        }

        /**
         * Creates an account object from an initial set of values.
         */
        protected Account(String country, String displayName,
                long uid, String referralLink, long quota, long quotaNormal,
                long quotaShared) {
            this.country = country;
            this.displayName = displayName;
            this.uid = uid;
            this.referralLink = referralLink;
            this.quota = quota;
            this.quotaNormal = quotaNormal;
            this.quotaShared = quotaShared;
        }
    }

    /**
     * A metadata entry that describes a file or folder.
     */
    public static class Entry {

        /** Size of the file. */
        public long bytes;

        /**
         * If a directory, the hash is its "current version". If the hash
         * changes between calls, then one of the directory's immediate
         * children has changed.
         */
        public String hash;

        /**
         * Name of the icon to display for this entry. Corresponds to filenames
         * (without an extension) in the icon library available at
         * https://www.dropbox.com/static/images/dropbox-api-icons.zip.
         */
        public String icon;

        /** True if this entry is a directory, or false if it's a file. */
        public boolean isDir;

        /**
         * Last modified date, in "EEE, dd MMM yyyy kk:mm:ss ZZZZZ" form (see
         * {@code RESTUtility#parseDate(String)} for parsing this value.
         */
        public String modified;

        /**
         * For a file, this is the modification time set by the client when
         * the file was added to Dropbox.  Since this time is not verified (the
         * Dropbox server stores whatever the client sends up) this should only
         * be used for display purposes (such as sorting) and not, for example,
         * to determine if a file has changed or not.
         *
         * <p>
         * This is not set for folders.
         * </p>
         */
        public String clientMtime;

        /** Path to the file from the root. */
        public String path;

        /**
         * Name of the root, usually either "dropbox" or "app_folder".
         */
        public String root;

        /**
         * Human-readable (and localized, if possible) description of the
         * file size.
         */
        public String size;

        /** The file's MIME type. */
        public String mimeType;

        /**
         * Full unique ID for this file's revision. This is a string, and not
         * equivalent to the old revision integer.
         */
        public String rev;

        /** Whether a thumbnail for this is available. */
        public boolean thumbExists;

        /**
         * Whether this entry has been deleted but not removed from the
         * metadata yet. Most likely you'll only want to show entries with
         * isDeleted == false.
         */
        public boolean isDeleted;

        /** A list of immediate children if this is a directory. */
        public List<Entry> contents;

        /**
         * Creates an entry from a map, usually received from the metadata
         * call. It's unlikely you'll want to create these yourself.
         *
         * @param map the map representation of the JSON received from the
         *         metadata call, which should look like this:
         * <pre>
         * {
         *    "hash": "528dda36e3150ba28040052bbf1bfbd1",
         *    "thumb_exists": false,
         *    "bytes": 0,
         *    "modified": "Sat, 12 Jan 2008 23:10:10 +0000",
         *    "path": "/Public",
         *    "is_dir": true,
         *    "size": "0 bytes",
         *    "root": "dropbox",
         *    "contents": [
         *    {
         *        "thumb_exists": false,
         *        "bytes": 0,
         *        "modified": "Wed, 16 Jan 2008 09:11:59 +0000",
         *        "path": "/Public/\u2665asdas\u2665",
         *        "is_dir": true,
         *        "icon": "folder",
         *        "size": "0 bytes"
         *    },
         *    {
         *        "thumb_exists": false,
         *        "bytes": 4392763,
         *        "modified": "Thu, 15 Jan 2009 02:52:43 +0000",
         *        "path": "/Public/\u540d\u79f0\u672a\u8a2d\u5b9a\u30d5\u30a9\u30eb\u30c0.zip",
         *        "is_dir": false,
         *        "icon": "page_white_compressed",
         *        "size": "4.2MB"
         *    }
         *    ],
         *    "icon": "folder_public"
         * }
         * </pre>
         */
        @SuppressWarnings("unchecked")
        public Entry(Map<String, Object> map) {
            bytes = getFromMapAsLong(map, "bytes");
            hash = (String) map.get("hash");
            icon = (String) map.get("icon");
            isDir = getFromMapAsBoolean(map, "is_dir");
            modified = (String) map.get("modified");
            clientMtime = (String) map.get("client_mtime");
            path = (String) map.get("path");
            root = (String) map.get("root");
            size = (String) map.get("size");
            mimeType = (String) map.get("mime_type");
            rev = (String) map.get("rev");
            thumbExists = getFromMapAsBoolean(map, "thumb_exists");
            isDeleted = getFromMapAsBoolean(map, "is_deleted");

            Object json_contents = map.get("contents");
            if (json_contents != null && json_contents instanceof JSONArray) {
                contents = new ArrayList<Entry>();
                Object entry;
                Iterator<?> it = ((JSONArray) json_contents).iterator();
                while (it.hasNext()) {
                    entry = it.next();
                    if (entry instanceof Map) {
                        contents.add(new Entry((Map<String, Object>) entry));
                    }
                }
            } else {
                contents = null;
            }
        }

        public Entry() {
        }

        /**
         * Returns the file name if this is a file (the part after the last
         * slash in the path).
         */
        public String fileName() {
            int ind = path.lastIndexOf('/');
            return path.substring(ind + 1, path.length());
        }

        /**
         * Returns the path of the parent directory if this is a file.
         */
        public String parentPath() {
            if (path.equals("/")) {
                return "";
            } else {
                int ind = path.lastIndexOf('/');
                return path.substring(0, ind + 1);
            }
        }

        public static final JsonExtractor<Entry> JsonExtractor = new JsonExtractor<Entry>() {
            public Entry extract(JsonThing jt) throws JsonExtractionException {
                return new Entry(jt.expectMap().internal);
            }
        };
    }

    /**
     * Contains info describing a downloaded file.
     */
    public static final class DropboxFileInfo {

        private String mimeType = null;
        private long fileSize = -1;
        private String charset = null;
        private Entry metadata = null;

        // fileSize and metadata are guaranteed to be valid if the constructor
        // doesn't throw an exception.
        private DropboxFileInfo(HttpResponse response) throws DropboxException {
            metadata = parseXDropboxMetadata(response);
            if (metadata == null) {
                throw new DropboxParseException("Error parsing metadata.");
            }

            fileSize = parseFileSize(response, metadata);
            if (fileSize == -1) {
                throw new DropboxParseException("Error determining file size.");
            }

            // Parse mime type and charset.
            Header contentType = response.getFirstHeader("Content-Type");
            if (contentType != null) {
                String contentVal = contentType.getValue();
                if (contentVal != null) {
                    String[] splits = contentVal.split(";");
                    if (splits.length > 0) {
                        mimeType = splits[0].trim();
                    }
                    if (splits.length > 1) {
                        splits = splits[1].split("=");
                        if (splits.length > 1) {
                            charset = splits[1].trim();
                        }
                    }
                }
            }
        }

        /**
         * Parses the JSON in the the 'x-dropbox-metadata' header field of the
         * http response.
         *
         * @param response The http response for the downloaded file.
         * @return An Entry object based on the metadata JSON. Can be null if
         * metadata isn't available.
         */
        private static Entry parseXDropboxMetadata(HttpResponse response) {
            if (response == null) {
                return null;
            }

            Header xDropboxMetadataHeader =
                response.getFirstHeader("X-Dropbox-Metadata");
            if (xDropboxMetadataHeader == null) {
                return null;
            }

            // Returns null if the parsing fails.
            String json = xDropboxMetadataHeader.getValue();
            Object metadata = JSONValue.parse(json);
            if (metadata == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String,Object> map = (Map<String,Object>) metadata;
            return new Entry(map);
        }

        /**
         * Determines the size of the downloaded file.
         *
         * @param response The http response for the file whose size we're
         * interested in.
         * @param metadata The metadata associated with the file. Can be null if
         * unavailable.
         * @return The determined file size. -1 if the size of the file can't be
         * determined.
         */
        private static long parseFileSize(HttpResponse response,
                                          Entry metadata) {
            // Use the response's content-length, if available (negative if
            // unavailable).
            long contentLength = response.getEntity().getContentLength();
            if (contentLength >= 0) {
                return contentLength;
            }

            // Fall back on the metadata, if available.
            if (metadata != null) {
                return metadata.bytes;
            }

            return -1;
        }

        /**
         * Returns the MIME type of the associated file, or null if it is
         * unknown.
         */
        public final String getMimeType() {
            return mimeType;
        }

        /**
         * @deprecated Replaced by {@link #getFileSize()}
         */
        @Deprecated
        public final long getContentLength() {
            return getFileSize();
        }

        /**
         * Returns the size of the file in bytes (always >= 0).
         */
        public final long getFileSize() {
            return fileSize;
        }

        /**
         * Returns the charset of the associated file, or null if it is
         * unknown.
         */
        public final String getCharset() {
            return charset;
        }

        /**
         * Returns the metadata of the associated file (always non-null).
         */
        public final Entry getMetadata() {
            return metadata;
        }
    }

    /**
     * An {@link InputStream} for a file download that includes the associated
     * {@link DropboxFileInfo}. Closing this stream will cancel the associated
     * download request.
     */
    public static class DropboxInputStream extends FilterInputStream {

        private final HttpUriRequest request;
        private final DropboxFileInfo info;

        public DropboxInputStream(HttpUriRequest request,
                HttpResponse response) throws DropboxException {
            // Give the FilterInputStream a null stream at first so we can
            // handle errors better.
            super(null);

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new DropboxException("Didn't get entity from HttpResponse");
            }

            // Now set the input stream on FilterInputStream. This will throw
            // an IOException itself if something goes wrong.
            try {
                in = entity.getContent();
            } catch (IOException e) {
                throw new DropboxIOException(e);
            }

            this.request = request;
            info = new DropboxFileInfo(response);
        }

        /**
         * Closes this stream and aborts the request to Dropbox, releasing
         * any associated resources. No more bytes will be downloaded after
         * this is called.
         *
         * @throws IOException if an error occurs while closing this stream.
         */
        @Override
        public void close() throws IOException {
            // Aborting the request also closes the input stream that it
            // creates (the in variable). Do not try to close it again.
            request.abort();
        }

        /**
         * Returns the {@link DropboxFileInfo} for the associated file.
         */
        public DropboxFileInfo getFileInfo() {
            return info;
        }


        /**
         * Copies from a {@link DropboxInputStream} to an
         * {@link OutputStream}, optionally providing updates via a
         * {@link ProgressListener}. You probably won't have a use for this
         * function because most API functions that return a
         * {@link DropboxInputStream} have an alternate that will copy to an
         * {@link OutputStream} for you.
         *
         * @param os the stream to copy to.
         * @param listener an optional {@link ProgressListener} to receive progress
         *         updates as the stream is copied, or null.
         *
         * @throws DropboxPartialFileException if only part of the input stream was
         *         copied.
         * @throws DropboxIOException for network-related errors.
         * @throws DropboxLocalStorageFullException if there is no more room to
         *         write to the output stream.
         * @throws DropboxException for any other unknown errors. This is also a
         *         superclass of all other Dropbox exceptions, so you may want to
         *         only catch this exception which signals that some kind of error
         *         occurred.
         */
        public void copyStreamToOutput(OutputStream os, ProgressListener listener)
                throws DropboxIOException, DropboxPartialFileException,
                DropboxLocalStorageFullException {
            BufferedOutputStream bos = null;
            long totalRead = 0;
            long lastListened = 0;
            long length = info.getFileSize();

            try {
                bos = new BufferedOutputStream(os);

                byte[] buffer = new byte[4096];
                int read;
                while (true) {
                    read = read(buffer);
                    if (read < 0) {
                        if (length >= 0 && totalRead < length) {
                            // We've reached the end of the file, but it's unexpected.
                            throw new DropboxPartialFileException(totalRead);
                        }
                        // TODO check for partial success, if possible
                        break;
                    }

                    bos.write(buffer, 0, read);

                    totalRead += read;

                    if (listener != null) {
                        long now = System.currentTimeMillis();
                        if (now - lastListened > listener.progressInterval()) {
                            lastListened = now;
                            listener.onProgress(totalRead, length);
                        }
                    }
                }

                bos.flush();
                os.flush();
                // Make sure it's flushed out to disk
                try {
                    if (os instanceof FileOutputStream) {
                        ((FileOutputStream)os).getFD().sync();
                    }
                } catch (SyncFailedException e) {
                }

            } catch (IOException e) {
                String message = e.getMessage();
                if (message != null && message.startsWith("No space")) {
                    // This is a hack, but it seems to be the only way to check
                    // which exception it is.
                    throw new DropboxLocalStorageFullException();
                } else {
                    /*
                     * If the output stream was closed, we notify the caller
                     * that only part of the file was copied. This could have
                     * been because this request is being intentionally
                     * canceled.
                     */
                    throw new DropboxPartialFileException(totalRead);
                }
            } finally {
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException e) {}
                }
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {}
                }
                // This will also abort/finish the request if the download is
                // canceled early.
                try {
                    close();
                } catch (IOException e) {}
            }
        }
    }

    /**
     * A request to upload a file to Dropbox.  This request can be canceled
     * by calling abort().
     */
    public interface UploadRequest {
        /**
         * Aborts the request. The original call to upload() will throw a
         * {@link DropboxPartialFileException}.
         */
        public void abort();

        /**
         * Executes the request.
         *
         * @return an {@link Entry} representing the uploaded file.
         *
         * @throws DropboxPartialFileException if the request was canceled
         *         before completion.
         * @throws DropboxServerException if the server responds with an error
         *         code. See the constants in {@link DropboxServerException} for
         *         the meaning of each error code. The most common error codes
         *         you can expect from this call are 404 (path to upload not
         *         found), 507 (user over quota), and 400 (unexpected parent
         *         rev).
         * @throws DropboxIOException if any network-related error occurs.
         * @throws DropboxException for any other unknown errors. This is also a
         *         superclass of all other Dropbox exceptions, so you may want to
         *         only catch this exception which signals that some kind of error
         *         occurred.
         */
        public Entry upload() throws DropboxException;
    }


    protected static final class BasicUploadRequest implements UploadRequest {
        private final HttpUriRequest request;
        private final Session session;

        public BasicUploadRequest(HttpUriRequest request, Session session) {
            this.request = request;
            this.session = session;
        }

        /**
         * Aborts the request. The original call to upload() will throw a
         * {@link DropboxPartialFileException}.
         */
        public void abort() {
            request.abort();
        }

        /**
         * Executes the request.
         *
         * @return an {@link Entry} representing the uploaded file.
         *
         * @throws DropboxPartialFileException if the request was canceled
         *         before completion.
         * @throws DropboxServerException if the server responds with an error
         *         code. See the constants in {@link DropboxServerException} for
         *         the meaning of each error code. The most common error codes
         *         you can expect from this call are 404 (path to upload not
         *         found), 507 (user over quota), and 400 (unexpected parent
         *         rev).
         * @throws DropboxIOException if any network-related error occurs.
         * @throws DropboxException for any other unknown errors. This is also a
         *         superclass of all other Dropbox exceptions, so you may want to
         *         only catch this exception which signals that some kind of error
         *         occurred.
         */
        public Entry upload() throws DropboxException {
            HttpResponse hresp;
            try {
                hresp = RESTUtility.execute(session, request,
                        UPLOAD_SO_TIMEOUT_MS);
            } catch (DropboxIOException e) {
                if (request.isAborted()) {
                    throw new DropboxPartialFileException(-1);
                } else {
                    throw e;
                }
            }

            Object resp = RESTUtility.parseAsJSON(hresp);

            @SuppressWarnings("unchecked")
            Map<String, Object> ret = (Map<String, Object>) resp;

            return new Entry(ret);
        }
    }

    /**
     * Holds an {@link HttpUriRequest} and the associated {@link HttpResponse}.
     */
    public static final class RequestAndResponse {
        /** The request */
        public final HttpUriRequest request;
        /** The response */
        public final HttpResponse response;

        protected RequestAndResponse(HttpUriRequest request, HttpResponse response) {
            this.request = request;
            this.response = response;
        }
    }

    /**
     * Contains a link to a Dropbox stream or share and its expiration date.
     */
    public static class DropboxLink {
        /** The url it links to */
        public final String url;
        /** When the url expires (after which this link will no longer work) */
        public final Date expires;

        private DropboxLink(String returl, boolean secure) {
            if (!secure && returl.startsWith("https://")) {
                returl = returl.replaceFirst("https://", "http://");
                returl = returl.replaceFirst(":443/", "/");
            }
            url = returl;
            expires = null;
        }

        private DropboxLink(Map<String, Object> map) {
            this(map, true);
        }

        /**
         * Creates a DropboxLink, with security optionally set to false.
         * This is useful for some clients, such as Android, which use
         * this to play a streaming audio or video file, and which are
         * unable to play from streaming https links.
         *
         * @param map the parsed parameters returned from Dropbox
         * @param secure if false, returns an http link
         */
        private DropboxLink(Map<String, Object> map, boolean secure) {
            String returl = (String)map.get("url");
            String exp = (String)map.get("expires");
            if (exp != null) {
                expires = RESTUtility.parseDate(exp);
            } else {
                expires = null;
            }

            if (!secure && returl.startsWith("https://")) {
                returl = returl.replaceFirst("https://", "http://");
                returl = returl.replaceFirst(":443/", "/");
            }
            url = returl;
        }
    }

    /**
     * Represents the size of thumbnails that the API can return.
     */
    public enum ThumbSize {
        /**
         * Will have at most a 32 width or 32 height, maintaining its
         * original aspect ratio.
         */
        ICON_32x32("small"),
        /** 64 width or 64 height, with original aspect ratio. */
        ICON_64x64("medium"),
        /** 128 width or 128 height, with original aspect ratio. */
        ICON_128x128("large"),
        /** 256 width or 256 height, with original aspect ratio. */
        ICON_256x256("256x256"),
        /**
         * Will either fit within a 320 x 240 rectangle or a
         * 240 x 320 rectangle, whichever results in a larger image.
         */
        BESTFIT_320x240("320x240_bestfit"),
        /** Fits within 480x320 or 320x480 */
        BESTFIT_480x320("480x320_bestfit"),
        /** Fits within 640x480 or 480x640 */
        BESTFIT_640x480("640x480_bestfit"),
        /** Fits within 960x640 or 640x960 */
        BESTFIT_960x640("960x640_bestfit"),
        /** Fits within 1024x768 or 768x1024 */
        BESTFIT_1024x768("1024x768_bestfit");

        private String size;

        ThumbSize(String size) {
            this.size = size;
        }

        public String toAPISize() {
            return size;
        }
    }

    /**
     * Represents the image format of thumbnails that the API can return.
     */
    public enum ThumbFormat {
        PNG, JPEG
    }

    /**
     * Returns the {@link Session} that this API is using.
     */
    public SESS_T getSession() {
        return session;
    }

    /**
     * Returns the {@link Account} associated with the current {@link Session}.
     *
     * @return the current session's {@link Account}.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code.
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public Account accountInfo() throws DropboxException {
        assertAuthenticated();

        @SuppressWarnings("unchecked")
        Map<String, Object> accountInfo =
                (Map<String, Object>) RESTUtility.request(RequestMethod.GET,
                session.getAPIServer(), "/account/info", VERSION,
                new String[] {"locale", session.getLocale().toString()},
                session);

        return new Account(accountInfo);
    }

    /**
     * Downloads a file from Dropbox, copying it to the output stream. Returns
     * the {@link DropboxFileInfo} for the file.
     *
     * @param path the Dropbox path to the file.
     * @param rev the revision (from the file's metadata) of the file to
     *         download, or null to get the latest version.
     * @param os the {@link OutputStream} to write the file to.
     * @param listener an optional {@link ProgressListener} to receive progress
     *         updates as the file downloads, or null.
     *
     * @return the {@link DropboxFileInfo} for the downloaded file.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error codes you
     *         can expect from this call are 404 (path not found) and 400 (bad
     *         rev).
     * @throws DropboxPartialFileException if a network error occurs during the
     *         download.
     * @throws DropboxIOException for some network-related errors.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public DropboxFileInfo getFile(String path, String rev, OutputStream os,
            ProgressListener listener) throws DropboxException {
        DropboxInputStream dis = getFileStream(path, rev);
        dis.copyStreamToOutput(os, listener);
        return dis.getFileInfo();
    }

    /**
     * Downloads a file from Dropbox. Returns a {@link DropboxInputStream} via
     * which the file contents can be read from the network. You must close the
     * stream when you're done with it to release all resources.
     *
     * You can also cancel the download by closing the returned
     * {@link DropboxInputStream} at any time.
     *
     * @param path the Dropbox path to the file.
     * @param rev the revision (from the file's metadata) of the file to
     *         download, or null to get the latest version.
     *
     * @return a {@link DropboxInputStream} from which to read the file
     *         contents. The contents are retrieved from the network and not
     *         stored locally.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error codes you
     *         can expect from this call are 404 (path not found) and 400 (bad
     *         rev).
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public DropboxInputStream getFileStream(String path, String rev)
            throws DropboxException {
        assertAuthenticated();

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        String url = "/files/" + session.getAccessType() + path;
        String[] args = new String[] {
                "rev", rev,
                "locale", session.getLocale().toString(),
        };
        String target = RESTUtility.buildURL(session.getContentServer(),
                VERSION, url, args);
        HttpGet req = new HttpGet(target);
        session.sign(req);

        HttpResponse response = RESTUtility.execute(session, req);

        return new DropboxInputStream(req, response);
    }

    /**
     * Uploads a file to Dropbox. The upload will not overwrite any existing
     * version of the file, unless the latest version on the Dropbox server
     * has the same rev as the parentRev given. Pass in null if you're expecting
     * this to create a new file.  Note: use {@code putFileRequest()} if you want
     * to be able to cancel the upload.
     *
     * @param path the full Dropbox path where to put the file, including
     *         directories and filename.
     * @param is the {@link InputStream} from which to upload.
     * @param length the amount of bytes to read from the {@link InputStream}.
     * @param parentRev the rev of the file at which the user started editing
     *         it (obtained from a metadata call), or null if this is a new
     *         upload. If null, or if it does not match the latest rev on the
     *         server, a copy of the file will be created and you'll receive
     *         the new metadata upon executing the request.
     * @param listener an optional {@link ProgressListener} to receive upload
     *         progress updates, or null.
     *
     * @return a metadata {@link Entry} representing the uploaded file.
     *
     * @throws IllegalArgumentException if the file does not exist.
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxFileSizeException if the file is bigger than the
     *         maximum allowed by the API. See
     *         {@code DropboxAPI.MAX_UPLOAD_SIZE}.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error codes you
     *         can expect from this call are 404 (path to upload not found),
     *         507 (user over quota), and 400 (unexpected parent rev).
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public Entry putFile(String path, InputStream is, long length,
            String parentRev, ProgressListener listener)
            throws DropboxException {
        UploadRequest request =
            putFileRequest(path, is, length, parentRev, listener);
        return request.upload();
    }

    /**
     * Creates a request to upload a file to Dropbox, which you can then
     * {@code upload()} or {@code abort()}. The upload will not overwrite any
     * existing version of the file, unless the latest version has the same rev
     * as the parentRev given.  Pass in null if you're expecting this to create
     * a new file.
     *
     * @param path the full Dropbox path where to put the file, including
     *         directories and filename.
     * @param is the {@link InputStream} from which to upload.
     * @param length the amount of bytes to read from the {@link InputStream}.
     * @param parentRev the rev of the file at which the user started editing
     *         it (obtained from a metadata call), or null if this is a new
     *         upload. If null, or if it does not match the latest rev on the
     *         server, a copy of the file will be created and you'll receive
     *         the new metadata upon executing the request.
     * @param listener an optional {@link ProgressListener} to receive upload
     *         progress updates, or null.
     *
     * @return an {@link UploadRequest}.
     *
     * @throws IllegalArgumentException if the file does not exist.
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxFileSizeException if the file is bigger than the
     *         maximum allowed by the API. See
     *         {@code DropboxAPI.MAX_UPLOAD_SIZE}.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public UploadRequest putFileRequest(String path, InputStream is,
            long length, String parentRev, ProgressListener listener)
            throws DropboxException {
        return putFileRequest(path, is, length, false,
                parentRev, listener);
    }

    /**
     * Uploads a file to Dropbox. The upload will overwrite any existing
     * version of the file. Use {@code putFileRequest()} if you want to be able
     * to cancel the upload.  If you expect the user to be able
     * to edit a file remotely and locally, then conflicts may arise and
     * you won't want to use this call: see {@code putFileRequest} instead.
     *
     * @param path the full Dropbox path where to put the file, including
     *         directories and filename.
     * @param is the {@link InputStream} from which to upload.
     * @param length the amount of bytes to read from the {@link InputStream}.
     * @param listener an optional {@link ProgressListener} to receive upload
     *         progress updates, or null.
     *
     * @return a metadata {@link Entry} representing the uploaded file.
     *
     * @throws IllegalArgumentException if the file does not exist.
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxFileSizeException if the file is bigger than the
     *         maximum allowed by the API. See
     *         {@code DropboxAPI.MAX_UPLOAD_SIZE}.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error codes you
     *         can expect from this call are 404 (path to upload not found),
     *         507 (user over quota), and 400 (unexpected parent rev).
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public Entry putFileOverwrite(String path, InputStream is, long length,
            ProgressListener listener) throws DropboxException {
        UploadRequest request = putFileOverwriteRequest(path, is, length,
                listener);
        return request.upload();
    }

    /**
     * Creates a request to upload a file to Dropbox, which you can then
     * {@code upload()} or {@code abort()}. The upload will overwrite any
     * existing version of the file.  If you expect the user to be able
     * to edit a file remotely and locally, then conflicts may arise and
     * you won't want to use this call: see {@code putFileRequest} instead.
     *
     * @param path the full Dropbox path where to put the file, including
     *         directories and filename.
     * @param is the {@link InputStream} from which to upload.
     * @param length the amount of bytes to read from the {@link InputStream}.
     * @param listener an optional {@link ProgressListener} to receive upload
     *         progress updates, or null.
     *
     * @return an {@link UploadRequest}.
     *
     * @throws IllegalArgumentException if the file does not exist locally.
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxFileSizeException if the file is bigger than the
     *         maximum allowed by the API. See
     *         {@code DropboxAPI.MAX_UPLOAD_SIZE}.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public UploadRequest putFileOverwriteRequest(String path, InputStream is,
            long length, ProgressListener listener) throws DropboxException {
        return putFileRequest(path, is, length, true, null, listener);
    }

    /**
     * Downloads a thumbnail from Dropbox, copying it to the output stream.
     * Returns the {@link DropboxFileInfo} for the downloaded thumbnail.
     *
     * @param path the Dropbox path to the file for which you want to get a
     *         thumbnail.
     * @param os the {@link OutputStream} to write the thumbnail to.
     * @param size the size of the thumbnail to download.
     * @param format the image format of the thumbnail to download.
     * @param listener an optional {@link ProgressListener} to receive progress
     *         updates as the thumbnail downloads, or null.
     *
     * @return the {@link DropboxFileInfo} for the downloaded thumbnail.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error codes you
     *         can expect from this call are 404 (path not found or can't be
     *         thumbnailed), 415 (this type of file can't be thumbnailed), and
     *         500 (internal error while creating thumbnail).
     * @throws DropboxPartialFileException if a network error occurs during the
     *         download.
     * @throws DropboxIOException for some network-related errors.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public DropboxFileInfo getThumbnail(String path, OutputStream os,
            ThumbSize size, ThumbFormat format, ProgressListener listener)
            throws DropboxException {
        DropboxInputStream thumb = getThumbnailStream(path, size, format);

        thumb.copyStreamToOutput(os, listener);

        return thumb.getFileInfo();
    }

    /**
     * Downloads a thumbnail from Dropbox. Returns a {@link DropboxInputStream}
     * via which the thumbnail can be read from the network. You must close the
     * stream when you're done with it to release all resources.
     *
     * You can also cancel the thumbnail download by closing the returned
     * {@link DropboxInputStream} at any time.
     *
     * @param path the Dropbox path to the file for which you want to get a
     *         thumbnail.
     * @param size the size of the thumbnail to download.
     * @param format the image format of the thumbnail to download.
     *
     * @return a {@link DropboxInputStream} from which to read the thumbnail.
     *         The contents are retrieved from the network and not stored
     *         locally.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error codes you
     *         can expect from this call are 404 (path not found or can't be
     *         thumbnailed), 415 (this type of file can't be thumbnailed), and
     *         500 (internal error while creating thumbnail)
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public DropboxInputStream getThumbnailStream(String path, ThumbSize size,
            ThumbFormat format) throws DropboxException {
        assertAuthenticated();

        String target = "/thumbnails/" + session.getAccessType() + path;
        String[] params = {"size", size.toAPISize(),
                           "format", format.toString(),
                           "locale", session.getLocale().toString()};
        RequestAndResponse rr = RESTUtility.streamRequest(RequestMethod.GET,
                session.getContentServer(), target, VERSION, params, session);

        return new DropboxInputStream(rr.request, rr.response);
    }

    /**
     * Returns the metadata for a file, or for a directory and (optionally) its
     * immediate children.
     *
     * @param path the Dropbox path to the file or directory for which to get
     *         metadata.
     * @param fileLimit the maximum number of children to return for a
     *         directory. Default is 25,000 if you pass in 0 or less. If there
     *         are too many entries to return, you will get a 406
     *         {@link DropboxServerException}. Pass in 1 if getting metadata
     *         for a file.
     * @param hash if you previously got metadata for a directory and have it
     *         stored, pass in the returned hash. If the directory has not
     *         changed since you got the hash, a 304
     *         {@link DropboxServerException} will be thrown. Pass in null for
     *         files or unknown directories.
     * @param list if true, returns metadata for a directory's immediate
     *         children, or just the directory entry itself if false. Ignored
     *         for files.
     * @param rev optionally gets metadata for a file at a prior rev (does not
     *         apply to folders). Use null for the latest metadata.
     *
     * @return a metadata {@link Entry}.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error codes you
     *         can expect from this call are 304 (contents haven't changed
     *         based on the hash), 404 (path not found or unknown rev for
     *         path), and 406 (too many entries to return).
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public Entry metadata(String path, int fileLimit, String hash,
            boolean list, String rev) throws DropboxException {
        assertAuthenticated();

        if (fileLimit <= 0) {
            fileLimit = METADATA_DEFAULT_LIMIT;
        }

        String[] params = {
                "file_limit", String.valueOf(fileLimit),
                "hash", hash,
                "list", String.valueOf(list),
                "rev", rev,
                "locale", session.getLocale().toString()
        };

        String url_path = "/metadata/" + session.getAccessType() + path;

        @SuppressWarnings("unchecked")
        Map<String, Object> dirinfo =
                (Map<String, Object>) RESTUtility.request(RequestMethod.GET,
                session.getAPIServer(), url_path, VERSION, params, session);

        return new Entry(dirinfo);
    }

    /**
     * Returns a list of metadata for all revs of the path.
     *
     * @param path the Dropbox path to the file for which to get revisions
     *         (directories are not supported).
     * @param revLimit the maximum number of revisions to return. Default is
     *         1,000 if you pass in 0 or less, and 1,000 is the most that will
     *         ever be returned.
     *
     * @return a list of metadata entries.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error code you
     *         can expect from this call is 404 (no revisions found for path).
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    @SuppressWarnings("unchecked")
    public List<Entry> revisions(String path, int revLimit)
            throws DropboxException {
        assertAuthenticated();

        if (revLimit <= 0) {
            revLimit = REVISION_DEFAULT_LIMIT;
        }

        String[] params = {
                "rev_limit", String.valueOf(revLimit),
                "locale", session.getLocale().toString()
        };

        String url_path = "/revisions/" + session.getAccessType() + path;

        JSONArray revs =
                (JSONArray)RESTUtility.request(RequestMethod.GET,
                session.getAPIServer(), url_path, VERSION, params, session);

        List<Entry> entries = new LinkedList<Entry>();
        for (Object metadata : revs) {
            entries.add(new Entry((Map<String, Object>)metadata));
        }

        return entries;
    }

    /**
     * Searches a directory for entries matching the query.
     *
     * @param path the Dropbox directory to search in.
     * @param query the query to search for (minimum 3 characters).
     * @param fileLimit the maximum number of file entries to return. Default
     *         is 10,000 if you pass in 0 or less, and 1,000 is the most that
     *         will ever be returned.
     * @param includeDeleted whether to include deleted files in search
     *         results.
     *
     * @return a list of metadata entries of matching files.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code.
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public List<Entry> search(String path, String query, int fileLimit,
            boolean includeDeleted) throws DropboxException {
        assertAuthenticated();

        if (fileLimit <= 0) {
            fileLimit = SEARCH_DEFAULT_LIMIT;
        }

        String target = "/search/" + session.getAccessType() + path;

        String[] params = {
                "query", query,
                "file_limit", String.valueOf(fileLimit),
                "include_deleted", String.valueOf(includeDeleted),
                "locale", session.getLocale().toString()
        };

        Object response = RESTUtility.request(RequestMethod.GET,
                session.getAPIServer(), target, VERSION, params, session);

        ArrayList<Entry> ret = new ArrayList<Entry>();
        if (response instanceof JSONArray) {
            JSONArray jresp = (JSONArray)response;
            for (Object next: jresp) {
                if (next instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Entry ent = new Entry((Map<String, Object>)next);
                    ret.add(ent);
                }
            }
        }

        return ret;
    }

    /**
     * Moves a file or folder (and all of the folder's contents) from one path
     * to another.
     *
     * @param fromPath the Dropbox path to move from.
     * @param toPath the full Dropbox path to move to (not just a directory).
     *
     * @return a metadata {@link Entry}.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error codes you
     *         can expect from this call are 403 (operation is forbidden), 404
     *         (path not found), and 507 (user over quota).
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public Entry move(String fromPath, String toPath)
            throws DropboxException {
        assertAuthenticated();

        String[] params = {"root", session.getAccessType().toString(),
                           "from_path", fromPath,
                           "to_path", toPath,
                           "locale", session.getLocale().toString()};

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>)RESTUtility.request(
                RequestMethod.POST,
                session.getAPIServer(), "/fileops/move", VERSION,
                params, session);

        return new Entry(resp);
    }

    /**
     * Copies a file or folder (and all of the folder's contents) from one path
     * to another.
     *
     * @param fromPath the Dropbox path to copy from.
     * @param toPath the full Dropbox path to copy to (not just a directory).
     *
     * @return a metadata {@link Entry}.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error codes you
     *         can expect from this call are 403 (operation is forbidden), 404
     *         (path not found), and 507 (user over quota).
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public Entry copy(String fromPath, String toPath)
            throws DropboxException {
        assertAuthenticated();

        String[] params = {"root", session.getAccessType().toString(),
                           "from_path", fromPath,
                           "to_path", toPath,
                           "locale", session.getLocale().toString()};

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>)RESTUtility.request(
                RequestMethod.POST,
                session.getAPIServer(), "/fileops/copy", VERSION,
                params, session);

        return new Entry(resp);
    }

    /**
     * Creates a new Dropbox folder.
     *
     * @param path the Dropbox path to the new folder.
     *
     * @return a metadata {@link Entry} for the new folder.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error codes you
     *         can expect from this call are 403 (something already exists at
     *         that path), 404 (path not found), and 507 (user over quota).
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public Entry createFolder(String path) throws DropboxException {
        assertAuthenticated();

        String[] params = {"root", session.getAccessType().toString(),
                           "path", path,
                           "locale", session.getLocale().toString()};

        @SuppressWarnings("unchecked")
        Map<String, Object> resp = (Map<String, Object>) RESTUtility.request(
                RequestMethod.POST, session.getAPIServer(),
                "/fileops/create_folder", VERSION, params, session);

        return new Entry(resp);
    }

    /**
     * Deletes a file or folder (and all of the folder's contents). After
     * deletion, metadata calls may still return this file or folder for some
     * time, but the metadata {@link Entry}'s {@code isDeleted} attribute will
     * be set to {@code true}.
     *
     * @param path the Dropbox path to delete.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error code you
     *         can expect from this call is 404 (path not found).
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public void delete(String path) throws DropboxException {
        assertAuthenticated();

        String[] params = {"root", session.getAccessType().toString(),
                           "path", path,
                           "locale", session.getLocale().toString()};

        RESTUtility.request(RequestMethod.POST, session.getAPIServer(),
                "/fileops/delete", VERSION, params, session);
    }

    /**
     * Restores a file to a previous rev.
     *
     * @param path the Dropbox path to the file to restore.
     * @param rev the rev to restore to (obtained from a metadata or revisions
     *         call).
     *
     * @return a metadata {@link Entry} for the newly restored file.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error code you
     *         can expect from this call is 404 (path not found or unknown
     *         revision).
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public Entry restore(String path, String rev) throws DropboxException {
        assertAuthenticated();

        String[] params = {
                "rev", rev,
                "locale", session.getLocale().toString()
        };

        String target = "/restore/" + session.getAccessType() + path;

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata =
                (Map<String, Object>)RESTUtility.request(RequestMethod.GET,
                session.getAPIServer(), target, VERSION, params, session);

        return new Entry(metadata);
    }

    /**
     * Returns a {@link DropboxLink} for a stream of the given file path (for
     * streaming media files).
     *
     * @param path the Dropbox path of the file for which to get a streaming
     *         link.
     * @param ssl whether the streaming URL is https or http.  Some Android
     *         and other platforms won't play https streams, so false converts
     *         the link to an http link before returning it.
     *
     * @return a {@link DropboxLink} for streaming the file.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error code you
     *         can expect from this call is 404 (path not found).
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public DropboxLink media(String path, boolean ssl)
            throws DropboxException {
        assertAuthenticated();
        String target = "/media/" + session.getAccessType() + path;

        @SuppressWarnings("unchecked")
        Map<String, Object> map =
                (Map<String, Object>)RESTUtility.request(RequestMethod.GET,
                        session.getAPIServer(), target, VERSION,
                        new String[] {"locale", session.getLocale().toString()},
                        session);

        return new DropboxLink(map, ssl);
    }

    /**
     * Generates a {@link DropboxLink} for sharing the specified directory or
     * file.
     *
     * @param path the Dropbox path to share, either a directory or file.
     *
     * @return a {@link DropboxLink} for the path.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxServerException if the server responds with an error
     *         code. See the constants in {@link DropboxServerException} for
     *         the meaning of each error code. The most common error code you
     *         can expect from this call is 404 (path not found).
     * @throws DropboxIOException if any network-related error occurs.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public DropboxLink share(String path) throws DropboxException {
        assertAuthenticated();

        String target = "/shares/" + session.getAccessType() + path;

        @SuppressWarnings("unchecked")
        Map<String, Object> map =
                (Map<String, Object>)RESTUtility.request(RequestMethod.GET,
                        session.getAPIServer(), target, VERSION,
                        new String[] {"locale", session.getLocale().toString()},
                        session);

        String url = (String)map.get("url");
        Date expires = RESTUtility.parseDate((String)map.get("expires"));

        if (url == null || expires == null) {
            throw new DropboxParseException("Could not parse share response.");
        }

        return new DropboxLink(map);
    }

    /**
     * Helper function to read boolean JSON return values
     *
     * @param map
     *            the one to read from
     * @param name
     *            the parameter name to read
     * @return the value, with false as a default if no parameter set
     */
    protected static boolean getFromMapAsBoolean(Map<String, Object> map, String name) {
        Object val = map.get(name);
        if (val != null && val instanceof Boolean) {
            return ((Boolean) val).booleanValue();
        } else {
            return false;
        }
    }

    /**
     * Creates a request to upload an {@link InputStream} to a Dropbox file.
     * You can then {@code upload()} or {@code abort()} this request. This is
     * the advanced version, which you should only use if you really need the
     * flexibility of uploading using an {@link InputStream}.
     *
     * @param path the full Dropbox path where to put the file, including
     *         directories and filename.
     * @param is the {@link InputStream} from which to upload.
     * @param length the amount of bytes to read from the {@link InputStream}.
     * @param overwrite whether to overwrite the file if it already exists. If
     *         true, any existing file will always be overwritten. If false,
     *         files will be overwritten only if the {@code parentRev} matches
     *         the current rev on the server or otherwise a conflicted copy of
     *         the file will be created and you will get the new file's
     *         metadata {@link Entry}.
     * @param parentRev the rev of the file at which the user started editing
     *         it (obtained from a metadata call), or null if this is a new
     *         upload. If null, or if it does not match the latest rev on the
     *         server, a copy of the file will be created and you'll receive
     *         the new metadata upon executing the request.
     * @param listener an optional {@link ProgressListener} to receive upload
     *         progress updates, or null.
     *
     * @return an {@link UploadRequest}.
     *
     * @throws IllegalArgumentException if {@code newFilename} is null or
     *         empty.
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxFileSizeException if the file is bigger than the
     *         maximum allowed by the API. See
     *         {@code DropboxAPI.MAX_UPLOAD_SIZE}.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    private UploadRequest putFileRequest(String path, InputStream is,
            long length, boolean overwrite, String parentRev,
            ProgressListener listener) throws DropboxException {
        if (path == null || path.equals("")) {
            throw new IllegalArgumentException("path is null or empty.");
        }

        assertAuthenticated();

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        String target = "/files_put/" + session.getAccessType() + path;

        if (parentRev == null) {
            parentRev = "";
        }

        String[] params = new String[] {
                "overwrite", String.valueOf(overwrite),
                "parent_rev", parentRev,
                "locale", session.getLocale().toString()
        };

        String url = RESTUtility.buildURL(session.getContentServer(), VERSION,
                target, params);

        HttpPut req = new HttpPut(url);
        session.sign(req);

        InputStreamEntity isEntity = new InputStreamEntity(is, length);
        isEntity.setContentEncoding("application/octet-stream");
        isEntity.setChunked(false);

        HttpEntity entity = isEntity;

        if (listener != null) {
            entity = new ProgressHttpEntity(entity, listener);
        }

        req.setEntity(entity);

        return new BasicUploadRequest(req, session);
    }

    /**
     * A way of letting you keep up with changes to files and folders in a user's
     * Dropbox.  You can periodically call this function to get a list of "delta
     * entries", which are instructions on how to update your local state to match
     * the server's state.
     *
     * @param cursor
     *     On the first call, you should pass in <code>null</code>.  On subsequent
     *     calls, pass in the {@link DeltaPage#cursor cursor} returned by the previous
     *     call.
     *
     * @return
     *     A single {@link DeltaPage DeltaPage} of results.  The {@link DeltaPage#hasMore hasMore}
     *     field will tell you whether the server has more pages of results to return.
     *     If the server doesn't have more results, you can wait a bit (say,
     *     5 or 10 minutes) and poll again.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public DeltaPage<Entry> delta(String cursor) throws DropboxException {
        String[] params = new String[] {
            "cursor", cursor,
            "locale", session.getLocale().toString(),
        };

        Object json = RESTUtility.request(RequestMethod.POST,
                session.getAPIServer(), "/delta", VERSION, params, session);
        try {
            return DeltaPage.extractFromJson(new JsonThing(json), Entry.JsonExtractor);
        } catch (JsonExtractionException ex) {
            throw new DropboxParseException("Error parsing /delta results: " + ex.getMessage());
        }
    }

    /**
     * A page of {@link DeltaEntry DeltaEntry}s (returned by {@link #delta delta}).
     */
    public static final class DeltaPage<MD> {
        /**
         * If <code>true</code>, then you should reset your local state to be an empty
         * folder before processing the list of delta entries.  This is only <code>true</code>
         * in rare situations.
         */
        public final boolean reset;

        /**
         * A string that is used to keep track of your current state.  On the next call to
         * {@link #delta delta}, pass in this value to pick up where you left off.
         */
        public final String cursor;

        /**
         * Apply these entries to your local state to catch up with the Dropbox server's state.
         */
        public final List<DeltaEntry<MD>> entries;

        /**
         * If <code>true</code>, then there are more entries available; you can call {@link
         * #delta delta} again immediately to retrieve those entries.  If <code>false</code>,
         * then wait at least 5 minutes (preferably longer) before checking again.
         */
        public final boolean hasMore;

        public DeltaPage(boolean reset, List<DeltaEntry<MD>> entries, String cursor, boolean hasMore) {
            this.reset = reset;
            this.entries = entries;
            this.cursor = cursor;
            this.hasMore = hasMore;
        }

        public static <MD> DeltaPage<MD> extractFromJson(JsonThing j, JsonExtractor<MD> entryExtractor) throws JsonExtractionException {
            JsonMap m = j.expectMap();
            boolean reset = m.get("reset").expectBoolean();
            String cursor = m.get("cursor").expectString();
            boolean hasMore = m.get("has_more").expectBoolean();
            List<DeltaEntry<MD>> entries = m.get("entries").expectList().extract(new DeltaEntry.JsonExtractor<MD>(entryExtractor));

            return new DeltaPage<MD>(reset, entries, cursor, hasMore);
        }

    }

    /**
     * A single entry in a {@link DeltaPage DeltaPage}.
     */
    public static final class DeltaEntry<MD> {
        /**
         * The lower-cased path of the entry.  Dropbox compares file paths in a
         * case-insensitive manner.  For example, an entry for <code>"/readme.txt"</code>
         * should overwrite the entry for <code>"/ReadMe.TXT"</code>.
         *
         * <p>
         * To get the original case-preserved path, look in the {@link #metadata metadata} field.
         * </p>
         */
        public final String lcPath;

        /**
         * If this is <code>null</code>, it means that this path doesn't exist on
         * on Dropbox's copy of the file system.  To update your local state to
         * match, delete whatever is at that path, including any children.
         * If your local state doesn't have anything at this path, ignore this entry.
         *
         * <p>
         * If this is not <code>null</code>, it means that Dropbox has a file/folder
         * at this path with the given metadata.  To update your local state to match,
         * add the entry to your local state as well.
         * </p>
         * <ul>
         * <li>
         *     If the path refers to parent folders that don't exist yet in your local
         *     state, create those parent folders in your local state.
         * </li>
         * <li>
         *     If the metadata is for a file, replace whatever your local state has at
         *     that path with the new entry.
         * </li>
         * <li>
         *     If the metadata is for a folder, check what your local state has at the
         *     path.  If it's a file, replace it with the new entry.  If it's a folder,
         *     apply the new metadata to the folder, but do not modify the folder's
         *     children.
         * </li>
         * </ul>
         */
        public final MD metadata;

        public DeltaEntry(String lcPath, MD metadata) {
            this.lcPath = lcPath;
            this.metadata = metadata;
        }

        public static final class JsonExtractor<MD> extends com.dropbox.client2.jsonextract.JsonExtractor<DeltaEntry<MD>> {
            public final com.dropbox.client2.jsonextract.JsonExtractor<MD> mdExtractor;

            public JsonExtractor(com.dropbox.client2.jsonextract.JsonExtractor<MD> mdExtractor) {
                this.mdExtractor = mdExtractor;
            }

            public DeltaEntry<MD> extract(JsonThing j) throws JsonExtractionException {
                return extract(j, this.mdExtractor);
            }

            public static <MD> DeltaEntry<MD> extract(JsonThing j, com.dropbox.client2.jsonextract.JsonExtractor<MD> mdExtractor) throws JsonExtractionException {
                JsonList l = j.expectList();
                String path = l.get(0).expectString();
                MD metadata = l.get(1).optionalExtract(mdExtractor);
                return new DeltaEntry<MD>(path, metadata);
            }
        }

    }

    /**
     * Creates a reference to a path that can be used with {@link #addFromCopyRef
     * addFromCopyRef()} to copy the contents of the file at that path to a
     * different Dropbox account.  This is more efficient than copying the content
     *
     * @param sourcePath
     *     The full path to the file that you want a
     *
     * @return
     *     A string representation of the file pointer.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public CreatedCopyRef createCopyRef(String sourcePath) throws DropboxException {
        assertAuthenticated();

        if (!sourcePath.startsWith("/")) {
            throw new IllegalArgumentException("'sourcePath' must start with \"/\": " + sourcePath);
        }

        String[] params = {
            "locale", session.getLocale().toString()
        };

        String url_path = "/copy_ref/" + session.getAccessType() + sourcePath;

        @SuppressWarnings("unchecked")
        Object result = RESTUtility.request(RequestMethod.GET,
                session.getAPIServer(), url_path, VERSION, params, session);

        try {
            return CreatedCopyRef.extractFromJson(new JsonThing(result));
        } catch (JsonExtractionException ex) {
            throw new DropboxParseException("Error parsing /copy_ref results: " + ex.getMessage());
        }
    }

    public static final class CreatedCopyRef
    {
        public final String copyRef;
        public final String expiration;

        public CreatedCopyRef(String copyRef, String expiration) {
            this.copyRef = copyRef;
            this.expiration = expiration;
        }

        public static CreatedCopyRef extractFromJson(JsonThing j) throws JsonExtractionException {
            JsonMap m = j.expectMap();
            String string = m.get("copy_ref").expectString();
            String expiration = m.get("expires").expectString();
            return new CreatedCopyRef(string, expiration);
        }
    }

    /**
     * Creates a file in the Dropbox that the client is currently connected to, using
     * the contents from a {@link com.dropbox.client2.DropboxAPI.CreatedCopyRef CopyRef} created with
     * {@link #createCopyRef createCopyRef()}.  The {@link CreatedCopyRef CreatedCopyRef}
     * can be for a file in a different Dropbox account.
     *
     * @param sourceCopyRef
     *     The copy-ref to use as the source of the file data (comes from
     *     {@link CreatedCopyRef#copyRef CreatedCopyRef.copyRef}, which is created
     *     through {@link #createCopyRef createCopyRef()}).
     * @param targetPath
     *     The path that you want to create the file at.
     *
     * @return
     *     The {@link Entry} for the new file.
     *
     * @throws DropboxUnlinkedException if you have not set an access token
     *         pair on the session, or if the user has revoked access.
     * @throws DropboxException for any other unknown errors. This is also a
     *         superclass of all other Dropbox exceptions, so you may want to
     *         only catch this exception which signals that some kind of error
     *         occurred.
     */
    public Entry addFromCopyRef(String sourceCopyRef, String targetPath) throws DropboxException {
        assertAuthenticated();

        if (!targetPath.startsWith("/")) {
            throw new IllegalArgumentException("'targetPath' doesn't start with \"/\": " + targetPath);
        }

        String[] params = {
            "locale", session.getLocale().toString(),
            "root", session.getAccessType().toString(),
            "from_copy_ref", sourceCopyRef,
            "to_path", targetPath,
        };

        String url_path = "/fileops/copy";

        @SuppressWarnings("unchecked")
        Map<String, Object> dirinfo =
            (Map<String, Object>) RESTUtility.request(RequestMethod.GET,
                session.getAPIServer(), url_path, VERSION, params, session);

        return new Entry(dirinfo);
    }

    /**
     * Throws a {@link DropboxUnlinkedException} if the session in this
     * instance is not linked.
     */
    protected void assertAuthenticated() throws DropboxUnlinkedException {
        if (!session.isLinked()) {
            throw new DropboxUnlinkedException();
        }
    }

    /**
     * Helper function to read long JSON return values
     *
     * @param map
     *            the one to read from
     * @param name
     *            the parameter name to read
     * @return the value, with 0 as a default if no parameter set
     */
    protected static long getFromMapAsLong(Map<String, Object> map, String name) {
        Object val = map.get(name);
        long ret = 0;
        if (val != null) {
            if (val instanceof Number) {
                ret = ((Number) val).longValue();
            } else if (val instanceof String) {
                // To parse cases where JSON can't represent a Long, so
                // it's stored as a string
                ret = Long.parseLong((String)val, 16);
            }
        }
        return ret;
    }

}
