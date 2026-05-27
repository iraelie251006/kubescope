package tech.iraelie.kubescope.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.iraelie.kubescope.IntegrationTestSupport;
import tech.iraelie.kubescope.api.dto.ClusterOverviewResponse;
import tech.iraelie.kubescope.api.dto.DeploymentResponse;
import tech.iraelie.kubescope.api.dto.NamespaceResponse;
import tech.iraelie.kubescope.api.dto.NodeResponse;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestSupport.class)
class ClusterControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private ClusterReadService reads;

    @Test
    void overviewReturnsSummary() throws Exception {
        when(reads.overview()).thenReturn(new ClusterOverviewResponse(
                3, 10,
                new BigDecimal("42.50"),
                new BigDecimal("30.00"),
                new BigDecimal("123.45")));

        mvc.perform(get("/api/v1/cluster/overview").with(user("u")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNodes").value(3))
                .andExpect(jsonPath("$.totalPods").value(10))
                .andExpect(jsonPath("$.estimatedMonthlyCostUsd").value(123.45));
    }

    @Test
    void nodesReturnsList() throws Exception {
        when(reads.nodes()).thenReturn(List.of(new NodeResponse(
                "a-node", "t3.medium", "us-east-1",
                2000L, 500L, 2000000000L, 500000000L,
                new BigDecimal("0.0416"), new BigDecimal("30.00"))));

        mvc.perform(get("/api/v1/cluster/nodes").with(user("u")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("a-node"))
                .andExpect(jsonPath("$[0].instanceType").value("t3.medium"));
    }

    @Test
    void namespacesReturnsList() throws Exception {
        when(reads.namespaces()).thenReturn(List.of(
                new NamespaceResponse("default", 5, 100L, 200L, new BigDecimal("10.00"))));

        mvc.perform(get("/api/v1/cluster/namespaces").with(user("u")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].namespace").value("default"))
                .andExpect(jsonPath("$[0].podCount").value(5));
    }

    @Test
    void deploymentsReturnsList() throws Exception {
        when(reads.deployments()).thenReturn(List.of(
                new DeploymentResponse("app", "default", 3, 300L, 600L, new BigDecimal("5.00"))));

        mvc.perform(get("/api/v1/cluster/deployments").with(user("u")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("app"))
                .andExpect(jsonPath("$[0].replicas").value(3));
    }
}
