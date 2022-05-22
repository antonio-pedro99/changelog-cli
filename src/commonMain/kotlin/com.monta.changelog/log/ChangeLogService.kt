package com.monta.changelog.log

import com.monta.changelog.git.CommitInfo
import com.monta.changelog.git.GitService
import com.monta.changelog.github.GitHubService
import com.monta.changelog.model.ChangeLog
import com.monta.changelog.printer.ChangeLogPrinter
import com.monta.changelog.util.DebugLogger
import com.monta.changelog.util.GroupedCommitMap
import com.monta.changelog.util.LinkResolver

class ChangeLogService(
    private val debug: Boolean,
    private val serviceName: String,
    private val jiraAppName: String?,
    private val githubRelease: Boolean,
    private val githubToken: String?,
) {

    private val gitService = GitService()
    private val gitHubService = GitHubService(githubToken)
    private val repoInfo = gitService.getRepoInfo()
    private val linkResolvers = listOf(
        LinkResolver.Jira(
            jiraAppName = jiraAppName
        ),
        LinkResolver.Github(
            repoOwner = repoInfo.repoOwner,
            repoName = repoInfo.repoName
        ),
    )

    init {
        if (debug) {
            DebugLogger.setLoggingLevel(DebugLogger.Level.Info)
        } else {
            DebugLogger.setLoggingLevel(DebugLogger.Level.Error)
        }

        DebugLogger.debug("serviceName=$serviceName jiraAppName=$jiraAppName githubRelease=$githubRelease")
        DebugLogger.debug("repoOwner=${repoInfo.repoOwner} repoName=${repoInfo.repoName}")
    }

    suspend fun generateChangeLog(changeLogPrinter: ChangeLogPrinter) {
        val commitInfo = gitService.getCommits()

        val changeLog = ChangeLog(
            serviceName = serviceName,
            jiraAppName = jiraAppName,
            tagName = commitInfo.tagName,
            repoOwner = repoInfo.repoOwner,
            repoName = repoInfo.repoName,
            groupedCommitMap = commitInfo.toGroupedCommitMap()
        )

        if (githubRelease) {
            changeLog.githubReleaseUrl = gitHubService.createRelease(linkResolvers, changeLog)
        }

        changeLogPrinter.print(linkResolvers, changeLog)
    }

    private fun CommitInfo.toGroupedCommitMap(): GroupedCommitMap {
        return this.commits
            // Group them up by the scope
            .groupBy { commit ->
                commit.scope
            }
            // Then the real work begins
            .map { (scope, commits) ->
                // Then after we've grouped our commits by scope we need to further sort them by
                // Type as there is a sorting order there
                scope to commits
                    // So first we start off by grouping by type
                    .groupBy { commit ->
                        commit.type
                    }
                    // Then we turn that into a list (as only these are sortable)
                    .toList()
                    .sortedBy { (type, _) ->
                        type.sortOrder
                    }
                    .toMap()
            }
            // Sort by the scope (in this instance null will be first)
            .sortedBy { (scope, _) ->
                scope
            }
            // Then back to a map (hopefully in the correct order)
            .toMap()
    }
}



