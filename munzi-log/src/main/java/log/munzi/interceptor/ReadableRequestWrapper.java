package log.munzi.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * Request Servlet 에 담긴 내용을 열어보면 휘발되기 때문에
 * request 정보를 휘발되지 않게 한번 감싼 것
 */
@Slf4j
public class ReadableRequestWrapper extends HttpServletRequestWrapper {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    private static final int EOF = -1;

    private final Charset encoding;
    private byte[] rawData;
    private boolean read;


    /**
     * @param request          HttpServletRequest
     * @param reqSecretApiList body를 로그에 찍지 않을 api list (ex. POST /api/secret)
     * @param reqMaxSize       request body max size
     */
    public ReadableRequestWrapper(HttpServletRequest request, List<String> reqSecretApiList, String reqMaxSize) {
        super(request);
        String encoding = request.getCharacterEncoding();
        this.encoding = StringUtils.hasLength(encoding) ? Charset.forName(encoding) : StandardCharsets.UTF_8;
        try {
            InputStream is = request.getInputStream();

            if (reqMaxSize.isEmpty()) reqMaxSize = "1KB";
            if (request.getContentType() == null
                    || request.getContentType().contains("multipart/form-data")
                    || (reqSecretApiList != null && reqSecretApiList.contains(request.getMethod() + " " + request.getRequestURI()))
                    || request.getContentLengthLong() <= 0
                    || request.getContentLengthLong() > textSizeToByteSize(reqMaxSize)) {
                this.read = false;
            } else {
                this.read = true;
                this.rawData = toByteArray(is);
            }
        } catch (IOException e) {
            log.error("ReaderRequestWrapper에서 Stream을 열다가 IOException 발생", e);
        }
    }

    private static byte[] toByteArray(final InputStream input) throws IOException {
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            copyLarge(input, output, new byte[DEFAULT_BUFFER_SIZE]);
            return output.toByteArray();
        }
    }

    private static void copyLarge(final InputStream input, final OutputStream output,
                                  final byte[] buffer) throws IOException {
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (!read) return super.getRequest().getInputStream();

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.rawData);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // Do nothing
            }

            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(this.getInputStream(), this.encoding));
    }

    private double textSizeToByteSize(String size) {
        String[] sArray = {"BYTES", "KB", "MB", "GB", "TB", "PB"};
        size = size.toUpperCase();
        for (int i = 0; i < sArray.length; i++) {
            if (size.contains(sArray[i])) {
                String sizeNumber = size.replaceAll(" ", "").replaceAll(sArray[i], "");
                return Double.parseDouble(sizeNumber) * Math.pow(1024, i);
            }
        }

        return 0;
    }

    /**
     * @return log에 찍을지 여부
     */
    public boolean isRead() {
        return this.read;
    }
}
