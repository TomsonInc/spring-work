package itx.examples.springboot.demo.controller;

import itx.examples.springboot.demo.config.ApplicationConfig;
import itx.examples.springboot.demo.dto.BuildInfo;
import itx.examples.springboot.demo.dto.DataMessage;
import itx.examples.springboot.demo.dto.RequestInfo;
import itx.examples.springboot.demo.dto.generic.ComplexDataPayload;
import itx.examples.springboot.demo.dto.generic.DataMarker;
import itx.examples.springboot.demo.dto.generic.GenericRequest;
import itx.examples.springboot.demo.dto.generic.GenericResponse;
import itx.examples.springboot.demo.dto.SystemInfo;
import itx.examples.springboot.demo.dto.generic.SimpleDataPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/data")
public class DataServiceController {

    private static final Logger LOG = LoggerFactory.getLogger(DataServiceController.class);
    private final ApplicationConfig applicationConfig;
    private final BuildProperties buildProperties;

    @Autowired
    public DataServiceController(ApplicationConfig applicationConfig, BuildProperties buildProperties) {
        this.applicationConfig = applicationConfig;
        this.buildProperties = buildProperties;
    }

    @GetMapping(path = "/build-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public BuildInfo getBuildInfo() {
        LOG.info("Fetching build information.");
        if (buildProperties != null) {
            LOG.info("Git Version: {}", buildProperties.get("gitversion"));
            return new BuildInfo(
                    buildProperties.getTime().toString(),
                    buildProperties.getVersion(),
                    buildProperties.getName(),
                    buildProperties.getArtifact(),
                    buildProperties.getGroup(),
                    buildProperties.get("gitfullhash"),
                    buildProperties.get("gitbranchname")
            );
        }
        return new BuildInfo("", "", "", "", "", "", "");
    }

    @GetMapping(path = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public SystemInfo getSystemInfo() {
        LOG.info("Fetching system information.");
        return new SystemInfo(applicationConfig.getId(), "spring-demo", "1.0.0", System.currentTimeMillis());
    }

    @PostMapping(path = "/message", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataMessage> getDataMessage(@RequestBody DataMessage dataMessage) {
        LOG.info("Received message: {}", dataMessage.getData());
        DataMessage responseMessage = new DataMessage(dataMessage.getData());
        return buildJsonResponse(responseMessage);
    }

    @GetMapping(path = "/echo/{message}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DataMessage> getEcho(@PathVariable String message) {
        LOG.info("Echoing message: {}", message);
        DataMessage responseMessage = new DataMessage(message);
        return buildJsonResponse(responseMessage);
    }

    @PostMapping(path = "/generics", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public GenericResponse<? extends DataMarker> getGenericResponse(@RequestBody GenericRequest<? extends DataMarker> request) {
        LOG.info("Received generic request.");
        return processGenericRequest(request);
    }

    @RequestMapping(path = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RequestInfo> getRequestParameters(HttpServletRequest request) throws IOException {
        LOG.info("Fetching request parameters: {}?{}", request.getRequestURL(), request.getQueryString());
        String body = request.getReader().lines().collect(Collectors.joining());
        RequestInfo requestInfo = new RequestInfo(
                applicationConfig.getId(),
                request.getRequestURL().toString(),
                request.getQueryString(),
                body,
                request.getCharacterEncoding(),
                request.getMethod(),
                createCookiesMap(request.getCookies()),
                request.getContentType(),
                createHeaderMap(request),
                request.getProtocol(),
                createRemoteInfo(request)
        );
        return ResponseEntity.ok(requestInfo);
    }

    // Helper Methods

    private <T> ResponseEntity<T> buildJsonResponse(T responseBody) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(responseBody, responseHeaders, HttpStatus.OK);
    }

    private GenericResponse<? extends DataMarker> processGenericRequest(GenericRequest<? extends DataMarker> request) {
        if (request.getData() instanceof SimpleDataPayload) {
            return new GenericResponse<>(request.getName(), request.getData());
        } else if (request.getData() instanceof ComplexDataPayload) {
            return new GenericResponse<>(request.getName(), request.getData());
        } else {
            throw new UnsupportedOperationException("Unsupported data type in request.");
        }
    }

    private Map<String, String> createCookiesMap(Cookie[] cookies) {
        if (cookies == null) return Collections.emptyMap();

        Map<String, String> cookiesMap = new HashMap<>();
        for (Cookie cookie : cookies) {
            cookiesMap.put(cookie.getDomain() + ":" + cookie.getName(), cookie.toString());
        }
        return cookiesMap;
    }

    private Map<String, List<String>> createHeaderMap(HttpServletRequest request) {
        Map<String, List<String>> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headerValues = request.getHeaders(headerName);
            List<String> valuesList = Collections.list(headerValues);
            headers.put(headerName, valuesList);
        }
        return headers;
    }

    private String createRemoteInfo(HttpServletRequest request) {
        return request.getRemoteHost() + ":" + request.getRemotePort();
    }
}
