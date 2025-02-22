// src/devops/common/utils.groovy
package devops.common;

// imports
import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonOutput
import hudson.FilePath
import jenkins.model.Jenkins

// checks input value for default value use if not set
def defaultInput(input, default_value) {
  return input ?: default_value
}

// removes file
@NonCPS
void removeFile(String file) {
  // delete a file off of the master
  if (env['NODE_NAME'].equals('master')) {
    new File(file).delete();
  }
  // delete a file off of the build node
  else {
    new FilePath(Jenkins.getInstance().getComputer(env['NODE_NAME']).getChannel(), file).delete();
  }
}

// downloads file
@NonCPS
void downloadFile(String url, String dest) {
  def file = null;

  // establish the file download for the master
  if (env['NODE_NAME'].equals('master')) {
    file = new File(dest).newOutputStream();
  }
  // establish the file download for the build node
  else {
    file = new FilePath(Jenkins.getInstance().getComputer(env['NODE_NAME']).getChannel(), dest).newOutputStream();
  }
  // download the file and close the ostream
  file << new URL(url).openStream();
  file.close();
}

// functionally equivalent to unix mkdir -p
@NonCPS
void makeDirParents(String dir) {
  print "Attempting to recursively create directory ${dir} on Jenkins ${env['NODE_NAME']}"

  // create a directory on the master
  if (env['NODE_NAME'].equals('master')) {
    // short circuit if directory exists
    if (File(dir).exists()) {
      print 'Directory already exists on node.'
      return
    }

    new File(dir).mkdirs();
  }
  // create a directory on the build agent
  else {
    // short circuit if directory exists
    if (FilePath(Jenkins.getInstance().getComputer(env['NODE_NAME']).getChannel(), dir).exists()) {
      print 'Directory already exists on node.'
      return
    }

    new FilePath(Jenkins.getInstance().getComputer(env['NODE_NAME']).getChannel(), dir).mkdirs();
  }
}

// converts content map to json string
String mapToJSON(Map content) {
  return JsonOutput.toJson(content);
}

// converts closure body to config map, or returns same config map
// bridges gap between users of older DSL and newer DSL
Map paramsConverter(body) {
  // initialize config
  Map config = [:]

  // if we received older DSL, convert it into config map
  if (body instanceof Closure) {
    // evaluate the body block and collect configuration into the object
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
  }
  // if newer DSL, return same map as the config
  else if (body instanceof Map) {
    config = body
  }
  // params are invalid type
  else {
    throw new Exception('The parameter inputs are an invalid type. They must either be a Closure or Map. Consult the documentation for more information.')
  }

  return config
}
// example usage: void globalVarMethod(body)
// where body is closure or map
// Map config = new utils().paramsConverter(body)
