import hudson.model.*
import hudson.AbortException
import hudson.console.HyperlinkNote
import java.util.concurrent.CancellationException
  
// Increase the number of executors to 2 (second executor is for sub-jobs)
Hudson hudson = Hudson.getInstance()
hudson.setNumExecutors(2)
hudson.setNodes(hudson.getNodes())

def finalResult = Result.SUCCESS

def jobs = ['job-name-a', 'job-name-b', 'job-name-c']
def failedJobs = []

for (job in jobs) {
    // Execute test case
    def result = executeJob(job)

    // Check that it succeeded
    if (result != Result.SUCCESS && result != Result.UNSTABLE && result != Result.ABORTED) {
        finalResult = result
        failedJobs.add(job)
    }   
}

// Set the number of executors back to 1
hudson.setNumExecutors(1)
hudson.setNodes(hudson.getNodes())

if (finalResult != Result.SUCCESS && finalResult != Result.UNSTABLE) {
    println "Test suite has failed. Failing jobs: " + failedJobs
    build.getExecutor().interrupt(Result.FAILURE) 
} else {
    println "Test suite has been stopped"
    build.getExecutor().interrupt(Result.SUCCESS) 
}

// Function to execute test cases
def executeJob(jobName) {
    def job = Hudson.instance.getJob(jobName)
    def subBuild
    try {
        def future = job.scheduleBuild2(0, new Cause.UpstreamCause(build))
        println "Waiting for the completion of " + HyperlinkNote.encodeTo('/' + job.url, job.fullDisplayName)
        subBuild = future.get()
    } catch (InterruptedException e) {
        return Result.ABORTED
    }
    
    println HyperlinkNote.encodeTo('/' + subBuild.url, subBuild.fullDisplayName) + " completed. Result was " + subBuild.result  
    return subBuild.result
}