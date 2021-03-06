package de.mannodermaus.gradle.plugins.junit5

import de.mannodermaus.gradle.plugins.junit5.internal.android
import de.mannodermaus.gradle.plugins.junit5.internal.extend
import de.mannodermaus.gradle.plugins.junit5.internal.extensionByName
import de.mannodermaus.gradle.plugins.junit5.tasks.JUnit5UnitTest
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.tasks.Input
import org.gradle.util.ConfigureUtil
import org.junit.platform.commons.util.Preconditions
import org.junit.platform.console.options.Details
import org.junit.platform.engine.discovery.ClassNameFilter
import java.io.File

internal fun attachDsl(project: Project) {
  // Hook the JUnit Platform configuration into the Android testOptions
  project.android.testOptions
      .extend<AndroidJUnitPlatformExtension>(EXTENSION_NAME, arrayOf(project)) { ju5 ->
        ju5.extend<SelectorsExtension>(SELECTORS_EXTENSION_NAME)
        ju5.extend<FiltersExtension>(FILTERS_EXTENSION_NAME) { filters ->
          filters.extend<PackagesExtension>(PACKAGES_EXTENSION_NAME)
          filters.extend<TagsExtension>(TAGS_EXTENSION_NAME)
          filters.extend<EnginesExtension>(ENGINES_EXTENSION_NAME)
        }
      }
}

/**
 * The main extension provided through the android-junit5 Gradle plugin.
 * It defines the root of the configuration tree exposed by the plugin,
 * and is located under "android.testOptions" using the name "junitPlatform".
 */
open class AndroidJUnitPlatformExtension(private val project: Project) {

  operator fun invoke(config: AndroidJUnitPlatformExtension.() -> Unit) {
    this.config()
  }

  /**
   * The version of JUnit Platform to use
   */
  var platformVersion: String? = null
  fun platformVersion(version: String?) {
    this.platformVersion = version
  }

  /**
   * The version of JUnit Jupiter to use
   */
  var jupiterVersion: String? = null
  fun jupiterVersion(version: String?) {
    this.jupiterVersion = version
  }

  /**
   * The version of JUnit Vintage to use
   */
  var vintageVersion: String? = null
  fun vintageVersion(version: String?) {
    this.vintageVersion = version
  }

  /**
   * The directory for the test report files
   */
  var reportsDir: File? = null
  fun reportsDir(reportsDir: File?) {
    // Work around for https://discuss.gradle.org/t/bug-in-project-file-on-windows/19917
    if (reportsDir is File) {
      this.reportsDir = reportsDir
    } else {
      this.reportsDir = project.file(reportsDir ?: "")
    }
  }

  /**
   * The fully qualified class name of the {@link java.util.logging.LogManager}
   * to use. The plugin will set the {@code java.util.logging.manager}
   * system property to this value
   */
  var logManager: String? = null
  fun logManager(logManager: String?) {
    this.logManager = logManager
  }

  /**
   * Whether or not the standard Gradle {@code test} task should be enabled
   */
  var enableStandardTestTask = false
  fun enableStandardTestTask(state: Boolean) {
    this.enableStandardTestTask = state
  }

  /**
   * Select test execution plan details mode
   */
  var details = Details.NONE
  fun details(details: Details) {
    this.details = details
  }

  /**
   * The additional configuration parameters to be used
   */
  private val _configurationParameters = mutableMapOf<String, String>()
  val configurationParameters: Map<String, String>
    @Input get() = _configurationParameters.toMap()

  /**
   * Add a configuration parameter
   */
  fun configurationParameter(key: String, value: String) {
    Preconditions.notBlank(key, "key must not be blank")
    Preconditions.condition(!key.contains('='), { "key must not contain \'=\': \"$key\"" })
    Preconditions.notNull(value, { "value must not be null for key: \"$key\"" })
    _configurationParameters[key] = value
  }

  /**
   * Add a map of configuration parameters
   */
  fun configurationParameters(parameters: Map<String, String>) {
    parameters.forEach { configurationParameter(it.key, it.value) }
  }

  /**
   * Configure the {@link SelectorsExtension} for this plugin
   */
  val selectors: SelectorsExtension get() = extensionByName(SELECTORS_EXTENSION_NAME)

  /**
   * Configure the {@link SelectorsExtension} for this plugin
   */
  fun selectors(action: Action<SelectorsExtension>) {
    action.execute(selectors)
  }

  /**
   * Configure the {@link FiltersExtension} for this plugin
   */
  val filters: FiltersExtension get() = extensionByName(FILTERS_EXTENSION_NAME)

