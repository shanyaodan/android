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
 * limitations under the License.
 */
package com.android.tools.idea.resources.aar;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.android.ide.common.rendering.api.ArrayResourceValue;
import com.android.ide.common.rendering.api.AttrResourceValue;
import com.android.ide.common.rendering.api.AttributeFormat;
import com.android.ide.common.rendering.api.DensityBasedResourceValue;
import com.android.ide.common.rendering.api.PluralsResourceValue;
import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.rendering.api.StyleItemResourceValue;
import com.android.ide.common.rendering.api.StyleResourceValue;
import com.android.ide.common.rendering.api.StyleableResourceValue;
import com.android.ide.common.resources.ResourceItem;
import com.android.ide.common.resources.ResourceItemWithVisibility;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.util.PathString;
import com.android.resources.Density;
import com.android.resources.ResourceType;
import com.android.resources.ResourceVisibility;
import com.android.sdklib.IAndroidTarget;
import com.android.tools.idea.configurations.ConfigurationManager;
import com.android.tools.idea.rendering.multi.CompatibilityRenderTarget;
import com.android.tools.idea.res.ResourceHelper;
import com.android.tools.idea.util.AndroidTestPaths;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import junit.framework.TestCase;
import org.jetbrains.android.AndroidTestCase;
import org.jetbrains.android.sdk.StudioEmbeddedRenderTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tests for {@link FrameworkResourceRepository}.
 */
public class FrameworkResourceRepositoryTest extends AndroidTestCase {
  /** Enables printing of repository statistics. */
  private static final boolean PRINT_STATS = false;
  private static final List<String> FRAMEWORK_RES_JAR_PATHS = Arrays.asList(
    "../../tools/adt/idea/resources-aar/framework_res.jar", // For running from Bazel.
    "../../bazel-genfiles/tools/adt/idea/resources-aar/framework_res.jar", // For running from IntelliJ.
    "community/build/dependencies/build/android-sdk/prebuilts/studio/layoutlib/data/framework_res.jar", // For running from Idea Ultimate.
    "build/dependencies/build/android-sdk/prebuilts/studio/layoutlib/data/framework_res.jar" // For running from Idea Community.
  );

  private Path myResourceFolder;

  private void deleteRepositoryCache() throws IOException {
    for (boolean withLocaleResources : new boolean[] {false, true}) {
      Files.deleteIfExists(FrameworkResourceRepository.getCacheFile(myResourceFolder, withLocaleResources));
    }
  }

  /**
   * Returns the resource folder of the Android framework resources used by LayoutLib.
   */
  @NotNull
  private Path getSdkResFolder() {
    ConfigurationManager manager = ConfigurationManager.getOrCreateInstance(myModule);
    IAndroidTarget target = manager.getHighestApiTarget();
    if (target == null) {
      TestCase.fail();
    }
    CompatibilityRenderTarget compatibilityTarget = StudioEmbeddedRenderTarget.getCompatibilityTarget(target);
    return Paths.get(compatibilityTarget.getLocation(), "data", "res").normalize();
  }

  /**
   * Returns the resource folder of the Android framework resources used by LayoutLib.
   */
  @NotNull
  private static Path getFrameworkResJar() {
    return AndroidTestPaths.selectExisting(FRAMEWORK_RES_JAR_PATHS, "Could not find framework_res.jar");
  }

