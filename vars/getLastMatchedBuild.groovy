#!/usr/bin/env groovy

// references
// https://javadoc.jenkins-ci.org/hudson/model/Run.html
// https://javadoc.jenkins.io/plugin/workflow-job/org/jenkinsci/plugins/workflow/job/WorkflowJob.html
// https://javadoc.jenkins.io/plugin/workflow-job/org/jenkinsci/plugins/workflow/job/WorkflowRun.html

// examples:
// println getLastMatchedBuild("Builds/Build-All-Cores","SUCCESS", { params -> params.any{ p -> p.name == "TargetEnvironment" && p.value == "Prod" } })
// println getLastMatchedBuild("Builds/Build-All-Cores","", { params -> params.any{ p -> p.name == "TargetEnvironment" && p.value == "Prod" } }, { build -> build.result.toString() == "SUCCESS" })

def call(String name, String resultFilter = "SUCCESS", parametersFilter = { params -> true }, buildFilter = { build -> true }) {
    def job = Jenkins.instance.getItemByFullName(name)
    def builds = job.getBuilds()
    for(build in builds) {
        def params = build?.actions.find{ it instanceof ParametersAction }?.parameters
        def match = !resultFilter || build.result.toString() == resultFilter
        match = match && (!parametersFilter || (params && parametersFilter(params)))
        match = match && (!buildFilter || (buildFilter(build)))
        if(match) {
          return [number: build.getNumber(), result: build.result.toString(),name: build.getFullDisplayName(), id: build.getId(), duration:  build.getDuration(), description: build.getDescription() ]
        }
    }
    return null
}