  /**
   * Configure the {@link FiltersExtension} for this plugin
   */
  fun filters(action: Action<FiltersExtension>) {
    action.execute(filters)
  }

  /* Android Unit Test support */

  /**
   * Configures unit test options
   *
   * @since 1.0.23
   */
  val unitTests = UnitTestOptions()

  /**
   * Configures unit test options
   *
   * @since 1.0.23
   */
  fun unitTests(closure: Closure<UnitTestOptions>) {
    ConfigureUtil.configure(closure, unitTests)
  }

  /* Android Instrumentation Test support */

  /**
   * Options for controlling instrumentation test execution with JUnit 5
   *
   * @since 1.0.22
   */
  val instrumentationTests = InstrumentationTestOptions()

  /**
   * Options for controlling instrumentation test execution with JUnit 5
   *
   * @since 1.0.22
   */
  fun instrumentationTests(closure: Closure<InstrumentationTestOptions>) {
    ConfigureUtil.configure(closure, instrumentationTests)
  }

  /* Jacoco Reporting Integration */

  /**
   * Options for controlling Jacoco reporting
   */
  val jacocoOptions = JacocoOptions()

  /**
   * Options for controlling Jacoco reporting
   */
  fun jacocoOptions(closure: Closure<JacocoOptions>) {
    ConfigureUtil.configure(closure, jacocoOptions)
  }
}

/**
 * Discovery selector configuration options for the plugin
 */
open class SelectorsExtension {

  operator fun invoke(config: SelectorsExtension.() -> Unit) {
    this.config()
  }

  /* URIs */

  private val _uris = mutableListOf<String>()
  val uris @Input get() = _uris.toList()

  /**
   * Add a <em>URI</em> to be used for test discovery
   */
  fun uri(uri: String) = uris(uri)

  /**
   * Add one or more <em>URIs</em> to be used for test discovery
   */
  fun uris(vararg uris: String) = this._uris.addAll(uris)

  /* Files */

  private val _files = mutableListOf<String>()
  val files @Input get() = _files.toList()

  /**
   * Add a <em>file</em> to be used for test discovery
   */
  fun file(file: String) = files(file)

  /**
   * Add one or more <em>files</em> to be used for test discovery
   */
  fun files(vararg files: String) = this._files.addAll(files)

  /* Directories */

  private val _directories = mutableListOf<String>()
  val directories @Input get() = _directories.toList()

  /**
   * Add a <em>directory</em> to be used for test discovery
   */
  fun directory(directory: String) = directories(directory)

  /**
   * Add one or more <em>directories</em> to be used for test discovery
   */
  fun directories(vararg directories: String) = this._directories.addAll(directories)

  /* Packages */

  private val _packages = mutableListOf<String>()
  val packages @Input get() = _packages.toList()

  /**
   * Add a <em>package</em> to be used for test discovery
   */
  @JvmName("aPackage")
  fun `package`(aPackage: String) = packages(aPackage)

  /**
   * Add one or more <em>packages</em> to be used for test discovery
   */
  fun packages(vararg packages: String) = this._packages.addAll(packages)

  fun packages() = _packages.toList()

  /* Classes */

  private val _classes = mutableListOf<String>()
  val classes @Input get() = _classes.toList()

  /**
   * Add a <em>class</em> to be used for test discovery
   */
  @JvmName("aClass")
  fun `class`(aClass: String) = classes(aClass)

  /**
   * Add one or more <em>classes</em> to be used for test discovery
   */
  fun classes(vararg classes: String) = this._classes.addAll(classes)

  fun classes() = _classes.toList()

  /* Methods */

  private val _methods = mutableListOf<String>()
  val methods @Input get() = _methods.toList()

  /**
   * Add a <em>method</em> to be used for test discovery
   */
  fun method(method: String) = methods(method)

  /**
   * Add one or more <em>methods</em> to be used for test discovery
   */
  fun methods(vararg methods: String) = this._methods.addAll(methods)

  /* Resources */

  private val _resources = mutableListOf<String>()
  val resources @Input get() = _resources.toList()

  /**
   * Add a <em>resource</em> to be used for test discovery
   */
  fun resource(resource: String) = resources(resource)

  /**
   * Add one or more <em>resources</em> to be used for test discovery
   */
  fun resources(vararg resources: String) = this._resources.addAll(resources)

  fun isEmpty() = _uris.isEmpty() && _files.isEmpty() && _directories.isEmpty() && _packages.isEmpty() && _classes.isEmpty() && _methods.isEmpty() && _resources.isEmpty()
}

open class FiltersExtension {

  operator fun invoke(config: FiltersExtension.() -> Unit) {
    this.config()
  }

