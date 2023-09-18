package log.munzi.interceptor;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.CharArrayWriter;
import java.io.PrintWriter;

/**
 * Response Servlet 에 담긴 내용을 열어보면 휘발되기 때문에
 * Response 정보를 휘발되지 않게 한번 감싼 것
 */
public class MunziResponseWrapper extends HttpServletResponseWrapper {
    private CharArrayWriter charArrayWriter = new CharArrayWriter();
    private PrintWriter writer = new PrintWriter(charArrayWriter);

    /**
     * @param response HttpServletResponse
     */
    public MunziResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public PrintWriter getWriter() {
        return writer;
    }

    /**
     * response 내용 조회
     *
     * @return response content
     */
    public String getContent() {
        return charArrayWriter.toString();
    }
}

