/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.tools.internal.corext.refactoring.code.ExtractMethodRefactoring;
import com.google.dart.tools.internal.corext.refactoring.code.ParameterInfo;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.dialogs.TextFieldNavigationHandler;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartSourceViewer;
import com.google.dart.tools.ui.internal.util.RowLayouter;
import com.google.dart.tools.ui.text.DartSourceViewerConfiguration;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class ExtractMethodInputPage extends UserInputWizardPage {

  public static final String PAGE_NAME = "ExtractMethodInputPage";//$NON-NLS-1$

  private ExtractMethodRefactoring fRefactoring;
  private Text fTextField;
  private boolean fFirstTime;
  private DartSourceViewer fSignaturePreview;
  private Document fSignaturePreviewDocument;
  private IDialogSettings fSettings;

  private static final String DESCRIPTION = RefactoringMessages.ExtractMethodInputPage_description;
  private static final String THROW_RUNTIME_EXCEPTIONS = "ThrowRuntimeExceptions"; //$NON-NLS-1$
  private static final String GENERATE_JAVADOC = "GenerateJavadoc"; //$NON-NLS-1$
  private static final String ACCESS_MODIFIER = "AccessModifier"; //$NON-NLS-1$

  public ExtractMethodInputPage() {
    super(PAGE_NAME);
    setImageDescriptor(DartPluginImages.DESC_WIZBAN_REFACTOR_CU);
    setDescription(DESCRIPTION);
    fFirstTime = true;
    fSignaturePreviewDocument = new Document();
  }

  @Override
  public void createControl(Composite parent) {
    fRefactoring = (ExtractMethodRefactoring) getRefactoring();
    loadSettings();

    Composite result = new Composite(parent, SWT.NONE);
    setControl(result);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    result.setLayout(layout);
    RowLayouter layouter = new RowLayouter(2);
    GridData gd = null;

    initializeDialogUnits(result);

    Label label = new Label(result, SWT.NONE);
    label.setText(getLabelText());

    fTextField = createTextInputField(result, SWT.BORDER);
    fTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    layouter.perform(label, fTextField, 1);

//    ASTNode[] destinations = fRefactoring.getDestinations();
//    if (destinations.length > 1) {
//      label = new Label(result, SWT.NONE);
//      label.setText(RefactoringMessages.ExtractMethodInputPage_destination_type);
//      final Combo combo = new Combo(result, SWT.READ_ONLY | SWT.DROP_DOWN);
//      SWTUtil.setDefaultVisibleItemCount(combo);
//      for (int i = 0; i < destinations.length; i++) {
//        ASTNode declaration = destinations[i];
//        combo.add(getLabel(declaration));
//      }
//      combo.select(0);
//      combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//      combo.addSelectionListener(new SelectionAdapter() {
//        @Override
//        public void widgetSelected(SelectionEvent e) {
//          fRefactoring.setDestination(combo.getSelectionIndex());
//          updatePreview(getText());
//        }
//      });
//    }
//
//    label = new Label(result, SWT.NONE);
//    label.setText(RefactoringMessages.ExtractMethodInputPage_access_Modifiers);
//
//    Composite group = new Composite(result, SWT.NONE);
//    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//    layout = new GridLayout();
//    layout.numColumns = 4;
//    layout.marginWidth = 0;
//    group.setLayout(layout);
//
//    String[] labels = new String[] {
//        RefactoringMessages.ExtractMethodInputPage_public,
//        RefactoringMessages.ExtractMethodInputPage_protected,
//        RefactoringMessages.ExtractMethodInputPage_default,
//        RefactoringMessages.ExtractMethodInputPage_private};
//    Integer[] data = new Integer[] {
//        new Integer(Modifier.PUBLIC), new Integer(Modifier.PROTECTED), new Integer(Modifier.NONE),
//        new Integer(Modifier.PRIVATE)};
//    Integer visibility = new Integer(fRefactoring.getVisibility());
//    for (int i = 0; i < labels.length; i++) {
//      Button radio = new Button(group, SWT.RADIO);
//      radio.setText(labels[i]);
//      radio.setData(data[i]);
//      if (data[i].equals(visibility)) {
//        radio.setSelection(true);
//      }
//      radio.addSelectionListener(new SelectionAdapter() {
//        @Override
//        public void widgetSelected(SelectionEvent event) {
//          final Integer selectedModifier = (Integer) event.widget.getData();
//          fSettings.put(ACCESS_MODIFIER, selectedModifier.intValue());
//          setVisibility(selectedModifier);
//        }
//      });
//    }
//    layouter.perform(label, group, 1);

    if (!fRefactoring.getParameters().isEmpty()) {
      ChangeParametersControl cp = new ChangeParametersControl(
          result,
          SWT.NONE,
          RefactoringMessages.ExtractMethodInputPage_parameters,
          new IParameterListChangeListener.Empty() {
            @Override
            public void parameterChanged(ParameterInfo parameter) {
              parameterModified();
            }

            @Override
            public void parameterListChanged() {
              parameterModified();
            }
          },
          ChangeParametersControl.Mode.EXTRACT_METHOD);
      gd = new GridData(GridData.FILL_BOTH);
      gd.horizontalSpan = 2;
      cp.setLayoutData(gd);
      cp.setInput(fRefactoring.getParameters());
    }

//    Button checkBox = new Button(result, SWT.CHECK);
//    checkBox.setText(RefactoringMessages.ExtractMethodInputPage_throwRuntimeExceptions);
//    checkBox.setSelection(fSettings.getBoolean(THROW_RUNTIME_EXCEPTIONS));
//    checkBox.addSelectionListener(new SelectionAdapter() {
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        setRethrowRuntimeException(((Button) e.widget).getSelection());
//      }
//    });
//    layouter.perform(checkBox);
//
//    checkBox = new Button(result, SWT.CHECK);
//    checkBox.setText(RefactoringMessages.ExtractMethodInputPage_generateJavadocComment);
//    boolean generate = computeGenerateJavadoc();
//    setGenerateJavadoc(generate);
//    checkBox.setSelection(generate);
//    checkBox.addSelectionListener(new SelectionAdapter() {
//      @Override
//      public void widgetSelected(SelectionEvent e) {
//        setGenerateJavadoc(((Button) e.widget).getSelection());
//      }
//    });
//    layouter.perform(checkBox);

    int duplicates = fRefactoring.getNumberOfDuplicates();
    Button checkBox = new Button(result, SWT.CHECK);
    if (duplicates == 0) {
      checkBox.setText(RefactoringMessages.ExtractMethodInputPage_duplicates_none);
    } else if (duplicates == 1) {
      checkBox.setText(RefactoringMessages.ExtractMethodInputPage_duplicates_single);
    } else {
      checkBox.setText(Messages.format(
          RefactoringMessages.ExtractMethodInputPage_duplicates_multi,
          new Integer(duplicates)));
    }
    checkBox.setSelection(fRefactoring.getReplaceAllOccurrences());
    checkBox.setEnabled(duplicates > 0);
    checkBox.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fRefactoring.setReplaceAllOccurrences(((Button) e.widget).getSelection());
      }
    });
    layouter.perform(checkBox);

    label = new Label(result, SWT.SEPARATOR | SWT.HORIZONTAL);
    label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    layouter.perform(label);

    createSignaturePreview(result, layouter);

    Dialog.applyDialogFont(result);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(
        getControl(),
        DartHelpContextIds.EXTRACT_METHOD_WIZARD_PAGE);
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      if (fFirstTime) {
        fFirstTime = false;
        setPageComplete(false);
        updatePreview(getText());
        fTextField.setFocus();
      } else {
        setPageComplete(validatePage(true));
      }
    }
    super.setVisible(visible);
  }

