package id.segari.service.service.impl.fingerprint;

import id.segari.service.common.dto.external.SegariResponse;
import id.segari.service.common.dto.fingerprint.FingerprintSubjectResponse;
import id.segari.service.service.FingerprintSubjectExternalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class FingerprintSubjectExternalServiceImpl implements FingerprintSubjectExternalService {

    private static final Logger log = LoggerFactory.getLogger(FingerprintSubjectExternalServiceImpl.class);
    private static final String BASE_PATH = "/v1/attendances/fingerprints/subjects";
    private static final ParameterizedTypeReference<SegariResponse<List<FingerprintSubjectResponse>>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    @Value("${segari.backend.endpoint}")
    private final String backendEndpoint;

    public FingerprintSubjectExternalServiceImpl(RestClient restClient, @Value("${segari.backend.endpoint}") String backendEndpoint) {
        this.restClient = restClient;
        this.backendEndpoint = backendEndpoint;
    }

    @Override
    public List<FingerprintSubjectResponse> getFingerprintSubject(final long warehouseId,
                                                                   final String deviceId,
                                                                   final String sessionId) {
        final String url = buildWarehouseUrl(warehouseId, deviceId, sessionId);
        return fetchFingerprintSubjects(url, "warehouse: " + warehouseId + ", device: " + deviceId + ", session: " + sessionId);
    }

    @Override
    public List<FingerprintSubjectResponse> getFingerprintSubject(final long warehouseId,
                                                                   final long internalToolsUserId,
                                                                   final String deviceId,
                                                                   final String sessionId) {
        final String url = buildWarehouseWithUserUrl(warehouseId, internalToolsUserId, deviceId, sessionId);
        return fetchFingerprintSubjects(url,
                "warehouse: " + warehouseId + ", internal tools user: " + internalToolsUserId +
                ", device: " + deviceId + ", session: " + sessionId);
    }

    @Override
    public List<FingerprintSubjectResponse> getFingerprintSubject(final String employeeId,
                                                                   final String deviceId,
                                                                   final String sessionId) {
        final String url = buildEmployeeUrl(employeeId, deviceId, sessionId);
        return fetchFingerprintSubjects(url, "employee: " + employeeId + ", device: " + deviceId + ", session: " + sessionId);
    }

    private String buildWarehouseUrl(final long warehouseId, final String deviceId, final String sessionId) {
        return UriComponentsBuilder.fromUriString(backendEndpoint)
                .path(BASE_PATH + "/warehouses")
                .queryParam("warehouseId", warehouseId)
                .queryParam("deviceId", deviceId)
                .queryParam("sessionId", sessionId)
                .toUriString();
    }

    private String buildWarehouseWithUserUrl(final long warehouseId,
                                              final long internalToolsUserId,
                                              final String deviceId,
                                              final String sessionId) {
        return UriComponentsBuilder.fromUriString(backendEndpoint)
                .path(BASE_PATH + "/warehouses/itus")
                .queryParam("warehouseId", warehouseId)
                .queryParam("internalToolsUserId", internalToolsUserId)
                .queryParam("deviceId", deviceId)
                .queryParam("sessionId", sessionId)
                .toUriString();
    }

    private String buildEmployeeUrl(final String employeeId, final String deviceId, final String sessionId) {
        return UriComponentsBuilder.fromUriString(backendEndpoint)
                .path(BASE_PATH + "/employees")
                .queryParam("employeeId", employeeId)
                .queryParam("deviceId", deviceId)
                .queryParam("sessionId", sessionId)
                .toUriString();
    }

    private List<FingerprintSubjectResponse> fetchFingerprintSubjects(final String url, final String context) {
        try {
            log.debug("Fetching fingerprint subjects for {}", context);
            log.debug("Request URL: {}", url);
            final SegariResponse<List<FingerprintSubjectResponse>> response = callApi(url);
            return extractDataFromResponse(response, context);
        } catch (Exception e) {
            log.error("Error fetching fingerprint subjects for {}", context, e);
            return List.of();
        }
    }

    private SegariResponse<List<FingerprintSubjectResponse>> callApi(final String url) {
        return restClient.get()
                .uri(url)
                .retrieve()
                .body(RESPONSE_TYPE);
    }

    private List<FingerprintSubjectResponse> extractDataFromResponse(final SegariResponse<List<FingerprintSubjectResponse>> response,
                                                                      final String context) {
        if (response == null || response.data() == null) {
            log.warn("Received null or empty response for {}", context);
            return List.of();
        }

        log.debug("Successfully fetched {} fingerprint subjects for {}", response.data().size(), context);
        return response.data();
    }
}