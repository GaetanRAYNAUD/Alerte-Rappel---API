package fr.graynaud.alerterappel.api.service.source.rapex;

import fr.graynaud.alerterappel.api.config.properties.DataProperties;
import fr.graynaud.alerterappel.api.config.properties.RapexProperties;
import fr.graynaud.alerterappel.api.service.alert.AlertService;
import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import fr.graynaud.alerterappel.api.service.source.explore21.Explore21Response;
import fr.graynaud.alerterappel.api.service.source.rapex.dto.RapexData;
import fr.graynaud.alerterappel.api.service.source.rapex.dto.RapexNotification;
import fr.graynaud.alerterappel.api.service.source.rapex.dto.RapexNotificationSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RapexServiceTest {

    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private RapexProperties rapexProperties;
    @Mock
    private DataProperties dataProperties;
    @Mock
    private JsonMapper jsonMapper;
    @Mock
    private TaskScheduler taskScheduler;
    @Mock
    private AlertService alertService;
    @Mock
    private Environment environment;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private RapexService rapexService;

    @BeforeEach
    void setUp() throws IOException {
        when(restClientBuilder.clone()).thenReturn(restClientBuilder);
        when(restClientBuilder.baseUrl((String) any())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);
        
        when(rapexProperties.restClientBuilder(any())).thenReturn(restClientBuilder);
        when(restClientBuilder.requestInterceptor(any())).thenReturn(restClientBuilder);
        
        // Mocking DataProperties path
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("rapex", ".json");
        when(dataProperties.getSourcePath(any())).thenReturn(tempFile);
        
        when(rapexProperties.getCron()).thenReturn("0 0 0 * * *");

        rapexService = new RapexService(restClientBuilder, rapexProperties, dataProperties, jsonMapper, taskScheduler, alertService, environment);
    }

    @Test
    void handleNewData_fetchesAlertsAndSaves() throws NoSuchFieldException, IllegalAccessException {
        // Prepare data
        RapexData data = new RapexData();
        OffsetDateTime since = OffsetDateTime.now().minusDays(1);

        // Mock translation response
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(Collections.singletonMap("key", "value"));

        // Mock records response
        RapexNotificationSummary summary = new RapexNotificationSummary(since, "http://api.example.com/id1");
        Explore21Response<RapexNotificationSummary> response = new Explore21Response<>(1L, List.of(summary));
        Explore21Response<RapexNotificationSummary> emptyResponse = new Explore21Response<>(0L, Collections.emptyList());
        
        // Mocking protected updateClient field using reflection
        RestClient mockUpdateClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec mockUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);
        
        java.lang.reflect.Field updateClientField = fr.graynaud.alerterappel.api.service.source.explore21.Explore21Service.class.getDeclaredField("updateClient");
        updateClientField.setAccessible(true);
        updateClientField.set(rapexService, mockUpdateClient);

        when(mockUpdateClient.get()).thenReturn(mockUriSpec);
        when(mockUriSpec.uri(any(java.util.function.Function.class))).thenReturn(mockUriSpec);
        when(mockUriSpec.retrieve()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(response).thenReturn(emptyResponse);
        
        // Mock fetchAlert response
        RapexNotification notification = mock(RapexNotification.class);
        when(responseSpec.body(RapexNotification.class)).thenReturn(notification);
        
        // Run test
        rapexService.handleNewData(since, data);
        
        // Verify
        verify(alertService, times(1)).addAlerts(anyList());
    }
}
