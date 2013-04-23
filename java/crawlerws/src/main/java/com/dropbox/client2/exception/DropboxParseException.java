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


package com.dropbox.client2.exception;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Indicates there was trouble parsing a response from Dropbox.
 */
public class DropboxParseException extends DropboxException {
    private static final long serialVersionUID = 1L;

    /*
     * Takes a BufferedReader so it can be reset back to the beginning and read
     * again into the body variable.
     */
    public DropboxParseException(BufferedReader reader) {
        super("failed to parse: " + stringifyBody(reader));
    }

    public static String stringifyBody(BufferedReader reader) {
        String inputLine = null;

        try {
            if (reader != null) {
                reader.reset();
            }
        } catch (IOException ioe) {
        }
        StringBuffer result = new StringBuffer();
        try {
            while ((inputLine = reader.readLine()) != null) {
                result.append(inputLine);
            }
        } catch (IOException e) {
        }

        return result.toString();
    }

    public DropboxParseException(String message) {
        super(message);
    }
}