  /**
   * List of class name patterns in the form of regular expressions for
   * classes that should be <em>included</em> in the test plan.
   *
   * <p>The patterns are combined using OR semantics, i.e. if the fully
   * qualified name of a class matches against at least one of the patterns,
   * the class will be included in the test plan.
   *
   * <p>If null, defaults to {@value ClassNameFilter#STANDARD_INCLUDE_PATTERN}
   */
  private var _includeClassNamePatterns: MutableList<String>? = null
  val includeClassNamePatterns
    @Input get() = _includeClassNamePatterns?.toList()
        ?: listOf(ClassNameFilter.STANDARD_INCLUDE_PATTERN)

  /**
   * Add a pattern to the list of <em>included</em> patterns
   */
  fun includeClassNamePattern(pattern: String) = includeClassNamePatterns(pattern)

  /**
   * Add patterns to the list of <em>included</em> patterns
   */
  fun includeClassNamePatterns(vararg patterns: String) {
    if (_includeClassNamePatterns == null) {
      _includeClassNamePatterns = mutableListOf()
    }
    this._includeClassNamePatterns!!.addAll(patterns)
  }

  private val _excludeClassNamePatterns = mutableListOf<String>()
  val excludeClassNamePatterns
    @Input get() = _excludeClassNamePatterns.toList()

  /**
   * Add a pattern to the list of <em>excluded</em> patterns
   */
  fun excludeClassNamePattern(pattern: String) = excludeClassNamePatterns(pattern)

  /**
   * Add patterns to the list of <em>excluded</em> patterns
   */
  fun excludeClassNamePatterns(vararg patterns: String) =
      this._excludeClassNamePatterns.addAll(patterns)

  /**
   * List of class name patterns in the form of regular expressions for
   * classes that should be <em>excluded</em> from the test plan.
   *
   * <p>The patterns are combined using OR semantics, i.e. if the fully
   * qualified name of a class matches against at least one of the patterns,
   * the class will be excluded from the test plan
   */
  fun excludeClassNamePatterns() = _excludeClassNamePatterns.toList()

  /**
   * Configure the {@link PackagesExtension} for this plugin
   */
  val packages: PackagesExtension get() = extensionByName(PACKAGES_EXTENSION_NAME)

  /**
   * Configure the {@link PackagesExtension} for this plugin
   */
  fun packages(action: Action<PackagesExtension>) {
    action.execute(packages)
  }

  /**
   * Configure the {@link TagsExtension} for this plugin
   */
  val tags: TagsExtension get() = extensionByName(TAGS_EXTENSION_NAME)

  /**
   * Configure the {@link TagsExtension} for this plugin
   */
  fun tags(action: Action<TagsExtension>) {
    action.execute(tags)
  }

  /**
   * Configure the {@link EnginesExtension} for this plugin
   */
  val engines: EnginesExtension get() = extensionByName(ENGINES_EXTENSION_NAME)

  /**
   * Configure the {@link EnginesExtension} for this plugin
   */
  fun engines(action: Action<EnginesExtension>) {
    action.execute(engines)
  }
}

/**
 * Package configuration options for the plugin
 */
open class PackagesExtension : IncludeExcludeContainer() {
  operator fun invoke(config: PackagesExtension.() -> Unit) {
    this.config()
  }
}

/**
 * Tag configuration options for the plugin
 */
open class TagsExtension : IncludeExcludeContainer() {
  operator fun invoke(config: TagsExtension.() -> Unit) {
    this.config()
  }
}

/**
 * Engine configuration options for the plugin
 */
open class EnginesExtension : IncludeExcludeContainer() {
  operator fun invoke(config: EnginesExtension.() -> Unit) {
    this.config()
  }
}

open class IncludeExcludeContainer {
  private val _include = mutableListOf<String>()
  val include @Input get() = _include.toList()
  fun include(vararg items: String) = this._include.addAll(items)

  private val _exclude = mutableListOf<String>()
  val exclude @Input get() = _exclude.toList()
  fun exclude(vararg items: String) = this._exclude.addAll(items)
}

/**
 * Options for controlling how JUnit 5 Unit Tests should be executed
 */
class UnitTestOptions {

  operator fun invoke(config: UnitTestOptions.() -> Unit) {
    this.config()
  }

  private val testTasks = DefaultDomainObjectSet<JUnit5UnitTest>(JUnit5UnitTest::class.java)

  /**
   * Whether or not to apply the Android Gradle Plugin's "testOptions"
   * to JUnit 5 tasks - true by default.
   *
   * Note that this will only affect the following properties assigned
   * by a "testOptions.unitTests.all" closure:
   *
   * - jvmArgs
   * - systemProperties
   * - environment variables
   */
  var applyDefaultTestOptions = true
  fun applyDefaultTestOptions(state: Boolean) {
    this.applyDefaultTestOptions = state
  }

