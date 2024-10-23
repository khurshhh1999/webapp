package configs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private byte[] cachedBody;
    private BufferedReader reader;
    private ServletInputStream inputStream;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
    }

    public void setCachedBody(byte[] cachedBody) {
        this.cachedBody = cachedBody;
        // Reset stream and reader
        this.inputStream = null;
        this.reader = null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (inputStream == null) {
            inputStream = new CachedBodyServletInputStream(cachedBody);
        }
        return inputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (reader == null) {
            reader = new BufferedReader(new InputStreamReader(getInputStream()));
        }
        return reader;
    }
}