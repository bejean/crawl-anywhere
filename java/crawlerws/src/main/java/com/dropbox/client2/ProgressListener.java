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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

/**
 * Receives file transfer progress updates for some API calls, e.g. getFile and
 * putFile.
 */
public abstract class ProgressListener {

    /**
     * Gets called when some bytes have been transferred since the last time it
     * was called and the progress interval has passed.
     *
     * @param bytes the number of bytes transferred.
     * @param total the size of the file in bytes.
     */
    public abstract void onProgress(long bytes, long total);

    /**
     * Should return how often transferred bytes should be reported to this
     * listener, in milliseconds. It is not guaranteed that updates will happen
     * at this exact interval, but that at least this amount of time will pass
     * between updates. The default implementation always returns 500
     * milliseconds.
     */
    public long progressInterval() {
        return 500;
    }

    /**
     * A wrapper for an {@link HttpEntity} that can count the number of bytes
     * transferred.  This is used internally to give updates for uploads.
     */
    public static class ProgressHttpEntity extends HttpEntityWrapper {

        private final ProgressListener listener;
        private final long length;

        public ProgressHttpEntity(final HttpEntity wrapped,
                final ProgressListener listener) {
            super(wrapped);
            this.listener = listener;
            length = wrapped.getContentLength();
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            wrappedEntity.writeTo(new CountingOutputStream(out));
        }

        private class CountingOutputStream extends FilterOutputStream {
            private long lastListened = 0;
            private long intervalMs = 0;
            private long transferred = 0;

            public CountingOutputStream(final OutputStream out) {
                super(out);
                intervalMs = listener.progressInterval();
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                // Using out.write instead of super.write speeds it up because
                // the superclass seems to default to byte-by-byte transfers.
                out.write(b, off, len);
                transferred += len;
                long now = System.currentTimeMillis();
                if (now - lastListened > intervalMs) {
                    lastListened = now;
                    listener.onProgress(this.transferred, length);
                }
            }

            @Override
            public void write(int b) throws IOException {
                super.write(b);
                this.transferred++;
                long now = System.currentTimeMillis();
                if (now - lastListened > intervalMs) {
                    lastListened = now;
                    listener.onProgress(this.transferred, length);
                }
            }
        }
    }
}
