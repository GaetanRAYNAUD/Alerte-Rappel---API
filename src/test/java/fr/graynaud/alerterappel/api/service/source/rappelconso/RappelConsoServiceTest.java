package fr.graynaud.alerterappel.api.service.source.rappelconso;

import fr.graynaud.alerterappel.api.config.properties.DataProperties;
import fr.graynaud.alerterappel.api.config.properties.RappelConsoProperties;
import fr.graynaud.alerterappel.api.service.alert.AlertService;
import fr.graynaud.alerterappel.api.service.alert.dto.Alert;
import fr.graynaud.alerterappel.api.service.source.rappelconso.dto.RappelConsoData;
import fr.graynaud.alerterappel.api.service.source.rappelconso.dto.RappelConsoRappel;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RappelConsoServiceTest {

    @Mock
    private AlertService alertService;
    @Mock
    private Environment environment;
    @Mock
    private TaskScheduler taskScheduler;
    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private RappelConsoService service;

    @BeforeEach
    void setUp() throws IOException {
        when(restClientBuilder.clone()).thenReturn(restClientBuilder);
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.requestInterceptor(any())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);

        RappelConsoProperties props = new RappelConsoProperties();
        props.setName("RappelConso");
        props.setCron("0 0 0 * * *");
        DataProperties dataProps = mock(DataProperties.class);
        when(dataProps.getSourcePath(any())).thenReturn(java.nio.file.Files.createTempFile("rappelconso", ".json"));
        JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();

        this.service = new RappelConsoService(restClientBuilder, props, dataProps, jsonMapper, taskScheduler, alertService, environment);
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleNewDataFetchesAndAddsAlerts() throws NoSuchFieldException, IllegalAccessException {
        OffsetDateTime since = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        RappelConsoData data = new RappelConsoData();

        RestClient mockUpdateClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec mockUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec mockResponseSpec = mock(RestClient.ResponseSpec.class);

        java.lang.reflect.Field updateClientField = fr.graynaud.alerterappel.api.service.source.explore21.Explore21Service.class.getDeclaredField("updateClient");
        updateClientField.setAccessible(true);
        updateClientField.set(service, mockUpdateClient);

        when(mockUpdateClient.get()).thenReturn(mockUriSpec);
        when(mockUriSpec.uri(any(java.util.function.Function.class))).thenReturn(mockUriSpec);
        when(mockUriSpec.exchange(any())).thenAnswer(invocation -> {
            return List.of(new RappelConsoRappel(1L, "GUID", "SR/001/26", 1, "Libellé", OffsetDateTime.now(), "Nature", "Cat", "SousCat", "MARQUE", "Ref", null, "Cond", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        });

        service.handleNewData(since, data);

        verify(alertService).addAlerts(anyList());
    }
}
