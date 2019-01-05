/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.ui.kb.stmt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.widget.tooltip.TooltipBehavior;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.ui.core.Focusable;
import de.tudarmstadt.ukp.inception.ui.kb.WriteProtectionBehavior;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxPropertySelectionEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxStatementChangedEvent;
import de.tudarmstadt.ukp.inception.ui.kb.event.AjaxStatementGroupChangedEvent;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring.StatementColoringRegistry;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.coloring.StatementColoringStrategy;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.editor.StatementEditor;

public class StatementGroupPanel extends Panel {

    private static final long serialVersionUID = 2431747012293487976L;
    private static final Logger LOG = LoggerFactory.getLogger(StatementGroupPanel.class);

    
    private static final String CONTENT_MARKUP_ID = "content";

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean StatementColoringRegistry coloringRegistry;

    private CompoundPropertyModel<StatementGroupBean> groupModel;
    private Component content;

    public StatementGroupPanel(String aId, CompoundPropertyModel<StatementGroupBean> aGroupModel)
    {
        super(aId, aGroupModel);
        groupModel = aGroupModel;
        setOutputMarkupId(true);
        
        if (groupModel.getObject().isNew()) {
            NewStatementGroupFragment newStatement = new NewStatementGroupFragment(
                    CONTENT_MARKUP_ID);

            // obtain AjaxRequestTarget and set the focus
            RequestCycle.get()
                    .find(AjaxRequestTarget.class)
                    .ifPresent(target -> target.focusComponent(newStatement.getFocusComponent()));

            content = newStatement;
        } else {
            content = new ExistingStatementGroupFragment(CONTENT_MARKUP_ID);
        }
        add(content);
    }

    /**
     * Add a new prototype statement using this group's instance and its current property
     * @param aStmtGroupBean a {@link StatementGroupBean}
     */
    private void addStatementProto(StatementGroupBean aStmtGroupBean) {
        KBStatement statementProto = new KBStatement(
            aStmtGroupBean.getInstance(),
            aStmtGroupBean.getProperty());

        aStmtGroupBean.getStatements().add(statementProto);
    }
    
    private class NewStatementGroupFragment extends Fragment implements Focusable {

        private static final long serialVersionUID = 7617846171917989652L;
        
        private Component focusComponent;
        
        public NewStatementGroupFragment(String aId) {
            super(aId, "newStatementGroup", StatementGroupPanel.this, groupModel);
                        
            IModel<KBHandle> property = Model.of();
            
            Form<KBHandle> form = new Form<>("form", property);
            DropDownChoice<KBHandle> type = new BootstrapSelect<>("property");
            type.setModel(property);
            type.setChoiceRenderer(new ChoiceRenderer<>("uiLabel"));            
            type.setChoices(getUnusedProperties());
            type.setRequired(true);
            type.setOutputMarkupId(true);
            form.add(type);
            focusComponent = type;
            
            form.add(new LambdaAjaxButton<>("create", this::actionNewProperty));
            form.add(new LambdaAjaxLink("cancel", this::actionCancelNewProperty));
            add(form);
        }
        
