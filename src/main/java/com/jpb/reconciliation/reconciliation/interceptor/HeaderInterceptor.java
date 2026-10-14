package com.jpb.reconciliation.reconciliation.interceptor;
//import com.jfs.security.sb.identifiertoken.client.IdentifierTokenClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

//@Component
@Slf4j
public class HeaderInterceptor implements HandlerInterceptor {

    @Value("${disable-appid-token-validation: false}")
    private boolean disableAppIdTokenValidation;

    private static byte[] keyBytes;

    @Value("${apigw.publicKey.key}")
    private String keyPath;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (request.getDispatcherType() == DispatcherType.REQUEST && !disableAppIdTokenValidation) {

            String appIDToken = request.getHeader("appIDToken");
            String traceId = request.getHeader("x-trace-id");
            String channelId = request.getHeader("x-channel-id");
            
            log.info("Type: {}, URI: {}, channelId: {}, TraceID: {}", request.getDispatcherType(),
                    request.getRequestURI(), channelId, traceId);
            
            if (StringUtils.isEmpty(appIDToken)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Invalid or missing appIDToken header");
                return false;
            }

            if (keyBytes == null || keyBytes.length == 0) {
                try {
                    keyBytes = getFileByteArray(keyPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            
            boolean isAppIdTokenValid = false;
            try {

//                isAppIdTokenValid = IdentifierTokenClient.isTokenValid(appIDToken, keyBytes, channelId);

                log.info("isAppIdTokenValid ==> :{}", isAppIdTokenValid);

                if (!isAppIdTokenValid) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("Invalid or expired app id token");
                    return false;
                }

                return true;

            } catch (Exception e) {
                log.error(e.getMessage(), e);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(e.getMessage());
                return false;
            }
        }

        return true;
    }

    public static byte[] getFileByteArray(String path) throws IOException {
        return Files.readAllBytes(new File(path).toPath());
    }
}