  /**
   * Applies the provided config closure to all JUnit 5 test tasks,
   * and any task that'll be added in the future
   * @param configClosure Closure to apply
   */
  fun all(configClosure: Closure<JUnit5UnitTest>) {
    this.testTasks.all(configClosure)
  }

  /**
   * Applies the provided config action to all JUnit 5 test tasks,
   * and any task that'll be added in the future
   * @param configAction Action to apply
   */
  fun all(configAction: Action<JUnit5UnitTest>) {
    this.testTasks.all(configAction)
  }

  /**
   * Applies the provided config action to all JUnit 5 test tasks,
   * and any task that'll be added in the future
   * @param action Action to apply
   */
  fun all(action: JUnit5UnitTest.() -> Unit) {
    this.testTasks.all(action)
  }

  /**
   * Registers a JUnit 5 test task
   * @param task The task
   */
  fun applyConfiguration(task: JUnit5UnitTest) {
    this.testTasks.add(task)
  }
}

/**
 * Options for controlling instrumentation test execution
 */
class InstrumentationTestOptions {

  operator fun invoke(config: InstrumentationTestOptions.() -> Unit) {
    this.config()
  }

  /**
   * Whether or not to enable support for JUnit 5 instrumentation tests
   */
  var enabled: Boolean = true
  fun enabled(state: Boolean) {
    this.enabled = state
  }

  /**
   * The version of the instrumentation companion library to use
   */
  var version: String? = null
  fun version(version: String?) {
    this.version = version
  }
}


/**
 * Options for controlling Jacoco reporting
 */
class JacocoOptions {

  operator fun invoke(config: JacocoOptions.() -> Unit) {
    this.config()
  }

  /**
   * Whether or not to enable Jacoco task integration
   */
  var taskGenerationEnabled = true
  fun taskGenerationEnabled(state: Boolean) {
    this.taskGenerationEnabled = state
  }

  private val _onlyGenerateTasksForVariants = mutableSetOf<String>()
  val onlyGenerateTasksForVariants @Input get() = _onlyGenerateTasksForVariants.toSet()

  /**
   * Filter the generated Jacoco tasks,
   * so that only the given build variants are provided with a companion task.
   * Make sure to add the full product flavor name if necessary
   * (i.e. "paidDebug" if you use a "paid" product flavor and the "debug" build type)
   */
  fun onlyGenerateTasksForVariants(vararg variants: String) {
    _onlyGenerateTasksForVariants.addAll(variants)
  }

  /**
   * Options for controlling the HTML Report generated by Jacoco
   */
  val html = Report()

  /**
   * Options for controlling the HTML Report generated by Jacoco
   */
  fun html(closure: Closure<Report>) {
    ConfigureUtil.configure(closure, html)
  }

  /**
   * Options for controlling the CSV Report generated by Jacoco
   */
  val csv = Report()

  /**
   * Options for controlling the CSV Report generated by Jacoco
   */
  fun csv(closure: Closure<Report>) {
    ConfigureUtil.configure(closure, csv)
  }

  /**
   * Options for controlling the XML Report generated by Jacoco
   */
  val xml = Report()

  /**
   * Options for controlling the XML Report generated by Jacoco
   */
  fun xml(closure: Closure<Report>) {
    ConfigureUtil.configure(closure, xml)
  }

  /**
   * List of class name patterns that should be excluded from being processed by Jacoco.
   * By default, this will exclude R.class & BuildConfig.class
   */
  var excludedClasses = mutableListOf("**/R.class", "**/R$*.class", "**/BuildConfig.*")
  fun excludedClasses(vararg classes: String) = excludedClasses.addAll(classes)

  /**
   * List of source name patterns that should be excluded from being processed by Jacoco.
   * By default, this is an empty list
   */
  var excludedSources = mutableListOf<String>()
  fun excludedSources(vararg sources: String) = excludedSources.addAll(sources)

  class Report {

    operator fun invoke(config: Report.() -> Unit) {
      this.config()
    }

    /**
     * Whether or not this report should be generated
     */
    var enabled: Boolean = true
    fun enabled(state: Boolean) {
      this.enabled = state
    }

    /**
     * Name of the file to be generated; note that
     * due to the variant-aware nature of the plugin,
     * each variant will be assigned a distinct folder if necessary
     */
    var destination: File? = null
    fun destination(file: File?) {
      this.destination = file
    }
  }
}