        /**
         * Returns the list of properties in the knowledge base for which the current instance does
         * not have statements for yet.
         * 
         * @return
         */
        private List<KBHandle> getUnusedProperties() {
            StatementGroupBean bean = groupModel.getObject(); 
            StatementDetailPreference detailPreference = bean.getDetailPreference();
            Set<KBHandle> existingPropertyHandles = Collections.emptySet();
            try {
                existingPropertyHandles = kbService
                        .listStatements(bean.getKb(), bean.getInstance(),
                                detailPreference == StatementDetailPreference.ALL)
                        .stream().map(stmt -> stmt.getProperty()).collect(Collectors.toSet());
            }
            catch (QueryEvaluationException e) {
                error("Unable to list statements: " + e.getLocalizedMessage());
                LOG.error("Unable to list statements.", e);

            }

            List<KBHandle> properties = new ArrayList<KBHandle>();
            try {
                properties = kbService.listDomainProperties(groupModel.getObject().getKb(),
                        bean.getInstance().getIdentifier(), true, true);
                
                Set<KBHandle> parentConceptList = kbService.getParentConceptList(
                        groupModel.getObject().getKb(), bean.getInstance().getIdentifier(), true);
                
                for (KBHandle parent : parentConceptList) {
                    properties.addAll(kbService.listDomainProperties(groupModel.getObject().getKb(),
                            parent.getIdentifier(), true, true));
                }

                // Condition here to avoid fail case scenario e.g. WikiData : In case the above
                // domain property doesn't return anything, we consider the complete list of 
                // properties for now
                if (properties.isEmpty()) {
                    properties = kbService.listProperties(groupModel.getObject().getKb(),
                            detailPreference == StatementDetailPreference.ALL);
                } 
            }
            catch (QueryEvaluationException e) {
                error("Unable to list properties: " + e.getLocalizedMessage());
                LOG.error("Unable to list properties.", e);
            }
            
            properties.removeAll(existingPropertyHandles);
            return properties;
        }

        private void actionNewProperty(AjaxRequestTarget target, Form<KBHandle> form) {
            groupModel.getObject().setProperty(form.getModelObject());

            // replace content to show existing statement group with a new, empty statement
            ExistingStatementGroupFragment fragment = new ExistingStatementGroupFragment(
                CONTENT_MARKUP_ID);
            addStatementProto(groupModel.getObject());
            content = content.replaceWith(fragment);

            target.add(StatementGroupPanel.this);
        }

        private void actionCancelNewProperty(AjaxRequestTarget target)
        {
            StatementGroupBean bean = groupModel.getObject();
            send(getPage(), Broadcast.BREADTH,
                new AjaxStatementGroupChangedEvent(target, bean, true));
        }
        
        @Override
        public Component getFocusComponent() {
            return focusComponent;
        }
    }
    
    public class ExistingStatementGroupFragment extends Fragment {
        
        private static final long serialVersionUID = 5054250870556101031L;
        
        private WebMarkupContainer statementListWrapper;
        