//  private boolean computeGenerateJavadoc() {
//    boolean result = fRefactoring.getGenerateJavadoc();
//    if (result) {
//      return result;
//    }
//    return fSettings.getBoolean(GENERATE_JAVADOC);
//  }

  private void createSignaturePreview(Composite composite, RowLayouter layouter) {
    Label previewLabel = new Label(composite, SWT.NONE);
    previewLabel.setText(RefactoringMessages.ExtractMethodInputPage_signature_preview);
    layouter.perform(previewLabel);

    IPreferenceStore store = DartToolsPlugin.getDefault().getCombinedPreferenceStore();
    fSignaturePreview = new DartSourceViewer(composite, null, null, false, SWT.READ_ONLY
        | SWT.V_SCROLL | SWT.WRAP /*| SWT.BORDER*/, store);
    fSignaturePreview.configure(new DartSourceViewerConfiguration(
        DartToolsPlugin.getDefault().getJavaTextTools().getColorManager(),
        store,
        null,
        null));
    fSignaturePreview.getTextWidget().setFont(
        JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
    fSignaturePreview.adaptBackgroundColor(composite);
    fSignaturePreview.setDocument(fSignaturePreviewDocument);
    fSignaturePreview.setEditable(false);

    Control signaturePreviewControl = fSignaturePreview.getControl();
    PixelConverter pixelConverter = new PixelConverter(signaturePreviewControl);
    GridData gdata = new GridData(GridData.FILL_BOTH);
    gdata.widthHint = pixelConverter.convertWidthInCharsToPixels(50);
    gdata.heightHint = pixelConverter.convertHeightInCharsToPixels(2);
    signaturePreviewControl.setLayoutData(gdata);
    layouter.perform(signaturePreviewControl);
  }

  private Text createTextInputField(Composite parent, int style) {
    Text result = new Text(parent, style);
    result.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        textModified(getText());
      }
    });
    TextFieldNavigationHandler.install(result);
    return result;
  }