  private static void assertVisibility(
      @NotNull ResourceRepository repository, @NotNull ResourceType type, @NotNull String name, @NotNull ResourceVisibility visibility) {
    List<ResourceItem> resources = repository.getResources(ResourceNamespace.ANDROID, type, name);
    assertThat(resources).isNotEmpty();
    assertThat(((ResourceItemWithVisibility)resources.get(0)).getVisibility()).isEqualTo(visibility);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myResourceFolder = getSdkResFolder();
    deleteRepositoryCache();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      deleteRepositoryCache();
    } finally {
      super.tearDown();
    }
  }

  public void testLoadingFromSourcesAndCache() {
    for (boolean withLocaleResources : new boolean[] {true, false}) {
      // Create persistent cache.
      FrameworkResourceRepository.create(myResourceFolder, withLocaleResources, true, MoreExecutors.directExecutor());

      long loadTimeFromSources = 0;
      long loadTimeFromCache = 0;
      int count = PRINT_STATS ? 100 : 1;
      for (int i = 0; i < count; ++i) {
        long start = System.currentTimeMillis();
        FrameworkResourceRepository fromSourceFiles =
            FrameworkResourceRepository.create(myResourceFolder, withLocaleResources, false, null);
        loadTimeFromSources += System.currentTimeMillis() - start;
        if (i == 0) {
          assertThat(fromSourceFiles.isLoadedFromCache()).isFalse();
          checkContents(fromSourceFiles);
        }
        start = System.currentTimeMillis();
        FrameworkResourceRepository fromCache =
            FrameworkResourceRepository.create(myResourceFolder, withLocaleResources, true, null);
        loadTimeFromCache += System.currentTimeMillis() - start;
        if (i == 0) {
          assertThat(fromCache.isLoadedFromCache()).isTrue();
          compareContents(fromSourceFiles, fromCache);
          checkContents(fromCache);
        }
      }

      if (PRINT_STATS) {
        String type = withLocaleResources ? "Load time" : "Load time without locale resources";
        System.out.println(type + " without cache: " + loadTimeFromSources / (count * 1000.)
                           + " sec, with cache " + loadTimeFromCache / (count * 1000.) + " sec");
      }
    }
  }

  public void testLoadingFromSourcesAndJar() {
    Path frameworkResJar = getFrameworkResJar();
    for (boolean withLocaleResources : new boolean[] {true, false}) {
      long loadTimeFromSources = 0;
      long loadTimeFromJar = 0;
      int count = PRINT_STATS ? 100 : 1;
      for (int i = 0; i < count; ++i) {
        long start = System.currentTimeMillis();
        FrameworkResourceRepository fromSourceFiles = FrameworkResourceRepository.create(myResourceFolder, withLocaleResources);
        loadTimeFromSources += System.currentTimeMillis() - start;
        if (i == 0) {
          assertThat(fromSourceFiles.isLoadedFromCache()).isFalse();
          checkContents(fromSourceFiles);
        }
        start = System.currentTimeMillis();
        FrameworkResourceRepository fromJar = FrameworkResourceRepository.create(frameworkResJar, withLocaleResources);
        loadTimeFromJar += System.currentTimeMillis() - start;
        if (i == 0) {
          assertThat(fromJar.isLoadedFromCache()).isFalse();
          compareContents(fromSourceFiles, fromJar);
          checkContents(fromJar);
        }
      }

      if (PRINT_STATS) {
        String type = withLocaleResources ? "Load time" : "Load time without locale resources";
        System.out.println(type + " from source files: " + loadTimeFromSources / (count * 1000.)
                           + " sec, from framework_res.jar file " + loadTimeFromJar / (count * 1000.) + " sec");
      }
    }
  }

  private static void compareContents(@NotNull ResourceRepository expected, @NotNull ResourceRepository actual) {
    List<ResourceItem> expectedItems = new ArrayList<>(expected.getAllResources());
    List<ResourceItem> actualItems = new ArrayList<>(actual.getAllResources());

    Comparator<ResourceItem> comparator = (item1, item2) -> {
      int comp = item1.getType().compareTo(item2.getType());
      if (comp != 0) {
        return comp;
      }
      comp = item1.getNamespace().compareTo(item2.getNamespace());
      if (comp != 0) {
        return comp;
      }
      comp = item1.getName().compareTo(item2.getName());
      if (comp != 0) {
        return comp;
      }
      return item1.getSource().compareTo(item2.getSource());
    };
    expectedItems.sort(comparator);
    actualItems.sort(comparator);
    assertThat(actualItems.size()).isEqualTo(expectedItems.size());
    for (int i = 0; i < expectedItems.size(); i++) {
      ResourceItem expectedItem = expectedItems.get(i);
      ResourceItem actualItem = actualItems.get(i);
      assertWithMessage("Different ResourceItem at position " + i).that(areEquivalent(expectedItem, actualItem)).isTrue();
      assertWithMessage("Different FolderConfiguration at position " + i)
          .that(actualItem.getConfiguration()).isEqualTo(expectedItem.getConfiguration());
      ResourceValue expectedValue = expectedItem.getResourceValue();
      ResourceValue actualValue = actualItem.getResourceValue();
      if (!areEquivalentResourceValues(expectedValue, actualValue)) {
        assertWithMessage("Different ResourceValue at position " + i).that(actualValue).isEqualTo(expectedValue);
      }
    }

    for (ResourceType type : ResourceType.values()) {
      List<ResourceItem> expectedPublic = new ArrayList<>(expected.getPublicResources(ResourceNamespace.ANDROID, type));
      List<ResourceItem> actualPublic = new ArrayList<>(actual.getPublicResources(ResourceNamespace.ANDROID, type));
      assertWithMessage("Number of public resources doesn't match for type " + type.getName())
          .that(actualPublic.size()).isEqualTo(expectedPublic.size());
      expectedPublic.sort(comparator);
      actualPublic.sort(comparator);
      for (int i = 0; i < expectedPublic.size(); i++) {
        ResourceItem expectedItem = expectedPublic.get(i);
        ResourceItem actualItem = actualPublic.get(i);
        assertWithMessage("Public resource difference at position " + i + " for type " + type.getName())
            .that(areEquivalent(expectedItem, actualItem)).isTrue();
      }
    }
  }

  private static boolean areEquivalent(@NotNull ResourceItem item1, @NotNull ResourceItem item2) {
    if (!item1.getType().equals(item2.getType())) {
      return false;
    }
    if (!item1.getNamespace().equals(item2.getNamespace())) {
      return false;
    }
    if (!item1.getName().equals(item2.getName())) {
      return false;
    }
    if (!Objects.equals(item1.getLibraryName(), item2.getLibraryName())) {
      return false;
    }
    if (item1.isFileBased() != item2.isFileBased()) {
      return false;
    }
    if (item1.isFileBased() && !areEquivalentSources(item1.getSource(), item2.getSource())) {
      return false;
    }
    if (!areEquivalentSources(item1.getOriginalSource(), item2.getOriginalSource())) {
      return false;
    }
    return true;
  }

  private static boolean areEquivalentResourceValues(@Nullable ResourceValue value1, @Nullable ResourceValue value2) {
    if (value1 == value2) {
      return true;
    }
    if (value1 == null || value2 == null) {
      return false;
    }
    if (!value1.getResourceType().equals(value2.getResourceType())) {
      return false;
    }
    if (!value1.getNamespace().equals(value2.getNamespace())) {
      return false;
    }
    if (!value1.getName().equals(value2.getName())) {
      return false;
    }
    if (!Objects.equals(value1.getLibraryName(), value2.getLibraryName())) {
      return false;
    }

    Density density1 = value1 instanceof DensityBasedResourceValue ? ((DensityBasedResourceValue)value1).getResourceDensity() : null;
    Density density2 = value2 instanceof DensityBasedResourceValue ? ((DensityBasedResourceValue)value2).getResourceDensity() : null;
    if (!Objects.equals(density1, density2)) {
      return false;
    }

    if (value1 instanceof StyleableResourceValue && value2 instanceof StyleableResourceValue) {
      List<AttrResourceValue> attrs1 = ((StyleableResourceValue)value1).getAllAttributes();
      List<AttrResourceValue> attrs2 = ((StyleableResourceValue)value2).getAllAttributes();
      if (attrs1.size() != attrs2.size()) {
        return false;
      }
      for (int i = 0; i < attrs1.size(); i++) {
        if (!areEquivalentResourceValues(attrs1.get(i), attrs2.get(i))) {
          return false;
        }
      }
    } else if ((value1 instanceof StyleableResourceValue) != (value2 instanceof StyleableResourceValue)) {
      return false;
    }

    if (value1 instanceof AttrResourceValue && value2 instanceof AttrResourceValue) {
      AttrResourceValue attr1 = (AttrResourceValue)value1;
      AttrResourceValue attr2 = (AttrResourceValue)value2;
      if (!Objects.equals(attr1.getDescription(), attr2.getDescription())) {
        return false;
      }
      if (!Objects.equals(attr1.getGroupName(), attr2.getGroupName())) {
        return false;
      }
      if (!Objects.equals(attr1.getFormats(), attr2.getFormats())) {
        return false;
      }
      Map<String, Integer> attrValues1 = attr1.getAttributeValues();
      Map<String, Integer> attrValues2 = attr2.getAttributeValues();
      if (!attrValues1.equals(attrValues2)) {
        return false;
      }
      for (String valueName: attrValues1.keySet()) {
        if (!Objects.equals(attr1.getValueDescription(valueName), attr2.getValueDescription(valueName))) {
          return false;
        }
      }
    } else if ((value1 instanceof AttrResourceValue) != (value2 instanceof AttrResourceValue)) {
      return false;
    }

    if (value1 instanceof StyleResourceValue && value2 instanceof StyleResourceValue) {
      StyleResourceValue style1 = (StyleResourceValue)value1;
      StyleResourceValue style2 = (StyleResourceValue)value2;
      if (!Objects.equals(style1.getParentStyle(), style2.getParentStyle())) {
        return false;
      }
      Collection<StyleItemResourceValue> items1 = style1.getDefinedItems();
      Collection<StyleItemResourceValue> items2 = style2.getDefinedItems();
      if (items1.size() != items2.size()) {
        return false;
      }
      Iterator<StyleItemResourceValue> it1 = items1.iterator();
      Iterator<StyleItemResourceValue> it2 = items2.iterator();
      while (it1.hasNext()) {
        StyleItemResourceValue item1 = it1.next();
        StyleItemResourceValue item2 = it2.next();
        if (!areEquivalentResourceValues(item1, item2)) {
          return false;
        }
      }
    } else if ((value1 instanceof StyleResourceValue) != (value2 instanceof StyleResourceValue)) {
      return false;
    }

    if (value1 instanceof ArrayResourceValue && value2 instanceof ArrayResourceValue) {
      ArrayResourceValue array1 = (ArrayResourceValue)value1;
      ArrayResourceValue array2 = (ArrayResourceValue)value2;
      if (array1.getElementCount() != array2.getElementCount()) {
        return false;
      }
      for (int i = 0; i < array1.getElementCount(); i++) {
        if (!array1.getElement(i).equals(array2.getElement(i))) {
          return false;
        }
      }
    } else if ((value1 instanceof ArrayResourceValue) != (value2 instanceof ArrayResourceValue)) {
      return false;
    }

    if (value1 instanceof PluralsResourceValue && value2 instanceof PluralsResourceValue) {
      PluralsResourceValue plural1 = (PluralsResourceValue)value1;
      PluralsResourceValue plural2 = (PluralsResourceValue)value2;
      if (plural1.getPluralsCount() != plural2.getPluralsCount()) {
        return false;
      }
      for (int i = 0; i < plural1.getPluralsCount(); i++) {
        if (!plural1.getQuantity(i).equals(plural2.getQuantity(i)) || !plural1.getValue(i).equals(plural2.getValue(i))) {
          return false;
        }
      }
    } else if ((value1 instanceof PluralsResourceValue) != (value2 instanceof PluralsResourceValue)) {
      return false;
    }

    String v1 = value1.getValue();
    String v2 = value2.getValue();
    if (value1 instanceof AarFileResourceItem && value2 instanceof AarFileResourceItem) {
      PathString path1 = ResourceHelper.toFileResourcePathString(v1);
      PathString path2 = ResourceHelper.toFileResourcePathString(v2);
      if (!areEquivalentSources(path1, path2)) {
        return false;
      }
    } else if ((value1 instanceof AarFileResourceItem) != (value2 instanceof AarFileResourceItem)) {
      return false;
    } else if (!Objects.equals(v1, v2)) {
      return false;
    }

    return true;
  }

  private static boolean areEquivalentSources(@Nullable PathString path1, @Nullable PathString path2) {
    if (Objects.equals(path1, path2)) {
      return true;
    }
    if (path1 != null && path2 != null) {
      URI filesystemUri1 = path1.getFilesystemUri();
      URI filesystemUri2 = path2.getFilesystemUri();
      URI nonFileUri = filesystemUri2;
      if (filesystemUri2.getScheme().equals("file") && !filesystemUri1.getScheme().equals("file")) {
        PathString temp = path1;
        path1 = path2;
        path2 = temp;
        nonFileUri = path1.getFilesystemUri();
      }
      String portablePath1 = path1.getPortablePath();
      String portablePath2 = path2.getPortablePath();
      if (nonFileUri.getScheme().equals("jar")) {
        int offset1 = indexOfEnd(portablePath1, "/res/");
        int offset2 = indexOfEnd(portablePath2, "/res/");
        return portablePath1.length() - offset1 == portablePath2.length() - offset2 &&
               portablePath1.regionMatches(offset1, portablePath2, offset2, portablePath1.length() - offset1);
      }
    }
    return false;
  }

  private static int indexOfEnd(@NotNull String stringToSearch, @NotNull String toSearchFor) {
    int index = stringToSearch.indexOf(toSearchFor);
    return index < 0 ? index : index + toSearchFor.length();
  }

  private static void checkContents(@NotNull ResourceRepository repository) {
    checkPublicResources(repository);
    checkAttributes(repository);
    checkIdResources(repository);
  }

  private static void checkAttributes(@NotNull ResourceRepository repository) {
    // `typeface` is declared first at top-level and later referenced from within `<declare-styleable>`.
    // Make sure the later reference doesn't shadow the original definition.
    AttrResourceValue attrValue = getAttrValue(repository, "typeface");
    assertThat(attrValue).isNotNull();
    assertThat(attrValue.getFormats()).containsExactly(AttributeFormat.ENUM);
    assertThat(attrValue.getDescription()).isEqualTo("Default text typeface.");
    assertThat(attrValue.getGroupName()).isEqualTo("Other non-theme attributes");
    Map<String, Integer> valueMap = attrValue.getAttributeValues();
    assertThat(valueMap.size()).isEqualTo(4);
    assertThat(valueMap).containsEntry("monospace", 3);
    assertThat(attrValue.getValueDescription("monospace")).isNull();

    // `appCategory` is defined only in attr_manifest.xml.
    attrValue = getAttrValue(repository, "appCategory");
    assertThat(attrValue).isNotNull();
    assertThat(attrValue.getFormats()).containsExactly(AttributeFormat.ENUM);
    assertThat(attrValue.getDescription()).startsWith("Declare the category of this app");
    assertThat(attrValue.getGroupName()).isNull();
    valueMap = attrValue.getAttributeValues();
    assertThat(valueMap.size()).isAtLeast(7);
    assertThat(valueMap).containsEntry("maps", 6);
    assertThat(attrValue.getValueDescription("maps")).contains("navigation");
  }

  private static AttrResourceValue getAttrValue(@NotNull ResourceRepository repository,
                                                @NotNull String attrName) {
    ResourceItem attrItem = repository.getResources(ResourceNamespace.ANDROID, ResourceType.ATTR, attrName).get(0);
    return (AttrResourceValue)attrItem.getResourceValue();
  }

  private static void checkIdResources(@NotNull ResourceRepository repository) {
    List<ResourceItem> items = repository.getResources(ResourceNamespace.ANDROID, ResourceType.ID, "mode_normal");
    items = items.stream().filter(item -> item.getConfiguration().isDefault()).collect(Collectors.toList());
    assertThat(items).hasSize(1);
  }

  private static void checkPublicResources(@NotNull ResourceRepository repository) {
    List<ResourceItem> resourceItems = repository.getAllResources();
    assertWithMessage("Too few resources: " + resourceItems.size()).that(resourceItems.size()).isAtLeast(10000);
    for (ResourceItem item : resourceItems) {
      assertThat(item.getNamespace()).isEqualTo(ResourceNamespace.ANDROID);
    }
    ImmutableMap<ResourceType, Integer> expectations = ImmutableMap.of(
      ResourceType.STYLE, 700,
      ResourceType.ATTR, 1200,
      ResourceType.DRAWABLE, 600,
      ResourceType.ID, 60,
      ResourceType.LAYOUT, 20
    );
    for (ResourceType type : ResourceType.values()) {
      Collection<ResourceItem> publicItems = repository.getPublicResources(ResourceNamespace.ANDROID, type);
      Integer minExpected = expectations.get(type);
      if (minExpected != null) {
        assertWithMessage("Too few public resources of type " + type.getName()).that(publicItems.size()).isAtLeast(minExpected);
      }
    }

    // Not mentioned in public.xml.
    assertVisibility(repository, ResourceType.STRING, "byteShort", ResourceVisibility.PRIVATE);
    // Defined at top level.
    assertVisibility(repository, ResourceType.STYLE, "Widget.DeviceDefault.Button.Colored", ResourceVisibility.PUBLIC);
    assertVisibility(repository, ResourceType.ATTR, "isVrOnly", ResourceVisibility.PRIVATE); // Due to the @hide comment
    assertVisibility(repository, ResourceType.ATTR, "__removed2", ResourceVisibility.PRIVATE); // Due to the naming convention
  }
}
