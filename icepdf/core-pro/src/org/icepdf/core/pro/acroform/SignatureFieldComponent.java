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

import org.icepdf.core.pobjects.annotations.Annotation;
import org.icepdf.ri.common.views.AbstractPageViewComponent;
import org.icepdf.ri.common.views.DocumentViewController;
import org.icepdf.ri.common.views.DocumentViewModel;

import java.util.logging.Logger;

/**
 * UI component that represents a Acroform signature widget in the interactive UI.
 * Focus, mouse, validation and form submission is handled by this class.
 *
 * @since 6.1
 */
public class SignatureFieldComponent extends org.icepdf.ri.common.views.annotations.SignatureFieldComponent {

    private static final Logger logger =
            Logger.getLogger(SignatureFieldComponent.class.toString());


    public SignatureFieldComponent(Annotation annotation, DocumentViewController documentViewController,
                                   AbstractPageViewComponent pageViewComponent, DocumentViewModel documentViewModel) {
        super(annotation, documentViewController, pageViewComponent, documentViewModel);

    }
}

