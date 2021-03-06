/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.npw.benchmark;

import static com.android.tools.idea.npw.model.NewProjectModel.getInitialDomain;

import com.android.tools.adtui.LabelWithEditButton;
import com.android.tools.adtui.util.FormScalingUtil;
import com.android.tools.adtui.validation.Validator;
import com.android.tools.adtui.validation.ValidatorPanel;
import com.android.tools.idea.npw.platform.Language;
import com.android.tools.idea.npw.project.DomainToPackageExpression;
import com.android.tools.idea.npw.template.components.LanguageComboProvider;
import com.android.tools.idea.npw.validator.ModuleValidator;
import com.android.tools.idea.observable.BindingsManager;
import com.android.tools.idea.observable.ListenerManager;
import com.android.tools.idea.observable.core.BoolProperty;
import com.android.tools.idea.observable.core.BoolValueProperty;
import com.android.tools.idea.observable.core.ObservableBool;
import com.android.tools.idea.observable.core.StringValueProperty;
import com.android.tools.idea.observable.expressions.Expression;
import com.android.tools.idea.observable.ui.SelectedItemProperty;
import com.android.tools.idea.observable.ui.TextProperty;
import com.android.tools.idea.ui.wizard.StudioWizardStepPanel;
import com.android.tools.idea.ui.wizard.WizardUtils;
import com.android.tools.idea.wizard.model.SkippableWizardStep;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigureBenchmarkModuleStep extends SkippableWizardStep<NewBenchmarkModuleModel> {
  @NotNull private final StudioWizardStepPanel myRootPanel;
  @NotNull private ValidatorPanel myValidatorPanel;
  private final BindingsManager myBindings = new BindingsManager();
  private final ListenerManager myListeners = new ListenerManager();

  private JPanel myPanel;
  private JTextField myModuleName;
  private LabelWithEditButton myPackageName;
  private JComboBox<Language> myLanguageComboBox;

  public ConfigureBenchmarkModuleStep(@NotNull NewBenchmarkModuleModel model, String title) {
    super(model, title);

    TextProperty moduleNameText = new TextProperty(myModuleName);
    TextProperty packageNameText = new TextProperty(myPackageName);
    SelectedItemProperty<Language> language = new SelectedItemProperty<>(myLanguageComboBox);
    BoolProperty isPackageNameSynced = new BoolValueProperty(true);

    ModuleValidator moduleValidator = new ModuleValidator(model.getProject());
    myValidatorPanel = new ValidatorPanel(this, myPanel);
    myValidatorPanel.registerValidator(moduleNameText, moduleValidator);
    myValidatorPanel.registerValidator(model.packageName(),
            value -> Validator.Result.fromNullableMessage(WizardUtils.validatePackageName(value)));

    myModuleName.setText(WizardUtils.getUniqueName(model.moduleName().get(), moduleValidator));

    Expression<String> computedPackageName =
      new DomainToPackageExpression(new StringValueProperty(getInitialDomain(false)), model.moduleName());
    myBindings.bind(model.moduleName(), moduleNameText, myValidatorPanel.hasErrors().not());
    myBindings.bind(packageNameText, computedPackageName, isPackageNameSynced);
    myBindings.bind(model.packageName(), packageNameText);
    myBindings.bindTwoWay(language, model.language());

    myListeners.listen(packageNameText, value -> isPackageNameSynced.set(value.equals(computedPackageName.get())));

    myRootPanel = new StudioWizardStepPanel(myValidatorPanel);
    FormScalingUtil.scaleComponentTree(this.getClass(), myRootPanel);
  }

  @NotNull
  @Override
  protected ObservableBool canGoForward() {
    return myValidatorPanel.hasErrors().not();
  }

  @NotNull
  @Override
  protected JComponent getComponent() {
    return myRootPanel;
  }

  @Nullable
  @Override
  protected JComponent getPreferredFocusComponent() {
    return myPackageName;
  }

  private void createUIComponents() {
    myLanguageComboBox = new LanguageComboProvider().createComponent();
  }

  @Override
  public void dispose() {
    myBindings.releaseAll();
    myListeners.releaseAll();
  }
}
