/*
 * Copyright (C) 2018 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.tools.build.bundletool.validation;

import static com.android.tools.build.bundletool.utils.ResourcesUtils.resourceIds;

import com.android.aapt.Resources.ResourceTable;
import com.android.tools.build.bundletool.exceptions.ValidationException;
import com.android.tools.build.bundletool.model.BundleModule;
import com.android.tools.build.bundletool.version.BundleToolVersion;
import com.android.tools.build.bundletool.version.Version;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.Set;

/** Validates module Titles for On Demand Modules */
public class ModuleTitleValidator extends SubValidator {

  @Override
  public void validateAllModules(ImmutableList<BundleModule> modules) {
    checkModuleTitles(modules);
  }

  private static void checkModuleTitles(ImmutableList<BundleModule> modules) {

    BundleModule baseModule = modules.stream().filter(BundleModule::isBaseModule).findFirst().get();

    // For bundles built using older versions we haven't strictly enforced module Title Validation.
    if (BundleToolVersion.getVersionFromBundleConfig(baseModule.getBundleConfig())
        .isOlderThan(Version.of("0.4.3"))) {
      return;
    }
    ResourceTable table = baseModule.getResourceTable().orElse(ResourceTable.getDefaultInstance());

    Set<Integer> stringResourceIds = resourceIds(table, type -> type.getName().equals("string"));

    for (BundleModule module : modules) {

      if (module.isDynamicModule()) {
        Optional<Integer> titleRefId = module.getAndroidManifest().getTitleRefId();

        if (!titleRefId.isPresent()) {
          throw ValidationException.builder()
              .withMessage(
                  "Mandatory title is missing in manifest for dynamic module '%s'.",
                  module.getName())
              .build();
        }
        if (!stringResourceIds.contains(titleRefId.get())) {
          throw ValidationException.builder()
              .withMessage(
                  "Title for dynamic module '%s' is missing in the base resource table.",
                  module.getName())
              .build();
        }
      }
    }
  }
}