//  private String getLabel(ASTNode node) {
//    if (node instanceof AbstractTypeDeclaration) {
//      return ((AbstractTypeDeclaration) node).getName().getIdentifier();
//    } else if (node instanceof AnonymousClassDeclaration) {
//      if (node.getLocationInParent() == ClassInstanceCreation.ANONYMOUS_CLASS_DECLARATION_PROPERTY) {
//        ClassInstanceCreation creation = (ClassInstanceCreation) node.getParent();
//        return Messages.format(
//            RefactoringMessages.ExtractMethodInputPage_anonymous_type_label,
//            BasicElementLabels.getJavaElementName(ASTNodes.asString(creation.getType())));
//      } else if (node.getLocationInParent() == EnumConstantDeclaration.ANONYMOUS_CLASS_DECLARATION_PROPERTY) {
//        EnumConstantDeclaration decl = (EnumConstantDeclaration) node.getParent();
//        return decl.getName().getIdentifier();
//      }
//    }
//    return "UNKNOWN"; //$NON-NLS-1$
//  }

  private String getLabelText() {
    return RefactoringMessages.ExtractMethodInputPage_label_text;
  }

  private String getText() {
    if (fTextField == null) {
      return null;
    }
    return fTextField.getText();
  }

  private void loadSettings() {
    // TODO(scheglov)
//    fSettings = getDialogSettings().getSection(ExtractMethodWizard.DIALOG_SETTING_SECTION);
//    if (fSettings == null) {
//      fSettings = getDialogSettings().addNewSection(ExtractMethodWizard.DIALOG_SETTING_SECTION);
//      fSettings.put(THROW_RUNTIME_EXCEPTIONS, false);
//      fSettings.put(
//          GENERATE_JAVADOC,
//          JavaPreferencesSettings.getCodeGenerationSettings(fRefactoring.getCompilationUnit().getJavaProject()).createComments);
//      fSettings.put(ACCESS_MODIFIER, Modifier.PRIVATE);
//    }
//    fRefactoring.setThrowRuntimeExceptions(fSettings.getBoolean(THROW_RUNTIME_EXCEPTIONS));
//    final String accessModifier = fSettings.get(ACCESS_MODIFIER);
//    if (accessModifier != null) {
//      fRefactoring.setVisibility(Integer.parseInt(accessModifier));
//    }
  }

  private void parameterModified() {
    updatePreview(getText());
    setPageComplete(validatePage(false));
  }

//  private void setGenerateJavadoc(boolean value) {
//    fSettings.put(GENERATE_JAVADOC, value);
//    fRefactoring.setGenerateJavadoc(value);
//  }
//
//  private void setRethrowRuntimeException(boolean value) {
//    fSettings.put(THROW_RUNTIME_EXCEPTIONS, value);
//    fRefactoring.setThrowRuntimeExceptions(value);
//    updatePreview(getText());
//  }

  //---- Input validation ------------------------------------------------------

//  private void setVisibility(Integer visibility) {
//    fRefactoring.setVisibility(visibility.intValue());
//    updatePreview(getText());
//  }

  private void textModified(String text) {
    fRefactoring.setMethodName(text);
    RefactoringStatus status = validatePage(true);
    if (!status.hasFatalError()) {
      updatePreview(text);
    } else {
      fSignaturePreviewDocument.set(""); //$NON-NLS-1$
    }
    setPageComplete(status);
  }

  private void updatePreview(String text) {
    if (fSignaturePreview == null) {
      return;
    }

    if (text.length() == 0) {
      text = "someMethodName"; //$NON-NLS-1$
    }

    int top = fSignaturePreview.getTextWidget().getTopPixel();
    String signature;
    try {
      signature = fRefactoring.getSignature(text);
    } catch (IllegalArgumentException e) {
      signature = ""; //$NON-NLS-1$
    }
    fSignaturePreviewDocument.set(signature);
    fSignaturePreview.getTextWidget().setTopPixel(top);
  }

  private RefactoringStatus validateMethodName() {
    RefactoringStatus result = new RefactoringStatus();
    String text = getText();
    if ("".equals(text)) { //$NON-NLS-1$
      result.addFatalError(RefactoringMessages.ExtractMethodInputPage_validation_emptyMethodName);
      return result;
    }
    result.merge(fRefactoring.checkMethodName());
    return result;
  }

  private RefactoringStatus validatePage(boolean text) {
    RefactoringStatus result = new RefactoringStatus();
    if (text) {
      result.merge(validateMethodName());
//      result.merge(validateParameters());
    } else {
//      result.merge(validateParameters());
      result.merge(validateMethodName());
    }
    return result;
  }

//  private RefactoringStatus validateParameters() {
//    RefactoringStatus result = new RefactoringStatus();
//    List<ParameterInfo> parameters = fRefactoring.getParameterInfos();
//    for (Iterator<ParameterInfo> iter = parameters.iterator(); iter.hasNext();) {
//      ParameterInfo info = iter.next();
//      if ("".equals(info.getNewName())) { //$NON-NLS-1$
//        result.addFatalError(RefactoringMessages.ExtractMethodInputPage_validation_emptyParameterName);
//        return result;
//      }
//    }
//    result.merge(fRefactoring.checkParameterNames());
//    result.merge(fRefactoring.checkVarargOrder());
//    return result;
//  }
}
