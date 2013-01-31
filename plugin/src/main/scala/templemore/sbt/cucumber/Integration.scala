package templemore.sbt.cucumber

import sbt._
import std.TaskStreams
import templemore.sbt.util._

/**
 * Provides the actual integration with cucumber jvm. Capable of launching 
 * cucumber as both a forked JVM and within the current JVM process.
 *
 * @author Chris Turner
 * @author RandomCoder
 */
trait Integration {

  protected def cuke(args: Seq[String],
                     jvmSettings: JvmSettings,
                     options: Options,
                     output: Output,
                     s: TaskStreams[_]) = {
    val log = s.log

    if ( options.featuresPresent ) {
      log.debug("JVM Settings: %s".format(jvmSettings))
      log.debug("Cucumber Options: %s".format(options))
      log.debug("Cucumber Output: %s".format(output))

      runCucumber(args, jvmSettings, options, output, log)
    }
    else {
      log.info("No features directory found. Skipping for curent project.")
      0
    }
  }

  private def runCucumber(args: Seq[String],
                          jvmSettings: JvmSettings,
                          options: Options,
                          output: Output,
                          log: Logger) = {
    def tagsFromArgs = args.filter(isATag).toList
    def optsFromArgs = args.filter(isAnOption).toList
    def namesFromArgs = args.filter(isAName).toList

    val optionPattern = """-[a-z]""".r.pattern

    def isAnOption(arg: String) = (arg.startsWith("--") || optionPattern.matcher(arg).matches())
    def isATag(arg: String) = arg.startsWith("@") || arg.startsWith("~@")
    def isAName(arg:String) = !isATag(arg) && !isAnOption(arg)

    log.info("Running cucumber...")
    options.beforeFunc()
    val result = launchCucumberInSeparateJvm(jvmSettings, options, output, tagsFromArgs, namesFromArgs, optsFromArgs)
    options.afterFunc()
    result
  }

  private def launchCucumberInSeparateJvm(jvmSettings: JvmSettings, 
                                          options: Options,
                                          output: Output,
                                          tags: List[String], 
                                          names: List[String],
                                          cucumberOptions: List[String]): Int = {
    def makeOptionsList(options: List[String], flag: String) = options flatMap(List(flag, _))

    val cucumberParams = ("--glue" :: options.basePackage :: Nil) ++
                         options.extraOptions ++ 
                         output.options ++
                         makeOptionsList(tags, "--tags") ++ 
                         makeOptionsList(names, "--name") ++
                         cucumberOptions ++
                         (options.featuresLocation :: Nil)
    JvmLauncher(jvmSettings).launch(cucumberParams)
  }
}
