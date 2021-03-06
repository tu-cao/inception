/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.kb;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseAccess;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseMapping;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

public class KnowledgeBaseProfileDeserializationTest
{
    private final String KNOWLEDGEBASE_TEST_PROFILES_YAML = "kb_test_profiles.yaml";

    @Test
    public void checkThatDeserializationWorks() throws IOException {
        String name = "Test KB";
        String url = "http://someurl/sparql";
        List<String> rootConcepts =  new ArrayList<>();
        RepositoryType type = RepositoryType.LOCAL;
        String classIri = "http://www.w3.org/2000/01/rdf-schema#Class";
        String subclassIri = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
        String typeIri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        String subPropertyIri = "http://www.w3.org/2000/01/rdf-schema#subPropertyOf";
        String label = "http://www.w3.org/2000/01/rdf-schema#label";
        String propertyTypeIri = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
        String descriptionIri = "http://www.w3.org/2000/01/rdf-schema#comment";
        String propertyLabelIri = "http://www.w3.org/2000/01/rdf-schema#label";
        String propertyDescriptionIri = "http://www.w3.org/2000/01/rdf-schema#comment";
        String fullTextSearchIri = "http://www.openrdf.org/contrib/lucenesail#matches";

        KnowledgeBaseMapping referenceMapping = new KnowledgeBaseMapping(classIri, subclassIri,
            typeIri, subPropertyIri, descriptionIri, label, propertyTypeIri, propertyLabelIri,
            propertyDescriptionIri, fullTextSearchIri);
        KnowledgeBaseProfile referenceProfile = new KnowledgeBaseProfile();

        KnowledgeBaseAccess referenceAccess = new KnowledgeBaseAccess();
        referenceAccess.setAccessUrl(url);

        referenceProfile.setMapping(referenceMapping);
        referenceProfile.setName(name);

        referenceProfile.setAccess(referenceAccess);
        referenceProfile.setType(type);
        referenceProfile.setRootConcepts(rootConcepts);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Map<String, KnowledgeBaseProfile> profiles;
        try (Reader r = new InputStreamReader(
            resolver.getResource(KNOWLEDGEBASE_TEST_PROFILES_YAML).getInputStream())) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            profiles = mapper
                .readValue(r, new TypeReference<HashMap<String, KnowledgeBaseProfile>>() {});
        }
        
        KnowledgeBaseProfile testProfile = profiles.get("test_profile");
        Assertions.assertThat(testProfile).isEqualToComparingFieldByFieldRecursively(referenceProfile);
    }
}
