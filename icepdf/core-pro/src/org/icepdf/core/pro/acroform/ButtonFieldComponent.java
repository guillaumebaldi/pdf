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

import org.icepdf.core.pobjects.Name;
import org.icepdf.core.pobjects.acroform.ButtonFieldDictionary;
import org.icepdf.core.pobjects.annotations.Annotation;
import org.icepdf.core.pobjects.annotations.Appearance;
import org.icepdf.core.pobjects.annotations.ButtonWidgetAnnotation;
import org.icepdf.ri.common.views.AbstractPageViewComponent;
import org.icepdf.ri.common.views.DocumentViewController;
import org.icepdf.ri.common.views.DocumentViewModel;
import org.icepdf.ri.common.views.annotations.WidgetAnnotationComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;

/**
 * UI component that represents a Acroform Button widget in the interactive UI.
 * Focus, mouse and form submission is handled by this class.
 *
 * @since 5.1
 */
public class ButtonFieldComponent extends WidgetAnnotationComponent {

    public ButtonFieldComponent(Annotation annotation, DocumentViewController documentViewController,
                                AbstractPageViewComponent pageViewComponent, DocumentViewModel documentViewModel) {
        super(annotation, documentViewController, pageViewComponent, documentViewModel);
        isShowInvisibleBorder = true;
        isResizable = false;
        isMovable = false;

        ButtonWidgetAnnotation widget = getButtonWidgetAnnotation();
        ButtonFieldDictionary fieldDictionary = widget.getFieldDictionary();

        // check for a push button or check box.
        if (fieldDictionary.getButtonFieldType() ==
                ButtonFieldDictionary.ButtonFieldType.CHECK_BUTTON ||
                fieldDictionary.getButtonFieldType() ==
                        ButtonFieldDictionary.ButtonFieldType.RADIO_BUTTON) {
            ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    buttonActuated();
                }
            };
            // space selection support of radio and checkboxes
            KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
            registerKeyboardAction(actionListener, stroke, JComponent.WHEN_FOCUSED);
        } else if (fieldDictionary.getButtonFieldType() ==
                ButtonFieldDictionary.ButtonFieldType.PUSH_BUTTON) {
            ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
                    // todo add animation for button press
                    mouseClicked(null);
                }
            };
            KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
            registerKeyboardAction(actionListener, stroke, JComponent.WHEN_FOCUSED);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        // handle the click
        buttonActuated();
    }

    private ButtonWidgetAnnotation getButtonWidgetAnnotation() {
        ButtonWidgetAnnotation widget = null;
        if (annotation instanceof ButtonWidgetAnnotation) {
            widget = (ButtonWidgetAnnotation) annotation;
        } else {
            // corner case for PDF that aren't well formed
            try {
                widget = new ButtonWidgetAnnotation(annotation);
                widget.init();
                annotation = widget;
            } catch (InterruptedException e) {
                logger.fine("ButtonWidgetAnnotation initialization interrupted.");
            }
        }
        return widget;
    }

    private void buttonActuated() {
        ButtonWidgetAnnotation widget = getButtonWidgetAnnotation();

        ButtonFieldDictionary fieldDictionary = widget.getFieldDictionary();

        // value information for radio is stored in the parent field dictionary.
        ButtonFieldDictionary parentFieldDictionary = (ButtonFieldDictionary) fieldDictionary.getParent();
        if (parentFieldDictionary == null) {
            parentFieldDictionary = fieldDictionary;
        }

        // check for a push button or check box.
        if (fieldDictionary.getButtonFieldType() ==
                ButtonFieldDictionary.ButtonFieldType.CHECK_BUTTON) {
            // toggle appearance, no state is store in a button.
            Name newValue = widget.toggle();
            fieldDictionary.setFieldValue(newValue, annotation.getPObjectReference());
            parentFieldDictionary.setFieldValue(newValue, annotation.getPObjectReference());
            // apply UI state changes and persist changes.
            resetAppearanceShapes();
        } else if (fieldDictionary.getButtonFieldType() ==
                ButtonFieldDictionary.ButtonFieldType.RADIO_BUTTON) {
            if (fieldDictionary.isRadioInUnison() ||
                    parentFieldDictionary.isRadioInUnison()) {
                // set all children are in sync,  still no test for this usage case.
                // set all children are in sync,  still no test for this usage case.
                if (fieldDictionary.getParent() != null && fieldDictionary.getParent().getKids() != null) {
                    for (Object childWidget : fieldDictionary.getParent().getKids()) {
                        if (childWidget instanceof ButtonWidgetAnnotation) {
                            Name newValue = ((ButtonWidgetAnnotation) childWidget).toggle();
                            parentFieldDictionary.setFieldValue(newValue, annotation.getPObjectReference());
                        }
                    }
                }
            }
            // default radio button behavior.
            else {
                // if already on, nothing to do just return .
                if (widget.isOn()) {
                    return;
                }
                // otherwise we turn all the kids off and set the new value to the parent
                else {
                    // set all children to off
                    if (fieldDictionary.getParent() != null && fieldDictionary.getParent().getKids() != null) {
                        for (Object childWidget : fieldDictionary.getParent().getKids()) {
                            if (childWidget instanceof ButtonWidgetAnnotation) {
                                ((ButtonWidgetAnnotation) childWidget).turnOff();
                                // persist the change of state.
                                ((ButtonWidgetAnnotation) childWidget).resetAppearanceStream(getPageTransform());
                            }
                        }
                    }
                    // toggle the current appearance and value
                    Name newValue = widget.toggle();
                    parentFieldDictionary.setFieldValue(newValue, annotation.getPObjectReference());
                }
            }
            // apply UI state changes and persist changes.
            resetAppearanceShapes();

        } else if (fieldDictionary.getButtonFieldType() ==
                ButtonFieldDictionary.ButtonFieldType.PUSH_BUTTON) {
            // nothing to do the actions will be associated and called via
            // the annotation callback.  callback.
        }
        this.getParent().repaint();

        // todo apply javascript and forum submision for buttons.
    }

    public void propertyChange(PropertyChangeEvent evt) {
        // make sure we check for value changes events.
        super.propertyChange(evt);

        String prop = evt.getPropertyName();
        Object oldValue = evt.getOldValue();
        Object newValue = evt.getNewValue();
        if ("valueFieldReset".equals(prop)) {
            ButtonWidgetAnnotation widget = (ButtonWidgetAnnotation) annotation;
            ButtonFieldDictionary fieldDictionary = widget.getFieldDictionary();
            ButtonFieldDictionary parentFieldDictionary = (ButtonFieldDictionary) fieldDictionary.getParent();

            if (fieldDictionary.getButtonFieldType() ==
                    ButtonFieldDictionary.ButtonFieldType.CHECK_BUTTON) {
                // check for a default value and and apply it.
                // Look for a default value.
                Object tmp = fieldDictionary.getDefaultFieldValue();
                if (tmp != null && tmp instanceof Name) {
                    fieldDictionary.setFieldValue(tmp, annotation.getPObjectReference());
                    return;
                }
                // check the parent
                if (parentFieldDictionary != null && parentFieldDictionary.getDefaultFieldValue() != null) {
                    fieldDictionary.setFieldValue(parentFieldDictionary.getDefaultFieldValue(),
                            parentFieldDictionary.getPObjectReference());
                    return;
                }
                // otherwise we default to the off appearance state.
                widget.turnOff();

            } else if (fieldDictionary.getButtonFieldType() ==
                    ButtonFieldDictionary.ButtonFieldType.RADIO_BUTTON) {
                // try and find the default value, if none then we turn the children to the off state.
                if (parentFieldDictionary != null && !parentFieldDictionary.hasFieldValue()) {
                    for (Object childWidget : parentFieldDictionary.getKids()) {
                        if (childWidget instanceof ButtonWidgetAnnotation) {
                            ((ButtonWidgetAnnotation) childWidget).turnOff();
                        }
                    }
                } // otherwise we need to find which child has the default value.
                else if (parentFieldDictionary != null && parentFieldDictionary.hasFieldValue()) {
                    // reset the child appearance stream.
                    Name defaultValue = (Name) parentFieldDictionary.getDefaultFieldValue();
                    parentFieldDictionary.setFieldValue(defaultValue, parentFieldDictionary.getPObjectReference());

                    // update the parent with the default field value.
                    ButtonWidgetAnnotation buttonWidgetAnnotation;
                    for (Object childWidget : parentFieldDictionary.getKids()) {
                        if (childWidget instanceof ButtonWidgetAnnotation) {
                            // default to off.
                            buttonWidgetAnnotation = (ButtonWidgetAnnotation) childWidget;
                            buttonWidgetAnnotation.turnOff();
                            // if we have a value match to the default value then turn the named appearance on.
                            Name currentAppearance = buttonWidgetAnnotation.getCurrentAppearance();
                            HashMap<Name, Appearance> appearances = buttonWidgetAnnotation.getAppearances();
                            Appearance appearance = appearances.get(currentAppearance);
                            if (appearance.getOnName().equals(defaultValue)) {
                                appearance.setSelectedName(appearance.getOnName());
                            }
                        }
                    }
                }
            }
            // apply UI state changes and persist changes.
            resetAppearanceShapes();
        }
    }

    @Override
    public void resetAppearanceShapes() {
        annotation.resetAppearanceStream(getPageTransform());
    }

    @Override
    public void paintComponent(Graphics g) {

    }
}
