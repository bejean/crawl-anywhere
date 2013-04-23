package com.dropbox.client2;

/**
 * Contains the SDK verison number.
 */
public final class SdkVersion {

    /** Returns the SDK version number. */
    public static String get() {
        return "1.3.1";  // Filled in by build process.
    }

    public static void main(String[] args) {
        System.out.println("Dropbox Java SDK, Version " + get());
    }
}
