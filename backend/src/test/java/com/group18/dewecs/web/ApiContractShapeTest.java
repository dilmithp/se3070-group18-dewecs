package com.group18.dewecs.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.group18.dewecs.domain.District;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Compares the JSON key sets of real responses with doc/api-samples (keys only, never values). The samples are what
 * the Flutter app copies, so a response that drifts from them must fail here. Tests run with backend/ as the
 * working directory.
 */
class ApiContractShapeTest extends ApiTestSupport {

    private JsonNode sample(String name) throws IOException {
        return objectMapper.readTree(new File("../doc/api-samples/" + name));
    }

    /** Key paths; arrays contribute the keys of their first element; fieldErrors is an open map. */
    private Set<String> shape(JsonNode node, String prefix) {
        Set<String> paths = new TreeSet<>();
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String path = prefix.isEmpty() ? field.getKey() : prefix + "." + field.getKey();
                paths.add(path);
                if (!"fieldErrors".equals(field.getKey())) {
                    paths.addAll(shape(field.getValue(), path));
                }
            }
        } else if (node.isArray() && !node.isEmpty()) {
            paths.add(prefix + "[]");
            paths.addAll(shape(node.get(0), prefix + "[]"));
        }
        return paths;
    }

    private void assertSameShape(String sampleName, MvcResult actual) throws Exception {
        assertThat(shape(body(actual), "")).as(sampleName).isEqualTo(shape(sample(sampleName), ""));
    }

    @Test
    void responsesHaveTheSameKeysAsTheSamples() throws Exception {
        District d = district();
        MvcResult citizen = identify(d, uniqueNic(), "Nimal Perera");
        long citizenId = body(citizen).get("id").asLong();
        MvcResult report = submit(reportRequest(citizenId, d, minutesAgo(5)));
        long reportId = body(report).get("id").asLong();

        assertSameShape("citizen.json", citizen);
        assertSameShape("report.json", report);
        assertSameShape("reference-data.json", mvc.perform(get("/api/v1/reference-data")).andReturn());
        assertSameShape("report-list.json",
                mvc.perform(get("/api/v1/citizens/{id}/ground-reports", citizenId)).andReturn());
        assertSameShape("report.json", upload(reportId, PNG));
        assertSameShape("problem.json", mvc.perform(get("/api/v1/ground-reports/999999999")).andReturn());
        assertSameShape("problem-with-field-errors.json", mvc.perform(post("/api/v1/ground-reports")
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn());
    }
}
