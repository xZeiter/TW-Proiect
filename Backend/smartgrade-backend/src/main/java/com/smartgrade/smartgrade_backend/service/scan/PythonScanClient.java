package com.smartgrade.smartgrade_backend.service.scan;

import com.smartgrade.smartgrade_backend.dto.scan.ScanUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PythonScanClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${smartgrade.scanner.base-url:http://localhost:8001}")
    private String scannerBaseUrl;

    public ScanUploadResponse scan(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) throw new RuntimeException("File is missing");

            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    String name = file.getOriginalFilename();
                    return (name == null || name.isBlank()) ? "scan.jpg" : name;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<ScanUploadResponse> response =
                    restTemplate.postForEntity(scannerBaseUrl + "/scan", request, ScanUploadResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Scan service returned " + response.getStatusCode());
            }

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Scan service failed: " + e.getMessage());
        }
    }
}