        public ExistingStatementGroupFragment(String aId) {
            super(aId, "existingStatementGroup", StatementGroupPanel.this, groupModel);

            StatementGroupBean statementGroupBean = groupModel.getObject();
            Form<StatementGroupBean> form = new Form<StatementGroupBean>("form");
            LambdaAjaxLink propertyLink = new LambdaAjaxLink("propertyLink",
                    this::actionPropertyLinkClicked);
            propertyLink.add(new Label("property", groupModel.bind("property.uiLabel")));
            form.add(propertyLink);

            // TODO what about handling type intersection when multiple range statements are
            // present?
            // obtain IRI of property range, if existent
            Optional<KBProperty> property = kbService.readProperty(statementGroupBean.getKb(),
                statementGroupBean.getProperty().getIdentifier());
            IModel<KBProperty> propertyModel = Model.of(property.orElse(null));

            WebMarkupContainer statementIdentifier = new WebMarkupContainer("statementIdtext"); 
            TooltipBehavior tip = new TooltipBehavior();
            tip.setOption("autoHide", false);
            tip.setOption("content",
                    Options.asString((groupModel.bind("property.identifier").getObject())));
            tip.setOption("showOn", Options.asString("click"));
            statementIdentifier.add(tip);
            form.add(statementIdentifier);
            RefreshingView<KBStatement> statementList = new RefreshingView<KBStatement>(
                    "statementList") {
                private static final long serialVersionUID = 5811425707843441458L;

                @Override
                protected Iterator<IModel<KBStatement>> getItemModels() {
                    return new ModelIteratorAdapter<KBStatement>(
                        statementGroupBean.getStatements()) {
                        @Override
                        protected IModel<KBStatement> model(KBStatement object) {
                            return LambdaModel.of(() -> object);
                        }
                    };
                }

                @Override
                protected void populateItem(Item<KBStatement> aItem) {
                    StatementEditor editor = new StatementEditor("statement",
                            groupModel.bind("kb"), aItem.getModel(), propertyModel);
                    aItem.add(editor);
                    aItem.setOutputMarkupId(true);
                }

            };
            statementList.setItemReuseStrategy(new ReuseIfModelsEqualStrategy());

            // wrap the RefreshingView in a WMC, otherwise we can't redraw it with AJAX (see
            // https://cwiki.apache.org/confluence/display/WICKET/How+to+repaint+a+ListView+via+Ajax)
            statementListWrapper = new WebMarkupContainer("statementListWrapper");
            statementListWrapper.setOutputMarkupId(true);
            statementListWrapper.add(statementList);
            form.add(statementListWrapper);

            WebMarkupContainer statementGroupFooter = new WebMarkupContainer("statementGroupFooter");
            LambdaAjaxLink addLink = new LambdaAjaxLink("add", this::actionAddValue);
            addLink.add(new Label("label", new ResourceModel("statement.value.add")));
            addLink.add(new WriteProtectionBehavior(groupModel.bind("kb")));
            statementGroupFooter.add(addLink);

            // Apply a specific coloring strategy depending on the property
            StatementColoringStrategy coloringStrategy = coloringRegistry
                .getStatementColoringStrategy(statementGroupBean.getProperty().getIdentifier(),
                    statementGroupBean.getKb());

            String frameColor = coloringStrategy.getFrameColor();
            AttributeAppender framehighlightAppender = new AttributeAppender("style",
                "background-color:#" + frameColor);
            statementGroupFooter.add(framehighlightAppender);
            form.add(framehighlightAppender);

            String textColor = coloringStrategy.getTextColor();
            String backgroundColor = coloringStrategy.getBackgroundColor();
            AttributeAppender highlightAppender = new AttributeAppender("style",
                "background-color:#" + backgroundColor + ";color:#" + textColor);
            statementListWrapper.add(highlightAppender);


            form.add(statementGroupFooter);
            add(form);

        }
        
        private void actionPropertyLinkClicked(AjaxRequestTarget target)
        {
            send(getPage(), Broadcast.BREADTH, new AjaxPropertySelectionEvent(target,
                    groupModel.getObject().getProperty(), true));
        }
        
        @OnEvent
        public void actionStatementChanged(AjaxStatementChangedEvent event) {
            // event is not relevant if the statement in the event has a different property|subject
            // than the property|subject of this statement group
            KBStatement statement = event.getStatement();
            boolean isEventForThisStatementGroup =
                statement.getProperty().equals(groupModel.getObject().getProperty())
                    && statement.getInstance().getIdentifier()
                    .equals(groupModel.getObject().getInstance().getIdentifier());
            if (!isEventForThisStatementGroup) {
                return;
            }
            else if (!event.isDeleted()) {
                KBStatement oldStatement = event.getStatementBeforeChange();
                // update the statement from the event
                StatementGroupBean bean = groupModel.getObject();
                bean.getStatements().remove(oldStatement);
                bean.getStatements().add(statement);
                groupModel.setObject(bean);
            }
            if (event.isDeleted()) {
                // remove statement found in the event from the model
                StatementGroupBean bean = groupModel.getObject();
                bean.getStatements().remove(statement);
                groupModel.setObject(bean);

                if (bean.getStatements().isEmpty()) {
                    send(getPage(), Broadcast.BREADTH,
                            new AjaxStatementGroupChangedEvent(event.getTarget(), bean, true));
                } else {
                    // refresh the list wrapper (only necessary if at least one statement remains,
                    // otherwise the whole statement group is removed anyway)
                    event.getTarget().add(statementListWrapper);
                }
            }
            event.getTarget().add(statementListWrapper);
        }

        private void actionAddValue(AjaxRequestTarget target) {
            addStatementProto(groupModel.getObject());
            target.add(statementListWrapper);
        }
    }
}
