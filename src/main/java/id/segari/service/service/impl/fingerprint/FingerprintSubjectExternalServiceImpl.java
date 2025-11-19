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
    public List<FingerprintSubjectResponse> getFingerprintSubject(final long warehouseId) {
        final String url = buildWarehouseUrl(warehouseId);
        return fetchFingerprintSubjects(url, "warehouse: " + warehouseId);
    }

    @Override
    public List<FingerprintSubjectResponse> getFingerprintSubject(final long warehouseId, final long latestInternalToolsId) {
        final String url = buildWarehouseWithUserUrl(warehouseId, latestInternalToolsId);
        return fetchFingerprintSubjects(url, "warehouse: " + warehouseId + ", internal tools user: " + latestInternalToolsId);
    }

    @Override
    public List<FingerprintSubjectResponse> getFingerprintSubject(final String employeeId) {
        final String url = buildEmployeeUrl(employeeId);
        return fetchFingerprintSubjects(url, "employee: " + employeeId);
    }

    private String buildWarehouseUrl(final long warehouseId) {
        return backendEndpoint + BASE_PATH + "/warehouses/" + warehouseId;
    }

    private String buildWarehouseWithUserUrl(final long warehouseId, final long internalToolsUserId) {
        return backendEndpoint + BASE_PATH + "/warehouses/" + warehouseId + "/itus/" + internalToolsUserId;
    }

    private String buildEmployeeUrl(final String employeeId) {
        return backendEndpoint + BASE_PATH + "/employees/" + employeeId;
    }

    private List<FingerprintSubjectResponse> fetchFingerprintSubjects(final String url, final String context) {
        try {
            log.debug("Fetching fingerprint subjects for {}", context);
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
