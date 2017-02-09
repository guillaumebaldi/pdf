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

import org.icepdf.core.pobjects.Resources;
import org.icepdf.core.pobjects.acroform.TextFieldDictionary;
import org.icepdf.core.pobjects.acroform.VariableTextFieldDictionary;
import org.icepdf.core.pobjects.annotations.Annotation;
import org.icepdf.core.pobjects.annotations.Appearance;
import org.icepdf.core.pobjects.annotations.AppearanceState;
import org.icepdf.core.pobjects.annotations.TextWidgetAnnotation;
import org.icepdf.core.pobjects.graphics.text.LineText;
import org.icepdf.core.pobjects.graphics.text.WordText;
import org.icepdf.ri.common.views.AbstractPageViewComponent;
import org.icepdf.ri.common.views.DocumentViewController;
import org.icepdf.ri.common.views.DocumentViewModel;
import org.icepdf.ri.common.views.annotations.*;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * TextFieldComponent represent a TextField or txt widget annotation type.  This
 * component will display JTextField, JTextArea, JTextPassword depending on the
 * the txt bit flags.  When the component comes into focus a Java Swing component
 * is rendered for editing purposes.  Once focus is lost the new appearance
 * stream written.
 *
 * @since 5.1
 */
public class TextFieldComponent extends WidgetAnnotationComponent {

    private static final Logger logger =
            Logger.getLogger(TextFieldComponent.class.toString());

    // text area
    private JTextComponent textFieldComponent;

    private boolean contentTextChange;

    private TextWidgetAnnotation textWidgetAnnotation;

