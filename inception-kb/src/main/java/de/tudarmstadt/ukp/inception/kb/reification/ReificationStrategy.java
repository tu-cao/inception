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
package de.tudarmstadt.ukp.inception.kb.reification;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.Statement;

import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public interface ReificationStrategy {
    Set<Statement> reify(KnowledgeBase kb, KBStatement aStatement);
    List<KBStatement> listStatements(KnowledgeBase kb, KBHandle aInstance, boolean aAll);
    void deleteInstance(KnowledgeBase kb, KBInstance aInstance);
    void deleteProperty(KnowledgeBase kb, KBProperty aProperty);
    void deleteConcept(KnowledgeBase kb, KBConcept aConcept);
    void deleteStatement(KnowledgeBase kb, KBStatement aStatement);
    void upsertStatement(KnowledgeBase kb, KBStatement aStatement);
    void addQualifier(KnowledgeBase kb, KBQualifier newQualifier);
    void deleteQualifier(KnowledgeBase kb, KBQualifier oldQualifier);
    void upsertQualifier(KnowledgeBase kb, KBQualifier aQualifier);
    List<KBQualifier> listQualifiers(KnowledgeBase kb, KBStatement aStatement);
    boolean statementsMatchSPO(KnowledgeBase akb, KBStatement mockStatement);
}
