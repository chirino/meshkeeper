package org.fusesource.meshkeeper.util.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author chirino
 */
public class IOSupport {

    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte buffer[] = new byte[1024 * 4];
        int c;
        while ((c = is.read(buffer)) > 0) {
            os.write(buffer, 0, c);
        }
    }

    public static void close(OutputStream os) throws IOException {
        try {
            os.close();
        } catch (Throwable e) {
        }
    }

    public static void close(InputStream is) {
        try {
            is.close();
        } catch (Throwable e) {
        }
    }    
    
}