    public TextFieldComponent(Annotation annotation,
                              final DocumentViewController documentViewController,
                              final AbstractPageViewComponent pageViewComponent,
                              final DocumentViewModel documentViewModel) {
        super(annotation, documentViewController, pageViewComponent, documentViewModel);

        // not focus for the base component, this allows for the focus management for the sub
        // field to work correctly.
        if (!annotation.allowScreenOrPrintRenderingOrInteraction()) {
            return;
        }
        this.setFocusable(true);
        isRollover = false;
        isShowInvisibleBorder = true;
        isResizable = true;
        isMovable = true;
        textWidgetAnnotation = (TextWidgetAnnotation) annotation;

        // text widget types for fixed for now so build out the needed components
        TextFieldDictionary textFieldDictionary = textWidgetAnnotation.getFieldDictionary();
        TextFieldDictionary.TextFieldType textFieldType = textFieldDictionary.getTextFieldType();

        // factory call the build the respective components.
        if (textFieldType == TextFieldDictionary.TextFieldType.TEXT_INPUT) {
            textFieldComponent = new ScalableTextField(documentViewModel);
            // TODO apply any quadding, need to support in postscript generator.
            VariableTextFieldDictionary.Quadding quadding = textFieldDictionary.getQuadding();
            // zero is left justified.
            // center justified
            if (quadding == VariableTextFieldDictionary.Quadding.CENTERED) {
                ((ScalableTextField) textFieldComponent).setHorizontalAlignment(JTextField.CENTER);
            }
            // right justified.
            else if (quadding == VariableTextFieldDictionary.Quadding.RIGHT_JUSTIFIED) {
                ((ScalableTextField) textFieldComponent).setHorizontalAlignment(JTextField.RIGHT);
            }
        } else if (textFieldType == TextFieldDictionary.TextFieldType.TEXT_AREA) {
            ScalableTextArea textArea = new ScalableTextArea(documentViewModel);
            textFieldComponent = textArea;
            // line wrap false to force users to add line breaks.
            textArea.setLineWrap(false);
        } else if (textFieldType == TextFieldDictionary.TextFieldType.TEXT_PASSWORD) {
            textFieldComponent = new ScalablePasswordField(documentViewModel);
        }
        textFieldComponent.addKeyListener(new TabKeyListener());
        // line wrap false to force users to add line breaks.
        if (textFieldComponent != null) {
            // check for max length
            if ((textWidgetAnnotation.getFieldDictionary())
                    .getMaxLength() > 0) {
                textFieldComponent.setDocument(new JTextFieldLimit(
                        (textWidgetAnnotation.getFieldDictionary())
                                .getMaxLength()));
            }
            // default font, will be reset from variable text on edit.
            textFieldComponent.setFont(new Font("Helvetica", Font.PLAIN, 12));
//            textFieldComponent.setBackground(new Color(0, 0, 255, 50));
            textFieldComponent.setMargin(new Insets(0, 0, 0, 0));
            textFieldComponent.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    contentTextChange = true;
                }

                public void removeUpdate(DocumentEvent e) {
                    contentTextChange = true;
                }

                public void changedUpdate(DocumentEvent e) {
                    contentTextChange = true;
                }
            });
            // lock the field until the correct tool selects it.
            textFieldComponent.setEditable(false);
            textFieldComponent.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
            GridLayout grid = new GridLayout(1, 1, 0, 0);
            this.setLayout(grid);
            this.add(textFieldComponent);
        }

        if (isInteractiveAnnotationsEnabled &&
                annotation.allowScreenOrPrintRenderingOrInteraction()) {
            textFieldComponent.setFocusable(true);
            textFieldComponent.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    super.focusGained(e);
                    if (textFieldComponent.getText().length() > 0) {
                        textFieldComponent.setSelectionStart(0);
                        textFieldComponent.setSelectionEnd(textFieldComponent.getText().length());
                    }
                }
            });
        } else {
            textFieldComponent.setFocusable(false);
        }

        // clean up the value and pass into the component.
        assignTextValue();

        // set zero border.
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        // add a focus management listener.
        KeyboardFocusManager focusManager =
                KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addPropertyChangeListener(this);

        revalidate();
    }

    /**
     * Force the focus into the child component so text entry is smooth.
     *
     * @param e focus event.
     */
    @Override
    public void focusGained(FocusEvent e) {
        super.focusGained(e);
        // mark annotation as selected.
        documentViewController.assignSelectedAnnotation(this);
        textFieldComponent.requestFocus();
    }

    private void assignTextValue() {
        String contents = (String) textWidgetAnnotation.getFieldDictionary().getFieldValue();
        if ((contents == null || contents.equals("")) &&
                textWidgetAnnotation.getFieldDictionary().getParent() != null) {
            contents = (String) textWidgetAnnotation.getFieldDictionary().getParent().getFieldValue();
        }
        if (contents != null && textFieldComponent != null) { //&& !contents.equals("")
            contents = contents.replace('\r', '\n');
            textFieldComponent.setText(contents);
        } else {
            // some PDF's don't use the field value but there might be a value that we can get from the
            // content stream.
            Appearance appearance = textWidgetAnnotation.getAppearances().get(
                    TextWidgetAnnotation.APPEARANCE_STREAM_NORMAL_KEY);
            AppearanceState appearanceState = appearance.getSelectedAppearanceState();
            if (appearanceState.getShapes() != null &&
                    appearanceState.getShapes().getPageText() != null) {
                textFieldComponent.setText(pageLinesToString(appearanceState.getShapes().getPageText().getPageLines()));
            }
        }
    }

    /**
     * Utility to get text from the appearance streams shapes.  Used of the TextField field dictionary
     * does not contain a value or default value entry.
     *
     * @param lineText line text array to parse text form.
     * @return String representation of the linText.
     */
    private String pageLinesToString(ArrayList<LineText> lineText) {
        if (lineText != null) {
            StringBuilder lines = new StringBuilder();
            for (LineText line : lineText) {
                for (WordText word : line.getWords()) {
                    lines.append(word.toString()).append(" ");
                }
            }
            return lines.toString();
        } else {
            return "";
        }
    }

    public void setAppearanceStream() {

        assignTextValue();
        // copy over annotation properties from the textField widget annotation.
        textFieldComponent.setOpaque(false);

        // update the text properties fields.
        textWidgetAnnotation.getFieldDictionary().setFieldValue(textFieldComponent.getText(),
                annotation.getPObjectReference());

        textFieldComponent.revalidate();
    }

    @Override
    public void dispose() {
        super.dispose();
        KeyboardFocusManager focusManager =
                KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.removePropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        super.propertyChange(evt);

        String prop = evt.getPropertyName();
        Object newValue = evt.getNewValue();
        Object oldValue = evt.getOldValue();

        if ("focusOwner".equals(prop) &&
                oldValue instanceof ScalableField) {
            ScalableField textField = (ScalableField) oldValue;
            if (textField.equals(textFieldComponent)) {
                textField.setEditable(false);
                if (contentTextChange) {
                    contentTextChange = false;
                    // update the annotation values
                    TextFieldDictionary textFieldDictionary = textWidgetAnnotation.getFieldDictionary();
                    textFieldDictionary.setFieldValue(textFieldComponent.getText(), annotation.getPObjectReference());
                    resetAppearanceShapes();
                }
                getParent().validate();
                textField.setActive(false);
                getParent().repaint();
            }
        } else if ("focusOwner".equals(prop) &&
                newValue instanceof ScalableField) {
            ScalableField textField = (ScalableField) newValue;
            if (textField.equals(textFieldComponent)) {
                textField.setEditable(true);
                textField.setActive(true);
                getParent().validate();
                getParent().repaint();
            }
        } else if ("valueFieldReset".equals(prop)) {
            assignTextValue();
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

    @Override
    public void validate() {
        if (textFieldComponent != null &&
                !(textFieldComponent instanceof ScalableTextArea)) {
            VariableTextFieldDictionary variableText = textWidgetAnnotation.getFieldDictionary();
            // make sure we have the correct font and size to show for a null DA when
            // displaying the widget for the first time.
            if (textWidgetAnnotation.getFieldDictionary().getDefaultAppearance() == null) {
                Appearance appearance = textWidgetAnnotation.getAppearances().get(textWidgetAnnotation.getCurrentAppearance());
                AppearanceState appearanceState = appearance.getSelectedAppearanceState();
                Resources resource = appearanceState.getResources();
                String currentContentStream = appearanceState.getOriginalContentStream();
                textWidgetAnnotation.generateDefaultAppearance(currentContentStream, resource, variableText);
            }
            textFieldComponent.setFont(
                    new Font(variableText.getFontName().toString(),
                            Font.PLAIN,
                            (int) (variableText.getSize() * documentViewModel.getViewZoom())));
        }
        super.validate();
    }

    public boolean isActive() {
        return textFieldComponent != null && ((ScalableField) textFieldComponent).isActive();
    }

    public void setActive(boolean active) {
        if (textFieldComponent != null) {
            ((ScalableField) textFieldComponent).setActive(active);
        }
    }

    public void setEditable(boolean editable) {
        if (textFieldComponent != null) {
            (textFieldComponent).setEditable(editable);
        }
    }

    class JTextFieldLimit extends PlainDocument {
        private int limit;

        JTextFieldLimit(int limit) {
            super();
            this.limit = limit;
        }

        JTextFieldLimit(int limit, boolean upper) {
            super();
            this.limit = limit;
        }

        public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
            if (str == null)
                return;

            if ((getLength() + str.length()) <= limit) {
                super.insertString(offset, str, attr);
            }
            if (getLength() == limit) {
                // try to change focus to next field.
                KeyboardFocusManager focusManager =
                        KeyboardFocusManager.getCurrentKeyboardFocusManager();
                focusManager.focusNextComponent();
            }
        }
    }

    public String toString() {
        if (textWidgetAnnotation.getEntries() != null) {
            return textWidgetAnnotation.getEntries().toString();
        }
        return super.toString();
    }

    // keep the focus running smoothly with the tab key as TextArea tends to eat it.
    class TabKeyListener extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_TAB) {
                e.consume();
                if (e.isShiftDown()) {
                    KeyboardFocusManager.
                            getCurrentKeyboardFocusManager().focusPreviousComponent();
                } else {
                    KeyboardFocusManager.
                            getCurrentKeyboardFocusManager().focusNextComponent();
                }
            }
        }
    }
}
