/*
 * Copyright 2006-2017 ICEsoft Technologies Canada Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.icepdf.core.pro.acroform;

import org.icepdf.core.pobjects.acroform.ChoiceFieldDictionary;
import org.icepdf.core.pobjects.annotations.Annotation;
import org.icepdf.core.pobjects.annotations.ChoiceWidgetAnnotation;
import org.icepdf.ri.common.views.AbstractPageViewComponent;
import org.icepdf.ri.common.views.DocumentViewController;
import org.icepdf.ri.common.views.DocumentViewModel;
import org.icepdf.ri.common.views.annotations.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * UI component that represents a Acroform choice widget in the interactive UI.
 * Focus, mouse and form submission is handled by this class.
 *
 * @since 5.1
 */
public class ChoiceFieldComponent extends WidgetAnnotationComponent
        implements AdjustmentListener {

    private static final Logger logger =
            Logger.getLogger(ChoiceFieldComponent.class.toString());

    // combo box
    private ScalableJComboBox comboBoxList;
    // list box.
    private ScalableJList choiceList;
    private ScalableJScrollPane choiceListPane;

    private ChoiceWidgetAnnotation choiceWidgetAnnotation;


    public ChoiceFieldComponent(Annotation annotation, DocumentViewController documentViewController,
                                AbstractPageViewComponent pageViewComponent, DocumentViewModel documentViewModel) {
        super(annotation, documentViewController, pageViewComponent, documentViewModel);
        // not focus for the base component, this allows for the focus management for the sub
        // field to work correctly.
        setFocusable(false);

        isShowInvisibleBorder = true;
        isResizable = false;
        isMovable = false;

        choiceWidgetAnnotation = (ChoiceWidgetAnnotation) annotation;

        // text widget types for fixed for now so build out the needed components
        final ChoiceFieldDictionary choiceFieldDictionary = choiceWidgetAnnotation.getFieldDictionary();
        ChoiceFieldDictionary.ChoiceFieldType choiceFieldType = choiceFieldDictionary.getChoiceFieldType();
        // factory call the build the respective components.
        if (choiceFieldType == ChoiceFieldDictionary.ChoiceFieldType.CHOICE_COMBO ||
                choiceFieldType == ChoiceFieldDictionary.ChoiceFieldType.CHOICE_EDITABLE_COMBO) {
            ArrayList<ChoiceFieldDictionary.ChoiceOption> choices =
                    choiceFieldDictionary.getOptions();
            if (choices == null) {
                // try and get them from the shapes.
                choices = choiceWidgetAnnotation.generateChoices();
            }
            Vector items = new Vector(choices.size());
            for (ChoiceFieldDictionary.ChoiceOption choice : choices) {
                items.addElement(choice);
            }
            comboBoxList = new ScalableJComboBox(items, documentViewModel);
            if (choiceFieldDictionary.getFieldValue() != null &&
                    choiceFieldDictionary.getIndexes() != null &&
                    choiceFieldDictionary.getIndexes().size() == 1) {
                comboBoxList.setSelectedIndex(choiceFieldDictionary.getIndexes().get(0));
            }

            comboBoxList.setOpaque(false);
            // add value change listener.
            final Annotation fieldAnnotation = annotation;
            comboBoxList.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    // update the annotation model with new value
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        ChoiceFieldDictionary.ChoiceOption option =
                                (ChoiceFieldDictionary.ChoiceOption) e.getItem();
                        choiceFieldDictionary.setFieldValue(option.getValue(),
                                fieldAnnotation.getPObjectReference());
                    }
                }
            });
            GridLayout grid = new GridLayout(1, 1, 0, 0);
            this.setLayout(grid);
            if (isInteractiveAnnotationsEnabled &&
                    annotation.allowScreenOrPrintRenderingOrInteraction()) {
                this.add(comboBoxList);
            }
            // setup the button style
            String fontName = "Helvetica";
            if (choiceFieldDictionary.getFontName() != null) fontName = choiceFieldDictionary.getFontName().toString();
            comboBoxList.setFont(new Font(fontName, Font.PLAIN, (int) choiceFieldDictionary.getSize()));
            comboBoxList.setFocusable(true);
        } else if (choiceFieldType == ChoiceFieldDictionary.ChoiceFieldType.CHOICE_LIST_SINGLE_SELECT ||
                choiceFieldType == ChoiceFieldDictionary.ChoiceFieldType.CHOICE_LIST_MULTIPLE_SELECT) {
            // build the model.
            DefaultListModel listModel = new DefaultListModel();
            ArrayList<ChoiceFieldDictionary.ChoiceOption> choices =
                    choiceFieldDictionary.getOptions();
            if (choices == null) {
                // try and get them from the shapes.
                choices = choiceWidgetAnnotation.generateChoices();
            }
            int i = 0;
            for (ChoiceFieldDictionary.ChoiceOption choice : choices) {
                listModel.add(i++, choice);
            }
            choiceList = new ScalableJList(listModel, documentViewModel);
            final Annotation childAnnotation = annotation;
            choiceList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    boolean adjust = choiceList.getValueIsAdjusting();
                    if (!adjust) {
                        JList list = (JList) e.getSource();
                        int[] selected = list.getSelectedIndices();
                        ArrayList<Integer> selectedElements = new ArrayList<Integer>(selected.length);
                        for (int item : selected) {
                            selectedElements.add(item);
                        }
                        choiceWidgetAnnotation.getFieldDictionary().setFieldValue(
                                choiceList.getSelectedValue(),
                                childAnnotation.getPObjectReference());
                    }
                }
            });
            // update the selection model.
            if (choiceFieldType == ChoiceFieldDictionary.ChoiceFieldType.CHOICE_LIST_SINGLE_SELECT) {
                choiceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                // assign selected index.
                if (choiceFieldDictionary.getFieldValue() != null &&
                        choiceFieldDictionary.getIndexes() != null &&
                        choiceFieldDictionary.getIndexes().size() == 1) {
                    choiceList.setSelectedIndex(choiceFieldDictionary.getIndexes().get(0));
                }
            } else {
                choiceList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                // apply multiple select.
                if (choiceFieldDictionary.getIndexes() != null &&
                        choiceFieldDictionary.getIndexes().size() > 0) {
                    ArrayList<Integer> indexes = choiceFieldDictionary.getIndexes();
                    int[] selected = new int[indexes.size()];
                    for (int j = 0, max = indexes.size(); j < max; j++) {
                        selected[j] = indexes.get(j);
                    }
                    choiceList.setSelectedIndices(selected);
                }
            }
            choiceList.setLayoutOrientation(JList.VERTICAL);
            String fontName = "Helvetica";
            if (choiceFieldDictionary.getFontName() != null) fontName = choiceFieldDictionary.getFontName().toString();
            choiceList.setFont(new Font(fontName, Font.PLAIN, (int) choiceFieldDictionary.getSize()));
            choiceList.setFocusable(true);
            choiceListPane = new ScalableJScrollPane(choiceList, documentViewModel);
            // adjustment listeners help make the child list become visible when focus is made.  For some
            // reason focus listeners on the scroll bars don't work.
            choiceListPane.getVerticalScrollBar().addAdjustmentListener(this);
            choiceListPane.getHorizontalScrollBar().addAdjustmentListener(this);
            choiceListPane.setFocusable(false);
            GridLayout grid = new GridLayout(1, 1, 0, 0);
            this.setLayout(grid);
            setOpaque(false);
            // lock the interactive field.
            if (isInteractiveAnnotationsEnabled &&
                    annotation.allowScreenOrPrintRenderingOrInteraction()) {
                this.add(choiceListPane);
            }
        }
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        // add a focus management listener.
        KeyboardFocusManager focusManager =
                KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addPropertyChangeListener(this);

        revalidate();
    }

    @Override
    public void focusGained(FocusEvent e) {
        super.focusGained(e);
        if (comboBoxList != null) {
            comboBoxList.requestFocus();
        } else if (choiceList != null) {
            choiceList.requestFocus();
        }
    }

    @Override
    public void validate() {
        // text widget types for fixed for now so build out the needed components
        ChoiceFieldDictionary choiceFieldDictionary = choiceWidgetAnnotation.getFieldDictionary();
        ChoiceFieldDictionary.ChoiceFieldType choiceFieldType = choiceFieldDictionary.getChoiceFieldType();
        // apply font size change.
        String fontName = "Helvetica";
        if (choiceFieldDictionary.getFontName() != null) fontName = choiceFieldDictionary.getFontName().toString();
        if (choiceFieldType == ChoiceFieldDictionary.ChoiceFieldType.CHOICE_COMBO ||
                choiceFieldType == ChoiceFieldDictionary.ChoiceFieldType.CHOICE_EDITABLE_COMBO) {
            comboBoxList.setFont(new Font(fontName, Font.PLAIN,
                    (int) (choiceFieldDictionary.getSize() * documentViewModel.getViewZoom())));
        } else if (choiceFieldType == ChoiceFieldDictionary.ChoiceFieldType.CHOICE_LIST_SINGLE_SELECT ||
                choiceFieldType == ChoiceFieldDictionary.ChoiceFieldType.CHOICE_LIST_MULTIPLE_SELECT) {
            choiceList.setFont(new Font(fontName, Font.PLAIN,
                    (int) (choiceFieldDictionary.getSize() * documentViewModel.getViewZoom())));
        }
        super.validate();
    }

    public void setAppearanceStream() {

    }

    @Override
    public void dispose() {
        super.dispose();
        KeyboardFocusManager focusManager =
                KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.removePropertyChangeListener(this);
        if (choiceListPane != null) {
            choiceListPane.getHorizontalScrollBar().removeAdjustmentListener(this);
            choiceListPane.getVerticalScrollBar().removeAdjustmentListener(this);
        }
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        if (e.getValue() > 0) {
            choiceList.setActive(true);
            choiceListPane.setActive(true);
            // update underlying model, as focus might be lost
            choiceWidgetAnnotation.getFieldDictionary().setFieldValue(
                    choiceList.getSelectedValue(),
                    annotation.getPObjectReference());
        }
    }

    /**
     * We use the focusOwner property to detect focus of the lists, if focus is gained the respective
     * Swing components are made visible. If focus is lost the swing components are made invisible and the
     * the content stream are regenerated to reflect the change in state.
     *
     * @param evt property change event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        // make sure we check for value changes events.
        super.propertyChange(evt);

        String prop = evt.getPropertyName();
        Object newValue = evt.getNewValue();
        Object oldValue = evt.getOldValue();
        // focus detection for combo box or select one choice.
        if ("focusOwner".equals(prop) &&
                oldValue instanceof ScalableField) {
            ScalableField choiceField = (ScalableField) oldValue;
            if (choiceField.equals(comboBoxList) ||
                    choiceField.equals(choiceList) ||
                    choiceField.equals(choiceListPane)
                    ) {
                resetAppearanceShapes();
                getParent().validate();
                if (choiceField.equals(choiceList) ||
                        choiceField.equals(choiceListPane)) {
                    choiceList.setActive(false);
                    choiceListPane.setActive(false);
                    choiceListPane.getVerticalScrollBar().setVisible(false);
                } else {
                    choiceField.setActive(false);
                }
                getParent().validate();
                getParent().repaint();
            }
        } else if ("focusOwner".equals(prop) &&
                newValue instanceof ScalableField) {
            ScalableField choiceField = (ScalableField) newValue;
            if (choiceField.equals(comboBoxList) ||
                    choiceField.equals(choiceList) ||
                    choiceField.equals(choiceListPane)) {
                if (choiceField.equals(choiceList) ||
                        choiceField.equals(choiceListPane)) {
                    choiceList.setActive(true);
                    choiceListPane.setActive(true);
                    choiceListPane.getVerticalScrollBar().setVisible(true);
                } else {
                    choiceField.setActive(true);
                }
                getParent().validate();
                getParent().repaint();
            }
        } else if ("valueFieldReset".equals(prop)) {
            // update the component value
            ChoiceFieldDictionary choiceFieldDictionary = choiceWidgetAnnotation.getFieldDictionary();
            choiceFieldDictionary.setFieldValue(choiceFieldDictionary.getFieldValue(), annotation.getPObjectReference());
            // update widgets.
            resetAppearanceShapes();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        // show a light border when in edit mode so component is easier to see.
        isShowInvisibleBorder = false;
    }

    @Override
    public void resetAppearanceShapes() {
        setAppearanceStream();
        annotation.resetAppearanceStream(getPageTransform());
    }

    public boolean isActive() {
        return (comboBoxList != null && comboBoxList.isActive())
                || (choiceList != null && choiceList.isActive());
    }

}
